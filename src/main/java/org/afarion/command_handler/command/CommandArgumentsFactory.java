package org.afarion.command_handler.command;

import org.afarion.command_handler.command.config.CommandArgumentConfig;
import it.unimi.dsi.fastutil.ints.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;

final class CommandArgumentsFactory {

    private static final Logger log = LoggerFactory.getLogger(CommandArgumentsFactory.class);

    static CommandArguments processRawArguments(String strArgs) {
        return new CommandArguments(strArgs);
    }

    static CommandArguments processArguments(AbstractCommand command, String strArgs, MessageReceivedEvent event) {
        List<CommandArgumentConfig> argumentConfigs = command.getCommandArguments();
        Int2ObjectMap<String> strArgValues = new Int2ObjectOpenHashMap<>(); //TODO array map vs open hash map
        Int2DoubleMap doubleArgValues = new Int2DoubleLinkedOpenHashMap();
        IntList wrongArgsIds = new IntArrayList();


        StringUtils.substringBefore(strArgs, " ");
        for (CommandArgumentConfig argCfg : argumentConfigs) {
            log.trace("Validating argument {}", argCfg.getArgumentName());
            strArgs = strArgs.trim();
            if (strArgs.length() > 0) {
                if (!argCfg.isMultiWordChoosingEnabled() && !argCfg.isInQuotes()) {
                    String rawArg = StringUtils.substringBefore(strArgs, " ");
                    strArgs = StringUtils.substringAfter(strArgs, " ").trim();

                    strArgValues.put(argCfg.getId(), rawArg);

                    validateOptions(wrongArgsIds, rawArg, argCfg);

                    testStringValidators(wrongArgsIds, rawArg, argCfg);

                    if (argCfg.isParseToDouble()) {
                        parseToDouble(doubleArgValues, wrongArgsIds, rawArg, argCfg);
                    }
                } else {
                    int symbolsTaken;
                    symbolsTaken = processMultiWordArgument(wrongArgsIds, strArgValues, strArgs, argCfg, event, command);
                    if (symbolsTaken > 0) {
                        strArgs = strArgs.substring(symbolsTaken);
                        continue;
                    }

                    symbolsTaken = processMultiWordQuotesArg(wrongArgsIds, strArgValues, strArgs, argCfg);
                    if (symbolsTaken > 0) {
                        strArgs = symbolsTaken < strArgs.length() ? strArgs.substring(symbolsTaken) : "";
                    }
                }

            } else {
                if (!argCfg.isOptional()) {
                    wrongArgsIds.add(argCfg.getId());
                    log.debug("Argument {} has no value and isn't optional", argCfg.getArgumentName());
                } else {
                    setDefaultValues(strArgValues, doubleArgValues, argCfg);
                }

            }
        }

        return new CommandArguments(wrongArgsIds, strArgValues, doubleArgValues, strArgs);
    }

    /**
     * @return returns the same value if nothing was chosen, otherwise returns id of last word chosen + 1
     */
    private static int processMultiWordQuotesArg(IntList wrongArgsIds, Int2ObjectMap<String> strArgValues, String strArgs, CommandArgumentConfig argCfg) {
        if (argCfg.isInQuotes()) {
            int openingQuoteIndex = strArgs.indexOf('"');

            if (openingQuoteIndex > -1 && strArgs.length() > 1) {
                int closingQuoteIndex = strArgs.substring(openingQuoteIndex + 1).indexOf('"');

                if (closingQuoteIndex > -1) {
                    String betweenQuotes = strArgs.substring(1, closingQuoteIndex + 1);
                    log.trace("Found multi-word in quotes argument {}: {}", argCfg.getArgumentName(), betweenQuotes);

                    testStringValidators(wrongArgsIds, betweenQuotes, argCfg);

                    strArgValues.put(argCfg.getId(), betweenQuotes);

                    return  betweenQuotes.length() + 2;
                } else {
                    log.debug("Closing quote wasn't found in argument {}", argCfg.getArgumentName());
                }
            } else {
                log.debug("Opening quote wasn't in argument {}", argCfg.getArgumentName());
            }
        }

        log.debug("Argument {} is marked as in-quotes argument, but quotes weren't found",
                argCfg.getArgumentName());
        wrongArgsIds.add(argCfg.getId());
        return 0;
    }

    /**
     * @return index of last argument chosen
     */
    private static int processMultiWordArgument(IntList wrongArgsIds, Int2ObjectMap<String> strArgValues, String strArgs, CommandArgumentConfig argCfg, MessageReceivedEvent event, AbstractCommand cmd) {
        if (argCfg.isMultiWordChoosingEnabled()) {
            int id = argCfg.getId();
            int symbolsChosen = cmd.chooseArgumentSymbols(event, strArgs, id);
            if (symbolsChosen < 0) symbolsChosen = 0;
            if (symbolsChosen > 0) {
                String chosenStr = strArgs.substring(0, symbolsChosen);
                log.trace("Multi words chooser has chosen {} in argument {}", chosenStr, argCfg.getArgumentName());
                strArgValues.put(id, chosenStr);
            } else {
                log.debug("Multi word chooser didn't choose any words in argument {}", argCfg.getArgumentName());
                wrongArgsIds.add(id);
            }
            return symbolsChosen;
        }
        return 0;
    }

    private static void setDefaultValues(Int2ObjectMap<String> strArgValues, Int2DoubleMap doubleArgValues, CommandArgumentConfig argCfg) {
        int id = argCfg.getId();
        if (argCfg.isParseToDouble()) {
            log.trace("Argument {} has no value, setting default value {}", argCfg.getArgumentName(), argCfg.getDefaultDoubleValue());
            doubleArgValues.put(id, argCfg.getDefaultDoubleValue());
        } else {
            String defaultStringValue = argCfg.getDefaultStringValue();
            log.trace("Argument {} has no value, setting default value {}", argCfg.getArgumentName(), defaultStringValue);
            strArgValues.put(id, defaultStringValue);
        }
    }

    private static void parseToDouble(Int2DoubleMap doubleArgValues, IntList wrongArgsIds, String rawArg, CommandArgumentConfig argCfg) {
        try {
            double num = Double.parseDouble(rawArg);
            doubleArgValues.put(argCfg.getId(), num);
            log.trace("Parsed argument {} to double: {}", argCfg.getArgumentName(), num);

            testDoubleValidators(wrongArgsIds, argCfg, num);
        } catch (NumberFormatException e) {
            wrongArgsIds.add(argCfg.getId());
            log.debug("Unable to parse argument {} to double", argCfg.getArgumentName());
        }
    }

    private static void testDoubleValidators(IntList wrongArgsIds, CommandArgumentConfig argCfg, double num) {
        for (DoublePredicate predicate : argCfg.getDoubleValidators())
            if (!predicate.test(num)) {
                wrongArgsIds.add(argCfg.getId());
                log.debug("Argument {} with value {} doesn't match it's double predicate", argCfg.getArgumentName(), num);
                break;
            }
    }

    private static void testStringValidators(IntList wrongArgsIds, String rawArg, CommandArgumentConfig argCfg) {
        if(argCfg.isInQuotes())
            return;
        for (Predicate<String> predicate : argCfg.getStringValidators())
            if (!predicate.test(rawArg)) {
                log.debug("Argument {} with value {} doesn't match it's string validator", argCfg.getArgumentName(), rawArg);
                wrongArgsIds.add(argCfg.getId());
                break;
            }
    }

    private static void validateOptions(IntList wrongArgsIds, String rawArg, CommandArgumentConfig argCfg) {
        boolean correctOption = argCfg.getArgumentOptions().isEmpty();
        for (String option : argCfg.getArgumentOptions()) {
            if (option.toLowerCase().equals(rawArg.toLowerCase())) {
                correctOption = true;
                break;
            }
        }
        if (!correctOption && !argCfg.isOptional()) {
            log.debug("Argument {} with value {} doesn't match it's options - {}", argCfg.getArgumentName(), rawArg, argCfg.getArgumentOptions());
            wrongArgsIds.add(argCfg.getId());
        }
    }

}

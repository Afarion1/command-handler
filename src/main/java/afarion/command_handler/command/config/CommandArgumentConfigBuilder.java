package afarion.command_handler.command.config;

import afarion.command_handler.command.AbstractCommand;
import afarion.command_handler.command.CommandArguments;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;

/**
 * A sub-builder for {@link CommandConfigBuilder}, used in
 * {@link CommandConfigBuilder#addArguments(CommandArgumentConfigBuilder...)}<br/>
 * Changes to this builder won't affect arguments configuration once
 * {@link CommandConfigBuilder#build()} is called.
 */
public final class CommandArgumentConfigBuilder {
    private final int id;
    private final String argumentName;
    private final List<String> argumentOptions;
    private final List<Predicate<String>> stringValidators;
    private final List<DoublePredicate> doubleValidators;
    private boolean optional;
    private String argumentDesc;
    private boolean parseToDouble;
    private String defaultStringValue;
    private double defaultDoubleValue;
    private boolean inQuotes;
    private final String errorMsg;
    private boolean multiWordChoosingEnabled;


    /**
     * Default values:
     * <ul>
     *     <li>has no description</li>
     *     <li>isn't optional</li>
     *     <li>has no default string value</li>
     *     <li>shouldn't be parsed to Double</li>
     *     <li>has no default double value</li>
     *     <li>shouldn't be in quotes</li>
     *     <li>multi word choosing is disabled</li>
     * </ul>
     * @param id id of the argument which will be required to get it's value.
     *           The best practice is store the id in a static final field
     * @param argumentName name of the argument which will be used in command description,
     *                     command list and in wrong-command-usage error message
     */
    public CommandArgumentConfigBuilder(int id, String argumentName) {
        this.inQuotes = false;
        this.id = id;
        this.argumentName = argumentName;
        this.argumentDesc = "";
        this.defaultStringValue = null;
        this.defaultDoubleValue = Double.NaN;
        this.argumentOptions = new ArrayList<>();
        this.stringValidators = new ArrayList<>();
        this.doubleValidators = new ArrayList<>();
        this.errorMsg = "";
        this.multiWordChoosingEnabled = false;
    }


    /**
     * The description will be shown in command inspection (command info) and in wrong-command-usage error message
     */
    public CommandArgumentConfigBuilder setArgumentDescription(String desc) {
        if (desc != null)
            this.argumentDesc = desc;
        return this;
    }

    /**
     * Adds predicates that will be used to validate string value. <br/>
     * This option cannot be used if any of the following parameters is set to true:<br/>
     * <ul>
     *     <li>{@link #enableCustomArgumentChoosing(boolean)}</li>
     *     <li>{@link #setParseToDouble(boolean)}</li>
     * </ul>
     */
    @SafeVarargs
    public final CommandArgumentConfigBuilder addStringValidators(Predicate<String>... validators) {
        this.stringValidators.addAll(Arrays.asList(validators));
        return this;
    }

    /**
     * Adds predicates that will be used to validate double value.<br/>
     * The argument must also be set to {@link #setParseToDouble(boolean)}
     */
    public CommandArgumentConfigBuilder addDoubleValidators(DoublePredicate... validators) {
        this.doubleValidators.addAll(Arrays.asList(validators));
        return this;
    }

    /**
     * Execution of the argument's command will fail if given argument value doesn't match these options. Additionally,
     * these options will be displayed in command information.
     */
    public CommandArgumentConfigBuilder setArgumentOptions(String... options) {
        for (String option : options) {
            if (option != null && option.length() > 0)
                argumentOptions.add(option);
        }
        return this;
    }

    /**
     * When set to true the argument will be optional and could be omitted. There should be no non optional arguments
     * after an optional argument. In other words, optional arguments could be only at the end of all arguments, and
     * could be consecutive. <br/>
     * If an argument is set to be optional, then it's default value could be set as well, using one of the following
     * methods:
     * <ul>
     *     <li>{@link #setDefaultStringValue}</li>
     *     <li>{@link #setDefaultDoubleValue}</li>
     * </ul>
     * depending on whether the method is set to be parsed to Double or not
     *
     * @see CommandArgumentConfigBuilder#setParseToDouble
     */
    public CommandArgumentConfigBuilder setOptional(boolean bool) {
        this.optional = bool;
        return this;
    }

    /**
     * When set to true the argument will have to be typed in quotes (" ").<br/>
     * This is useful for separation of arguments with spaces.<br/>
     */
    public CommandArgumentConfigBuilder setInQuotes(boolean bool) {
        this.inQuotes = bool;
        return this;
    }

    //TODO implement custom error msg
//    public CommandArgumentConfigBuilder setErrorMsg(String msg) {
//        this.errorMsg = msg;
//        return this;
//    }

    /**
     * Sets default value that will be used if the argument is omitted. This method could be used if the argument is set
     * to be optional.<br/>
     * @see CommandArgumentConfigBuilder#setOptional
     */
    @NotNull
    public CommandArgumentConfigBuilder setDefaultStringValue(String str) {
        this.defaultStringValue = str;
        return this;
    }

    /**
     * Sets default value that will be used if the argument is omitted. This method could be used if the argument is set
     * to be optional and to be parsed to Double.<br/>
     * @see CommandArgumentConfigBuilder#setParseToDouble
     * @see CommandArgumentConfigBuilder#setOptional
     */
    public CommandArgumentConfigBuilder setDefaultDoubleValue(double num) {
        this.defaultDoubleValue = num;
        return this;
    }


    /**
     * When set to true the argument's value will be parsed to Double and will be accessible using
     * {@link CommandArguments#getDoubleArgumentValue}.<br/>
     */
    public CommandArgumentConfigBuilder setParseToDouble(boolean bool) {
        this.parseToDouble = bool;
        return this;
    }

    /**
     * Setting this to true enables custom argument choosing.
     * This means that instead of automatically splitting off all symbols before
     * whitespace from arguments string, the string will be given to
     * {@link AbstractCommand#chooseArgumentSymbols} and the method
     * will be able to process the splitting itself.
     * @see AbstractCommand#chooseArgumentSymbols(MessageReceivedEvent, String, int)
     */
    public CommandArgumentConfigBuilder enableCustomArgumentChoosing(boolean enable) {
        this.multiWordChoosingEnabled = enable;
        return this;
    }


    CommandArgumentConfig build() {
        return new CommandArgumentConfig(id, argumentName, argumentOptions, stringValidators, doubleValidators, optional, argumentDesc, parseToDouble, defaultStringValue, defaultDoubleValue, inQuotes, errorMsg, multiWordChoosingEnabled);
    }

    boolean isOptional() {
        return optional;
    }

    String getArgumentName() {
        return argumentName;
    }

    List<String> getArgumentOptions() {
        return argumentOptions;
    }
}

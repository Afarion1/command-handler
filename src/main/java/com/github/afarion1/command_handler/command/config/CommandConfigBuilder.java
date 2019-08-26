package com.github.afarion1.command_handler.command.config;

import com.github.afarion1.command_handler.command.AbstractCommand;
import com.github.afarion1.command_handler.command.CommandArguments;
import com.github.afarion1.command_handler.internal_commands.CmdCommandList;
import net.dv8tion.jda.core.Permission;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * A builder for convenient {@link CommandConfig} instance creation.
 */
//TODO add @see constructor
public final class CommandConfigBuilder {

    private List<String> nameAndAliases;
    private List<CommandArgumentConfigBuilder> arguments;
    private List<Permission> discordPerms;
    private String desc;
    private String verboseDesc;
    private String rawArgsDesc;
    private String rawArgsName;
    private CommandListType listType;
    private Duration commandCooldown;
    private Duration commandGuildCooldown;
    private boolean executeInGuildOnly;
    private boolean rawArgs;
    private boolean executeIfCantCheckCooldown;
    private boolean cleanCooldownRecords;
    private Duration cooldownRecordsCleaningPeriod;


    /**
     * Creates a configuration builder for a {@link AbstractCommand}.<br>
     * The default setting are following:<br>
     * <ul>
     * <li>has no description</li>
     * <li>is visible in list of commands, etc.</li>
     * <li>doesn't require any discord permissions</li>
     * <li>has no cooldown</li>
     * <li>has no per guild cooldown</li>
     * <li>has no arguments</li>
     * <li>doesn't use raw arguments</li>
     * <li>could be executed not only in guild chat</li>
     * <li>shouldn't be executed if can't check cooldown (db problems)</li>
     * <li>command's cooldown entries should be cleared on startup if they are outdated</li>
     * </ul>
     *
     * @param name the name will be used to call the command. It should not be null
     *             nor have length of zero.
     */
    public CommandConfigBuilder(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Name should be not null and with length 1 or morel");
        this.nameAndAliases = new ArrayList<>();
        this.nameAndAliases.add(name);
        this.desc = "";
        this.verboseDesc = "";
        this.listType = CommandListType.LISTED;
        this.commandCooldown = Duration.ZERO;
        this.commandGuildCooldown = Duration.ZERO;
        this.discordPerms = new ArrayList<>();
        this.arguments = new ArrayList<>();
        this.executeInGuildOnly = false;
        this.rawArgs = false;
        this.rawArgsName = "";
        this.rawArgsDesc = "";
        this.executeIfCantCheckCooldown = false;
        this.cleanCooldownRecords = true;
        this.cooldownRecordsCleaningPeriod = Duration.ZERO;
    }

    //TODO implement
//    public CommandConfigBuilder setCooldownRecordsCleaningPeriod(Duration period) {
//        if (period == null) period = Duration.ZERO;
//        this.cooldownRecordsCleaningPeriod = period;
//        return this;
//    }

    /**
     * Should outdated records of cooldown of the command be cleared.<br>
     * Default is true.
     */
    public CommandConfigBuilder setCleanCooldownRecords(boolean bool) {
        this.cleanCooldownRecords = bool;
        return this;
    }

    /**
     * Setting to true will allow the command to be used when there are problems with DB. If the command has no cooldown,
     * it will be executed regardless.<br>
     * Default is false.
     */
    public CommandConfigBuilder setExecuteIfCantCheckOrSaveCooldown(boolean bool) {
        this.executeIfCantCheckCooldown = bool;
        return this;
    }

    /**
     * Adds arguments to this command. The arguments and it's descriptions will be added to the description of this
     * command. The values of the arguments will be accessible using {@link CommandArguments} object
     */
    public CommandConfigBuilder addArguments(List<CommandArgumentConfigBuilder> args) {
        this.arguments.addAll(args);
        return this;
    }

    /**
     * Adds arguments to this command. The arguments and it's descriptions will be added to the description of this
     * command. The values of the arguments will be accessible using {@link CommandArguments} object
     */
    public CommandConfigBuilder addArguments(CommandArgumentConfigBuilder... arg) {
        this.arguments.addAll(Arrays.asList(arg));
        return this;
    }

    /**
     * Sets this command to use raw args, which means the arguments values will be accessible as a one String
     * or as List of the string separated by spaces instead of IDs and all the features coming from
     * {@link CommandArgumentConfigBuilder} won't be available.
     */
    public CommandConfigBuilder setUseRawArgs(boolean bool) {
        this.rawArgs = bool;
        return this;
    }

    /**
     * The command will be accessible in the same guild only after the duration since the last use.
     */
    public CommandConfigBuilder setPerGuildCooldown(Duration cooldown) {
        if (cooldown != null && !cooldown.equals(Duration.ZERO)) {
            this.commandGuildCooldown = cooldown;
            this.executeInGuildOnly = true;
        }
        return this;
    }

    /**
     * Adds a Discord permission that will be required in order to execute the command.
     */
    public CommandConfigBuilder addDiscordPermission(Permission perm) {
        this.discordPerms.add(perm);
        return this;
    }

    /**
     * Adds Discord permissions that will be required in order to execute the command.
     */
    public CommandConfigBuilder addDiscordPermission(List<Permission> perms) {
        this.discordPerms.addAll(perms);
        return this;
    }

    /**
     * The same user will have to wait the duration before being able to execute the command again
     */
    public CommandConfigBuilder setCooldown(Duration cooldown) {
        if (cooldown != null)
            this.commandCooldown = cooldown;
        return this;
    }

    /**
     * Adds an alias to the command which will work like additional name.
     * <br>
     * If a message from discord matches several different command names or aliases, then the longest one will be  processes.
     * <br>
     * To avoid confusion it is generally better to not make overlapping aliases if the command has arguments.
     * For example, <i>pour water</i> and <i>pour</i>, let's say the command takes one argument, which is name of target.
     * Then, if the name of the target is water, the command will start processing without arguments, because the alias
     * and the argument will match the full name, which will be preferred due to bigger length.
     */
    public CommandConfigBuilder addAliases(List<String> aliases) {
        this.nameAndAliases.addAll(aliases);
        return this;
    }

    /**
     * Adds an alias to the command which will work like additional name.
     * <p>
     * If a message from discord matches several different command names or aliases, then the longest one will be processed.
     * <p>
     * To avoid confusion it is generally better to not make overlapping aliases if the command has arguments.
     * For example, <i>pour water</i> and <i>pour</i>, let's say the command takes one argument, which is name of target.
     * Then, if the name of the target is water, the command will start processing without arguments, because the alias
     * and the argument will match the full name, which will be preferred due to bigger length.
     */
    public CommandConfigBuilder addAliases(String... aliases) {
        this.nameAndAliases.addAll(Arrays.asList(aliases));
        return this;
    }

    /**
     * Sets a description that will be displayed in list of commands and in commands information (inspection)
     */
    public CommandConfigBuilder setDescription(String desc) {
        this.desc = desc;
        return this;
    }


    /**
     * The verbose description will be used in command inspection (command info command)
     */
    public CommandConfigBuilder setVerboseDescription(String verbDesc) {
        if (verbDesc == null) {
            verbDesc = "";
        }
        this.verboseDesc = verbDesc;
        return this;
    }

    /**
     * Sets visibility of the command.
     */
    public CommandConfigBuilder setCommandListType(CommandListType type) {
        this.listType = type;
        return this;
    }

    /**
     * When set to true the command won't be accessible in DM chat.
     */
    public CommandConfigBuilder setExecuteInGuildOnly(boolean bool) {
        this.executeInGuildOnly = bool;
        return this;
    }

    /**
     * Sets description of the raw args which will be displayed in verbose description of the command
     * if usage of raw args is enabled.
     */
    public CommandConfigBuilder setRawArgsDesc(String desc) {
        this.rawArgsDesc = desc;
        return this;
    }

    /**
     * Sets name of the raw args which will be used in signature and description of the command, if usage of raw
     * args is enabled.
     */
    public CommandConfigBuilder setRawArgsName(String name) {
        this.rawArgsName = name;
        return this;
    }

    /**
     * @return final and validated command config
     */
    public CommandConfig build() {
        List<CommandArgumentConfig> finalArgs = arguments.stream()
                .map(CommandArgumentConfigBuilder::build)
                .collect(Collectors.toList());
        validate(finalArgs);
        String commandArgumentsSignature = generateCmdArgsSignature();
        List<String> aliasesCopy = new ArrayList<>(nameAndAliases);
        List<Permission> permissionsCopy = new ArrayList<>(discordPerms);


        return new CommandConfig(aliasesCopy, desc, verboseDesc, listType, commandCooldown, permissionsCopy, commandGuildCooldown, finalArgs, executeInGuildOnly, rawArgs, rawArgsName, rawArgsDesc, executeIfCantCheckCooldown, commandArgumentsSignature, cleanCooldownRecords, cooldownRecordsCleaningPeriod);
    }

    private void validate(List<CommandArgumentConfig> args) {
        if (!commandGuildCooldown.equals(Duration.ZERO) && !executeInGuildOnly)
            throw new IllegalArgumentException("If a command uses Guild Cooldown, in should be executeInGuildOnly");
        boolean prevOptional = false;
        for (CommandArgumentConfig argCfg : args) {

            //not sure what was the purpose of it
//            if (argProps.isOptional()) {
//                if (argProps.isParseToDouble()) {
//                    if (Double.isNaN(argProps.getDefaultDoubleValue()))
//                        throw new IllegalArgumentException("If an argument is optional and marked as parse to double, then default double value should be set");
//                } else {
//                    if (argProps.getDefaultStringValue() == null)
//                        throw new IllegalArgumentException("If an argument is optional, then default string value should be set");
//                }
//            }

            if(argCfg.isParseToDouble() && !argCfg.getArgumentOptions().isEmpty())
                throw new IllegalArgumentException("An argument must be either parse to double or have String options");

            if(!argCfg.getDoubleValidators().isEmpty() && !argCfg.isParseToDouble())
                throw new IllegalArgumentException("If an argument has double validators, it must be set to parse to " +
                        "double as well.");

            if(!argCfg.getStringValidators().isEmpty() && (argCfg.isMultiWordChoosingEnabled() || argCfg.isParseToDouble()))
                throw new IllegalArgumentException("An argument must either have String validators or be set to" +
                        "multi word choosing/paring to double");

            if (argCfg.isMultiWordChoosingEnabled() && argCfg.isInQuotes())
                throw new IllegalArgumentException("An argument must be either in quotes or have multi word choosing enabled");

            if ((argCfg.isMultiWordChoosingEnabled() || argCfg.isInQuotes()) && argCfg.isParseToDouble())
                throw new IllegalArgumentException("An argument must be either multi word/in quotes or parseToDouble");

            if (prevOptional && !argCfg.isOptional())
                throw new IllegalArgumentException("If an argument is optional, then all the following arguments should be optional as well");
            prevOptional = argCfg.isOptional();
        }
    }

    @NotNull
    private String generateCmdArgsSignature() {
        StringBuilder sbFieldName = new StringBuilder(nameAndAliases.get(0));
        if (rawArgs) {
            sbFieldName.append(" ");
            sbFieldName.append(rawArgsName);
        } else if (arguments.size() > 0) {
            for (CommandArgumentConfigBuilder arg : arguments) {
                sbFieldName.append(" ");
                if (arg.isOptional()) {
                    sbFieldName.append(CmdCommandList.optionalArgsPrefix);
                    sbFieldName.append(arg.getArgumentName().toLowerCase());
                    appendOptions(sbFieldName, arg);
                    sbFieldName.append(CmdCommandList.optionalArgsSuffix);
                } else {
                    sbFieldName.append(CmdCommandList.necessaryArgsPrefix);
                    sbFieldName.append(arg.getArgumentName().toLowerCase());
                    appendOptions(sbFieldName, arg);
                    sbFieldName.append(CmdCommandList.necessaryArgsSuffix);
                }
            }
        }
        return sbFieldName.toString();
    }

    private static void appendOptions(StringBuilder fieldName, CommandArgumentConfigBuilder arg) {
        if (!arg.getArgumentOptions().isEmpty()) {
            fieldName.append(CmdCommandList.ARGS_OPTION_PREFIX);
            for (int j = 0; j < arg.getArgumentOptions().size(); j++) {
                if (j > 0) fieldName.append("/");
                fieldName.append(arg.getArgumentOptions().get(j));
            }
            fieldName.append(CmdCommandList.ARGS_OPTION_SUFFIX);
        }
    }
}

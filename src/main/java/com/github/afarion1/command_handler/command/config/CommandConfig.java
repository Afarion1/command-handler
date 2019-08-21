package com.github.afarion1.command_handler.command.config;

import net.dv8tion.jda.core.Permission;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Represent configuration of command
 */
public final class CommandConfig {
    private final List<String> nameAndAliases;
    private final String desc;
    private final String verboseDesc;
    private final CommandListType listType;
    private final Duration commandCooldown;
    private final List<Permission> discordPerms;
    private final Duration commandGuildCooldown;
    private final List<CommandArgumentConfig> arguments;
    private final boolean executeInGuildOnly;
    private final boolean rawArgs;
    private final String rawArgsName;
    private final String rawArgsDesc;
    private final boolean executeIfCantCheckCooldown;
    private final String commandArgumentsSignature;
    private final boolean cleanCooldownRecords;
    private final Duration cooldownRecordsCleaningPeriod;


    CommandConfig(List<String> nameAndAliases, String desc, String verboseDesc, CommandListType listType, Duration commandCooldown, List<Permission> discordPerms, Duration commandGuildCooldown, List<CommandArgumentConfig> arguments, boolean executeInGuildOnly, boolean rawArgs, String rawArgsName, String rawArgsDesc, boolean executeIfCantCheckCooldown, String commandArgumentsSignature, boolean cleanCooldownRecords, Duration cooldownRecordsCleaningPeriod) {
        //keep everything immutable
        this.nameAndAliases = Collections.unmodifiableList(nameAndAliases);
        this.desc = desc;
        this.verboseDesc = verboseDesc;
        this.listType = listType;
        this.commandCooldown = commandCooldown;
        this.discordPerms = Collections.unmodifiableList(discordPerms);
        this.commandGuildCooldown = commandGuildCooldown;
        this.arguments = Collections.unmodifiableList(arguments);
        this.executeInGuildOnly = executeInGuildOnly;
        this.rawArgs = rawArgs;
        this.rawArgsName = rawArgsName;
        this.rawArgsDesc = rawArgsDesc;
        this.executeIfCantCheckCooldown = executeIfCantCheckCooldown;
        this.commandArgumentsSignature = commandArgumentsSignature;
        this.cleanCooldownRecords = cleanCooldownRecords;
        this.cooldownRecordsCleaningPeriod = cooldownRecordsCleaningPeriod;
    }

    public List<String> getNameAndAliases() {
        return nameAndAliases;
    }

    public String getName() {
        return nameAndAliases.get(0);
    }

    public String getDescription() {
        return desc;
    }

    public String getVerboseDesc() {
        return verboseDesc;
    }

    public CommandListType getListType() {
        return listType;
    }

    public Duration getUserCooldown() {
        return commandCooldown;
    }

    public List<Permission> getDiscordPerms() {
        return discordPerms;
    }

    public Duration getGuildCooldown() {
        return commandGuildCooldown;
    }

    public List<CommandArgumentConfig> getArguments() {
        return arguments;
    }

    public boolean isExecuteInGuildOnly() {
        return executeInGuildOnly;
    }

    public boolean isRawArgs() {
        return rawArgs;
    }

    public String getRawArgsName() {
        return rawArgsName;
    }

    public String getRawArgsDesc() {
        return rawArgsDesc;
    }

    public boolean shouldExecuteIfCantCheckOrSaveCooldown() {
        return executeIfCantCheckCooldown;
    }

    public String getCommandArgumentsSignature() {
        return commandArgumentsSignature;
    }

    public boolean shouldCleanCooldownRecords() {
        return cleanCooldownRecords;
    }

    public Duration getCooldownRecordsCleaningPeriod() {
        return cooldownRecordsCleaningPeriod;
    }

}

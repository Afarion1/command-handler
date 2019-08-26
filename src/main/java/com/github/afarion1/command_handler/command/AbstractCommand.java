package com.github.afarion1.command_handler.command;

import com.github.afarion1.command_handler.command.config.CommandArgumentConfigBuilder;
import com.github.afarion1.command_handler.annotations.Config;
import com.github.afarion1.command_handler.command.config.CommandArgumentConfig;
import com.github.afarion1.command_handler.command.config.CommandConfig;
import com.github.afarion1.command_handler.command.config.CommandListType;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AbstractCommand {

    public static final String argumentsSeparator = " ";
    private static final Logger log = LoggerFactory.getLogger(AbstractCommand.class);

    private CommandConfig config;
    private CommandHandler handler;

    protected AbstractCommand(CommandHandler handler) {
        this.handler = handler;
        this.config = handler.getCommandConfig(this.getClass());
        if(config == null)
            throw new RuntimeException("Command's config wasn't found. The command must be not registered on provided CommandHandler");
    }


    public final CommandConfig getConfig() {
        return config;
    }

    public final CommandHandler getHandler() {
        return handler;
    }

    public final String getName() {
        return config.getNameAndAliases().get(0);
    }

    public final String getDescription() {
        return config.getDescription();
    }

    public final String getVerboseDesc() {
        return config.getVerboseDesc();
    }

    public final CommandListType getListType() {
        return config.getListType();
    }

    /**
     * @return unmodifiable name and aliases list
     */
    public final List<String> getNameAndAliases() {
        return Collections.unmodifiableList(config.getNameAndAliases());
    }

    /**
     * @return a list of {@link CommandArgumentConfigBuilder} which is backed by Command Handler, so changes to this list will
     * affect the command's arguments
     */
    public final List<CommandArgumentConfig> getCommandArguments() {
        return config.getArguments();
    }

    //TODO rename, add javadoc
    public final String getCmdWithArgsSignature() {
        return config.getCommandArgumentsSignature();
    }

    public final boolean isRawArgs() {
        return config.isRawArgs();
    }

    public final String getRawArgsDesc() {
        return config.getRawArgsDesc();
    }

    public final String getRawArgsName() {
        return config.getRawArgsName();
    }

    /**
     * Could be overridden for dynamic calculation instead of static {@link Config}.
     * @param event context for overriding
     * @return true if the command should be executed only in a guild chat
     */
    public boolean isExecuteInGuildOnly(MessageReceivedEvent event) {
        return config.isExecuteInGuildOnly();
    }

    /**
     * Could be overridden so {@link #getUserCooldown(MessageReceivedEvent)}
     * doesn't get called to determine if there's any cooldown.<br>
     * Could be useful if the {@link #getUserCooldown(MessageReceivedEvent)}
     * is overridden and has heavy operations.
     * @param event context for overriding
     * @return true if the command has user cooldown
     */
    public boolean hasUserCooldown(MessageReceivedEvent event) {
        return !(getUserCooldown(event) == null || getUserCooldown(event).equals(Duration.ZERO));
    }

    /**
     * Could be overridden so {@link #getGuildCooldown(MessageReceivedEvent)}
     * doesn't get called to determine if there's any cooldown.<br>
     * Could be useful if the {@link #getGuildCooldown(MessageReceivedEvent)}
     * is overridden and has heavy operations.
     * @param event context for overriding
     * @return true if the command has guild cooldown
     */
    public boolean hasGuildCooldown(MessageReceivedEvent event) {
        return !(getGuildCooldown(event) == null || getGuildCooldown(event).equals(Duration.ZERO));
    }

    public final boolean hasAnyCooldown(MessageReceivedEvent event) {
        return hasUserCooldown(event) || hasGuildCooldown(event);
    }

    /**
     * Could be overridden for dynamic calculation instead of static {@link Config}.
     * @param event context for overriding
     * @return true if the command should be executed even if there
     */
    public boolean shouldExecuteIfCantCheckOrSaveCooldown(MessageReceivedEvent event) {
        return config.shouldExecuteIfCantCheckOrSaveCooldown();
    }

    /**
     * Could be overridden for dynamic cooldown calculation instead of static {@link Config}.<br>
     * @param event context for overriding
     * @return minimal duration between uses of the same command by the same user
     */
    public Duration getUserCooldown(MessageReceivedEvent event) {
        return config.getUserCooldown();
    }

    public Duration getGuildCooldown(MessageReceivedEvent event) {
        return config.getGuildCooldown();
    }

    /**
     * Could be overridden for dynamic needed discord permissions calculation instead of static {@link Config}.<br>
     * @param event context for overriding
     * @return needed discord permission to execute the command
     */
    public List<Permission> getRequiredDiscordPerms(MessageReceivedEvent event) {
        return config.getDiscordPerms();
    }

    /**
     * Designed to be overridden if {@link CommandArgumentConfigBuilder#enableCustomArgumentChoosing(boolean)}
     * of any argument of the command is set to true. <br>
     * This method will have to choose amount of consecutive symbols right from the start of a string, the symbols
     * will be value of the argument.<br>
     * The method will be given {@link MessageReceivedEvent} for context, a String which represents
     * the rest of unprocessed arguments sent by user, and ID of argument being processed.
     *
     * @param event jda on message received event, given for context
     * @param strArgs string containing the rest of unprocessed arguments sent by user
     * @param argumentId id of argument being processed. Necessary if several arguments marked to use
     *                   custom argument choosing
     * @return when overridden, should return amount of chosen symbols, it other words index of last symbol needed + 1
     *
     * @see CommandArgumentConfigBuilder#enableCustomArgumentChoosing(boolean)
     */
    public int chooseArgumentSymbols(MessageReceivedEvent event, String strArgs, int argumentId) {
        return 0;
    }

    abstract public void execute(MessageReceivedEvent event, CommandArguments args);

    List<Permission> getUnsatisfiedPermissions(MessageReceivedEvent event) {
        return getRequiredDiscordPerms(event).stream()
                .filter(perm -> !PermissionUtil.checkPermission(event.getTextChannel(), event.getMember(), perm))
                .collect(Collectors.toList());
    }

}

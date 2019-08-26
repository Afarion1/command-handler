package com.github.afarion1.command_handler.command;

import com.github.afarion1.command_handler.annotations.Command;
import com.github.afarion1.command_handler.annotations.Config;
import com.github.afarion1.command_handler.command.config.CommandArgumentConfig;
import com.github.afarion1.command_handler.command.config.CommandConfig;
import com.github.afarion1.command_handler.command.config.CommandConfigBuilder;
import com.github.afarion1.command_handler.command.config.CommandListType;
import com.github.afarion1.command_handler.internal_commands.CmdCommandList;
import com.github.afarion1.command_handler.internal_commands.CmdInspectCommand;
import it.unimi.dsi.fastutil.ints.IntList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents CommandHandler entity. Note that running several CommandHandlers in one
 * project is not yet supported (they would share config files and cooldown tables in
 * database).
 */
@SuppressWarnings("WeakerAccess")
public final class CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    private final Map<Class<? extends AbstractCommand>, CommandConfig> commandConfigMap = new HashMap<>();
    private final Map<Class<? extends AbstractCommand>, Function<CommandHandler,? extends AbstractCommand>>
            commandSupplierMap = new HashMap<>();
    private final Map<String, Class<? extends AbstractCommand>> commandAliasesMap = new HashMap<>();
    private final List<CommandConfig> visibleCommandConfigList = new ArrayList<>();

    private final JDA jda;
    private final ExecutorService executor;
    private final String commandsPrefix;
    private final Color helpColor;
    private final Color errorColor;
    private final Color inspectCommandColor;
    private final boolean enableCommandList;
    private final boolean enableInspectCommand;
    private final boolean cleanDbOnStartup;

    private boolean started = false;

    CommandHandler(CommandHandlerBuilder cfg) {
        this.jda = cfg.jda;
        this.executor = cfg.executor;
        this.commandsPrefix = cfg.commandsPrefix;
        this.helpColor = cfg.commandListColor;
        this.errorColor = cfg.errorColor;
        this.inspectCommandColor = cfg.inspectCommandColor;
        this.enableCommandList = cfg.enableCommandList;
        this.enableInspectCommand = cfg.enableInspectCommand;
        this.cleanDbOnStartup = cfg.cleanDbOnStartup;
    }


    /**
     * Initializes and starts listening for messages. <br>
     * Commands should be registered before calling the method.
     * @see #registerCommand(Class, Function, CommandConfig)
     * @see Command
     */
    public void start() {
        if(started){
            throw new IllegalStateException("The handler has already started");
        }

        if (isCommandListEnabled()) {
            log.info("Command list is enabled, registering the command");
            registerCommand(CmdCommandList.class);
        } else {
            log.info("Command list is disabled");
        }

        if (isInspectCommandEnabled()) {
            log.info("Inspect command is enabled, registering the command");
            registerCommand(CmdInspectCommand.class);
        } else {
            log.info("Inspect command is disabled");
        }

        registerAnnotatedCommands();

        Database.init();
        if (cleanDbOnStartup) Database.deleteOutdatedEntries(this);

        jda.addEventListener(new MessageListener(this));
        started = true;
    }

    /**
     * Registers a command to be handled.<br>
     * {@link Command} annotation could be used alternatively, it is usually more convenient.<br>
     * Unlike using annotation to register, using the method provides an option to use a different
     * from the {@link AbstractCommand} super class constructor for the command class. <br>
     * Function and config parameters could be null, overloaded versions of the method are present
     * for convenience.
     * @param aClass class of the command
     * @param function a function to provide instances of the command with different from  the
     *                {@link AbstractCommand} super class constructor. A reference to
     *                {@link CommandHandler} is given for super() call inside of the constructor.
     *                 If the parameter is null, a constructor with single parameter of
     *                 {@link CommandHandler} is expected
     *
     * @param config configuration of the command. If the parameter is null, the configuration
     *               {@link CommandConfig} object is expected to be annotated with {@link Config}
     *               inside of the command's class
     * @param <T> subclass of {@link AbstractCommand}
     * @see Command
     * @see Config
     * @see CommandConfigBuilder
     */
    public <T extends AbstractCommand> void registerCommand(@NotNull Class<T> aClass, Function<CommandHandler, T> function, CommandConfig config) {
        if(started)
            throw new IllegalStateException("The handler has already started. Command registration is only available " +
                    "before the start.");
        //call other overloaded versions if argument is null
        if (function == null) {
            if (config == null)
                registerCommand(aClass);
            else
                registerCommand(aClass, config);
            return;
        } else {
            if (config == null) {
                registerCommand(aClass, function);
                return;
            }
        }

        commandConfigMap.put(aClass, config);
        commandSupplierMap.put(aClass, function);
        for (String alias : config.getNameAndAliases()) {
            commandAliasesMap.put(alias, aClass);
        }
        if (config.getListType().equals(CommandListType.LISTED))
            visibleCommandConfigList.add(config);

        log.trace("Registered command {}", config.getName());
    }

    /**
     * Convenience method, equals to {@link #registerCommand(Class, Function, CommandConfig)} with
     * 3rd parameter of null.
     * @see #registerCommand(Class, Function, CommandConfig)
     */
    public <T extends AbstractCommand> void registerCommand(Class<T> aClass, Function<CommandHandler, T> supplier) {
        CommandConfig config = getAnnotatedCommandConfig(aClass);
        registerCommand(aClass, supplier, config);
    }

    /**
     * Convenience method, equals to {@link #registerCommand(Class, Function, CommandConfig)} with
     * 2nd parameter of null.
     * @see #registerCommand(Class, Function, CommandConfig)
     */
    public <T extends AbstractCommand> void registerCommand(Class<T> aClass, CommandConfig config) {
        Function<CommandHandler, T> supplier = commandConstructorSupplier(aClass);
        registerCommand(aClass, supplier, config);
    }

    /**
     * Convenience method, equals to {@link #registerCommand(Class, Function, CommandConfig)} with
     * 2nd and 3rd parameters of null.
     * @see #registerCommand(Class, Function, CommandConfig)
     */
    public <T extends AbstractCommand> void registerCommand(Class<T> aClass) {
        Function<CommandHandler, T> supplier = commandConstructorSupplier(aClass);
        CommandConfig config = getAnnotatedCommandConfig(aClass);
        registerCommand(aClass, supplier, config);
    }

    /**
     * Useful for custom command list implementation.
     * @return unmodifiable list of all {@link CommandConfig} with {@link CommandListType#LISTED}
     */
    public List<CommandConfig> getVisibleCommandConfigList() {
        return Collections.unmodifiableList(visibleCommandConfigList);
    }

    /**
     * Useful for custom command inspection implementation.
     * @return unmodifiable set of all command names and aliases
     * @see CmdInspectCommand
     */
    public Set<String> getAllCommandNamesAndAliases() {
        return Collections.unmodifiableSet(commandAliasesMap.keySet());
    }

    /**
     * Useful for custom command inspection implementation.
     * @return a config or null if the config wasn't found
     * @see CmdInspectCommand
     */
    public CommandConfig findCommandAndGetConfig(String name) {
        return commandConfigMap.get(commandAliasesMap.get(name));
    }

    /**
     * @return JDA that is used to listen to messages
     */
    public JDA getJda() {
        return jda;
    }

    /**
     * @return default prefix for command call recognition
     */
    public String getCommandsPrefix() {
        return commandsPrefix;
    }

    /**
     * @return color of command list's embed
     */
    public Color getCommandListColor() {
        return helpColor;
    }

    /**
     * @return color of command error's embed
     */
    public Color getErrorColor() {
        return errorColor;
    }

    /**
     * @return color of inspect command's embed
     */
    public Color getInspectCommandColor() {
        return inspectCommandColor;
    }

    /**
     * @return true if command list is enabled, false otherwise
     */
    public boolean isCommandListEnabled() {
        return enableCommandList;
    }

    /**
     * @return true if inspect command is enabled, false otherwise
     */
    public boolean isInspectCommandEnabled() {
        return enableInspectCommand;
    }

    /**
     * @return a config or null if the config wasn't found
     */
    CommandConfig getCommandConfig(Class<? extends AbstractCommand> aClass) {
        return commandConfigMap.get(aClass);
    }

    /**
     * @return unmodifiable collection of all command's configs
     */
    Collection<CommandConfig> getCommandConfigList() {
        return Collections.unmodifiableCollection(commandConfigMap.values());
    }

    /**
     * @return executor that is used to process commands
     */
    ExecutorService getExecutor() {
        return executor;
    }

    private void registerAnnotatedCommands() {
        Reflections refl = new Reflections("");

        Set<Class<?>> annotated = refl.getTypesAnnotatedWith(Command.class);
        @SuppressWarnings("unchecked")
        List<Class<? extends AbstractCommand>> commands = annotated.stream()
                .filter(t -> t.getSuperclass() == AbstractCommand.class)
                .map(t -> (Class<? extends AbstractCommand>) t)
                .collect(Collectors.toList());

        for (Class<? extends AbstractCommand> cmd : commands) {
            registerCommand(cmd);
        }
    }

    /**
     * @return command's class or null if wasn't found
     */
    Class<? extends AbstractCommand> findCommand(String name) {
        return commandAliasesMap.get(name);
    }

    /**
     * @return new instance if command is registered or null otherwise
     */
    <T extends AbstractCommand> T newCommandInstance(Class<T> aClass) {
        if (aClass == null)
            return null;
        @SuppressWarnings("unchecked")
        T instance = (T) commandSupplierMap.get(aClass).apply(this);
        return instance;
    }

    @NotNull
    private CommandConfig getAnnotatedCommandConfig(Class<? extends AbstractCommand> aClass) {
        Method configMethod = null;
        for (Method declaredMethod : aClass.getDeclaredMethods()) {
            if (declaredMethod.isAnnotationPresent(Config.class)) {
                configMethod = declaredMethod;
                break;
            }
        }

        CommandConfig config;
        if (configMethod != null) {
            Object methodResult;
            try {
                configMethod.setAccessible(true);
                methodResult = configMethod.invoke(this, (Object[]) null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Unable to get CommandConfig", e);
            }

            if (methodResult instanceof CommandConfig)
                config = (CommandConfig) methodResult;
            else
                throw new IllegalArgumentException("Method annotated with @Config is expected to return CommandConfig, but returned " + methodResult);
        } else {


            Field configField = null;
            for (Field declaredField : aClass.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Config.class)) {
                    configField = declaredField;
                    break;
                }
            }
            if (configField == null)
                throw new RuntimeException("Couldn't find any config method/field. Please create a method/field that" +
                        "will contain/return CommandConfig.");

            Object fieldContent;
            try {
                configField.setAccessible(true);
                fieldContent = configField.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to get CommandConfig", e);
            }

            if (fieldContent instanceof CommandConfig)
                config = (CommandConfig) fieldContent;
            else
                throw new IllegalArgumentException("Field annotated with @Config is expected to contain CommandConfig, but contains " + fieldContent);
        }

        return config;
    }

    @NotNull
    private <T extends AbstractCommand> Function<CommandHandler, T> commandConstructorSupplier(Class<T> aClass) {
        return ch -> {
            try {
                return aClass.getConstructor(CommandHandler.class).newInstance(ch);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create an instance of command. (If command has no constructor " +
                        "without parameters, then a supplier should be provided using another overloaded version of " +
                        "register method)", e);
            }
        };
    }

    void processCommand(MessageReceivedEvent event) {
        //remove prefix
        String commandString = event.getMessage().getContentRaw().substring(commandsPrefix.length()).trim();
        if (commandString.length() < 1) {
            log.trace("Nothing after prefix");
            return;
        }

        //find longest cmd name or alias match
        log.trace("Finding cmd name in the message");
        FindCommand findCommand = new FindCommand(commandString).invoke();
        AbstractCommand command = findCommand.getCommand();
        String cmdNameFoundBy = findCommand.getCmdFoundBy();
        if (command == null) return;
        log.trace("Found command {} by name {}", command.getName(), cmdNameFoundBy);


        if (command.isExecuteInGuildOnly(event) && event.getGuild() == null) {
            log.trace("The command could be executed only in server chat, aborting");
            event.getChannel().sendMessage("This command could be executed only in server chat.").queue();
            return;
        }

        boolean shouldExecuteIfCantCheckOrSaveCooldown = command.shouldExecuteIfCantCheckOrSaveCooldown(event);
        if (Database.noConnection() && command.hasAnyCooldown(event) && !shouldExecuteIfCantCheckOrSaveCooldown) {
            log.debug("Unable to execute the command due to DB issues.");
            event.getChannel().sendMessage("Unable to execute the command due to DB issues").queue();
            return;
        }

        //separate args
        String argumentString = commandString.substring(cmdNameFoundBy.length());
        //process args
        log.trace("Processing arguments");
        CommandArguments cmdArgs;
        if (command.isRawArgs())
            cmdArgs = CommandArgumentsFactory.processRawArguments(argumentString);
        else
            cmdArgs = CommandArgumentsFactory.processArguments(command, argumentString, event);

        if (!cmdArgs.areValid()) {
            log.trace("Arguments are invalid");
            sendErrorMessage(event, command, cmdArgs);
            return;
        }
        log.trace("Arguments are valid");

        List<Permission> unsatisfiedPermissions = command.getUnsatisfiedPermissions(event);
        if (!unsatisfiedPermissions.isEmpty()) {
            sendNotEnoughPermsMessage(event, unsatisfiedPermissions);
            return;
        }
        log.trace("Enough discord permissions");

        try{
            if(command.hasUserCooldown(event)){
                long cooledDownAfter = Database.getUserCooledDownDate(event.getAuthor().getIdLong(), command.getName());
                if(System.currentTimeMillis() < cooledDownAfter){
                    event.getChannel().sendMessage("The command is on cooldown: "
                            + DurationFormatUtils.formatDurationWords(
                            cooledDownAfter - System.currentTimeMillis(),
                            true, true)).queue();
                    log.debug("{} is on per user cooldown.", command.getName());
                    return;
                }
            }
        } catch (SQLException e) {
            handleCooldownSQLException(event, e);
            if(!shouldExecuteIfCantCheckOrSaveCooldown)
                return;
        }

        try{
            if(command.hasGuildCooldown(event)){
                long cooledDownAfter = Database.getGuildCooledDownDate(event.getGuild().getIdLong(), command.getName());
                if(System.currentTimeMillis() < cooledDownAfter){
                    event.getChannel().sendMessage("The command is on cooldown: "
                            + DurationFormatUtils.formatDurationWords(
                            cooledDownAfter - System.currentTimeMillis(),
                            true, true)).queue();
                    log.debug("{} is on per user cooldown.", command.getName());
                    return;
                }
            }
        } catch (SQLException e) {
            handleCooldownSQLException(event, e);
            if(!shouldExecuteIfCantCheckOrSaveCooldown)
                return;
        }


        try {
            if (command.hasUserCooldown(event)) {
                long cooledDownAfter = System.currentTimeMillis() + command.getUserCooldown(event).toMillis();
                Database.saveCommandUserCooldown(event.getAuthor().getIdLong(), command.getName(), cooledDownAfter);
            }
            if(command.hasGuildCooldown(event)) {
                long cooledDownAfter = System.currentTimeMillis() + command.getGuildCooldown(event).toMillis();
                Database.saveCommandGuildCooldown(event.getGuild().getIdLong(), command.getName(), cooledDownAfter);
            }
        } catch (SQLException e) {
            handleCooldownSQLException(event, e);
            if(!shouldExecuteIfCantCheckOrSaveCooldown)
                return;
        }

        executeCommand(event, command, cmdArgs);

    }

    private void handleCooldownSQLException(MessageReceivedEvent event, SQLException e) {
        log.error("Error while managing command's cooldown.", e);
        event.getChannel().sendMessage("Something went wrong while trying to manage cooldown of the command." +
                " Aborting execution.").queue();
    }

    private void sendNotEnoughPermsMessage(MessageReceivedEvent event, List<Permission> unsatisfiedPermissions) {
        StringBuilder sb = new StringBuilder();
        sb.append("You don't have ");
        sb.append(unsatisfiedPermissions.get(0).getName());
        for (int i = 1; i < unsatisfiedPermissions.size(); i++) {
            sb.append(", ");
            sb.append(unsatisfiedPermissions.get(i).getName());
        }
        sb.append(" permission");
        if (unsatisfiedPermissions.size() > 1)
            sb.append("s");
        sb.append("in order to execute this command.");
        event.getChannel().sendMessage(sb.toString()).queue();

        log.debug("Not enough discord permissions: {}", unsatisfiedPermissions);
    }

    private void sendErrorMessage(MessageReceivedEvent event, AbstractCommand command, CommandArguments cmdArgs) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Wrong command usage");
        eb.setColor(getErrorColor());
        eb.setDescription(command.getCmdWithArgsSignature() + "\n\nWrong arguments:");
        appendWrongArgs(command, cmdArgs, eb);
        event.getChannel().sendMessage(eb.build()).queue();
    }

    private void executeCommand(MessageReceivedEvent event, AbstractCommand command, CommandArguments cmdArgs) {
        try {
            log.debug("Executing command {} sent by {} in channel id {}", command.getName(),
                    event.getAuthor().getAsMention(), event.getChannel().getId());
            command.execute(event, cmdArgs);
            log.debug("Finished execution of command {} sent by {} in channel id {}", command.getName(),
                    event.getAuthor().getAsMention(), event.getChannel().getId());
        }  catch (Exception e) {
            log.error("Error while executing command", e);
            event.getChannel().sendMessage("Something went wrong while executing the command").queue();
        }
    }

    private static void appendWrongArgs(AbstractCommand cmd, CommandArguments args, EmbedBuilder eb) {
        IntList wrongArgsIds = args.getWrongArgsIds();

        for (CommandArgumentConfig arg : cmd.getCommandArguments()) {
            if (wrongArgsIds.contains(arg.getId())) {

                String fieldName = arg.getArgumentName().toLowerCase();
                String errorMsg = arg.getErrorMsg().length() > 0 ? arg.getErrorMsg() : arg.getArgumentDesc();
                eb.addField(fieldName, errorMsg, true);

            }
        }
    }

    private class FindCommand {
        private final String msg;
        private AbstractCommand command = null;
        private String cmdFoundBy = null;

        private FindCommand(String msg) {
            this.msg = msg;
        }

        private AbstractCommand getCommand() {
            return command;
        }

        private String getCmdFoundBy() {
            return cmdFoundBy;
        }

        private FindCommand invoke() {
            //choose longest option to avoid excessive arguments bug
            log.trace("All commands names and aliases: {}", getAllCommandNamesAndAliases());
            Optional<String> commandName = getAllCommandNamesAndAliases().stream()
                    .map(String::toLowerCase)
                    .filter(name -> msg.toLowerCase().indexOf(name) == 0)
                    .max(Comparator.comparingInt(String::length));

            if (commandName.isPresent()) {
                cmdFoundBy = commandName.get();
                Class<? extends AbstractCommand> aClass = findCommand(cmdFoundBy);
                command = newCommandInstance(aClass);
            }
            return this;
        }
    }
}

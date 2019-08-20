package afarion.command_handler.command;

import afarion.command_handler.utils.NamedThreadFactory;
import net.dv8tion.jda.core.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents configuration of Command handler for convenient instance building.<br/>
 * Parameters set through methods override the ones from config file. <br/>
 */
public final class CommandHandlerBuilder {
    private static final String KEY_PREFIX = "commands_prefix";
    private static final String KEY_COMMAND_LIST = "enable_command_list";
    private static final String KEY_INSPECT_COMMAND = "enable_inspect_command";
    private static final String KEY_CLEAN_DB = "clean_outdated_cooldown_on_startup";

    private static final Logger log = LoggerFactory.getLogger(CommandHandlerBuilder.class);

    public JDA jda = null;
    public String commandsPrefix = "~";
    public boolean enableCommandList = true;
    public boolean enableInspectCommand = true;
    public boolean cleanDbOnStartup = true;
    public Color commandListColor = Color.yellow;
    public Color inspectCommandColor = Color.yellow;
    public Color errorColor = Color.red;
    public ExecutorService executor = null;
    private final String fileName = "command_handler.properties";


    /**
     * Creates fluent configuration object for CommandHandler instance creation.<br/>
     * Equivalent of {@link #CommandHandlerBuilder(boolean)} with parameter true. A configuration file will be created
     * in root directory, to prevent the file creation use {@link #CommandHandlerBuilder(boolean)} with parameter
     * false. CommandHandler's parameters from the configuration are overridden by parameters set using methods of
     * the {@link CommandHandlerBuilder}.<br/>
     * Before calling {@link #build()} to create {@link CommandHandler} instance, JDA should be set using
     * {@link #setJda(JDA)} <br/>
     * Default values:
     * <ul>
     *     <li>Command prefix is ~</li>
     *     <li>Command list is enabled</li>
     *     <li>Command inspection is enabled</li>
     *     <li>Outdated cooldown entries will be removed from database</li>
     *     <li>Command list's embed color is {@link Color#yellow}</li>
     *     <li>Command inspection's embed color is {@link Color#yellow}</li>
     *     <li>Error's embed color is {@link Color#red}</li>
     * </ul>
     *  If no executor for command processing is specified, a FixedThreadPool with size of amount of available
     *  processors will be used.
     */
    public CommandHandlerBuilder() {
        this(true);
    }

    /**
     * Creates fluent configuration object for CommandHandler instance creation.<br/>
     * The single parameter determines whether a configuration file should be created in root directory.
     * CommandHandler's parameters from the configuration are overridden by parameters set using methods of
     * the {@link CommandHandlerBuilder}.<br/>
     * Before calling {@link #build()} to create {@link CommandHandler} instance, JDA should be set using
     * {@link #setJda(JDA)} <br/>
     * Default values:
     * <ul>
     *     <li>Command prefix is ~</li>
     *     <li>Command list is enabled</li>
     *     <li>Command inspection is enabled</li>
     *     <li>Outdated cooldown entries will be removed from database</li>
     *     <li>Command list's embed color is {@link Color#yellow}</li>
     *     <li>Command inspection's embed color is {@link Color#yellow}</li>
     *     <li>Error's embed color is {@link Color#red}</li>
     * </ul>
     *  If no executor for command processing is specified, a FixedThreadPool with size of amount of available
     *  processors will be used.
     */
    private CommandHandlerBuilder(boolean createConfigIfAbsent) {
        Properties prop;
        try {
            FileInputStream in = new FileInputStream(fileName);
            prop = new Properties();
            prop.load(in);
        } catch (IOException e) {
            log.warn("Unable to  find configuration file");
            log.debug(e.toString());
            prop = null;
        }
        if (prop == null) {
            if (!createConfigIfAbsent) return;
            prop = new Properties();
        }

        if (prop.containsKey(KEY_PREFIX)) {
            String commandsPrefixProb = prop.getProperty(KEY_PREFIX);
            log.trace("{} - Found property {} = {}", fileName, KEY_PREFIX, commandsPrefixProb);
            if (commandsPrefixProb != null && commandsPrefixProb.length() > 0)
                commandsPrefix = commandsPrefixProb;
        } else {
            prop.setProperty(KEY_PREFIX, commandsPrefix);
            log.trace("{} - Adding property {} with value {}", fileName, KEY_PREFIX, commandsPrefix);
        }

        enableCommandList = loadOrAddBoolProperty(prop, KEY_COMMAND_LIST, enableCommandList);
        enableInspectCommand = loadOrAddBoolProperty(prop, KEY_INSPECT_COMMAND, enableCommandList);
        cleanDbOnStartup = loadOrAddBoolProperty(prop, KEY_CLEAN_DB, cleanDbOnStartup);

        try (OutputStream out = new FileOutputStream(fileName)) {
            prop.store(out, "Configuration file for Command Handler.");
        } catch (IOException e) {
            log.error("Unable to create configuration file", e);
        }

    }

    private boolean loadOrAddBoolProperty(Properties prop, String key, boolean defaultBool) {
        if (prop.containsKey(key)) {
            String found = prop.getProperty(key).trim();
            log.trace("{} - Found property {} = {}", fileName, key, found);
            boolean parsedVal;
            if (found.equalsIgnoreCase("true")) parsedVal = true;
            else if (found.equalsIgnoreCase("false")) parsedVal = false;
            else {
                parsedVal = defaultBool;
                log.trace("Couldn't parse the value to a bool, setting value to {} (default)", defaultBool);
            }
            return parsedVal;
        } else {
            prop.setProperty(key, Boolean.toString(defaultBool));
            log.trace("{} - Adding property {} with value {}", fileName, key, defaultBool);
            return defaultBool;
        }
    }

    /**
     * @param jda the JDA the {@link CommandHandler} will be handling messages from.
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder setJda(JDA jda) {
        this.jda = jda;
        return this;
    }


    /**
     * Setting to false will prevent outdated cooldown entries from being deleted from the DB. <br/>
     * True by default.
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder cleanDbOnStartup(boolean cleanDbOnStartup) {
        this.cleanDbOnStartup = cleanDbOnStartup;
        return this;
    }


    /**
     * A thread pool with size of available processors amount is used by default.
     * @param executor the executor will be used for commands processing.
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder setExecutorService(ExecutorService executor) {
        this.executor = executor;
        return this;
    }

    /**
     * Default prefix is ~
     * @param commandsPrefix the prefix will be used to detect command call.
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder setCommandPrefix(String commandsPrefix) {
        this.commandsPrefix = commandsPrefix;
        return this;
    }

    /**
     * @param helpColor the color will be used for Command List's embed (the color to left of a message).
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder setCommandListEmbedColor(Color helpColor) {
        this.commandListColor = helpColor;
        return this;
    }
    /**
     * @param errorColor the color will be used for error embed (the color to left of a message).
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder setErrorEmbedColor(Color errorColor) {
        this.errorColor = errorColor;
        return this;
    }

    /**
     * @param inspectColor the color will be used for Inspect Command's embed (the color to left of a message).
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder setInspectCommandEmbedColor(Color inspectColor) {
        this.inspectCommandColor = inspectColor;
        return this;
    }

    /**
     * Disables internal Command List command.
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder disableCommandList() {
        this.enableCommandList = false;
        return this;
    }

    /**
     * Disables internal Inspect Command command.
     * @return instance of {@link CommandHandlerBuilder}. Useful for chaining
     */
    public CommandHandlerBuilder disableInspectCommand() {
        this.enableInspectCommand = false;
        return this;
    }

    /**
     * @return {@link CommandHandler} instance with the given configuration
     */
    public CommandHandler build() {

        if (jda == null)
            throw new IllegalStateException("JDA is not specified");

        if (executor == null) {
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            executor = Executors.newFixedThreadPool(availableProcessors, new NamedThreadFactory("Command handling thread"));
            log.info("Executor is not specified, using FixedThreadPool with size of {} (amount of available processors)",
                    availableProcessors);
        }

        return new CommandHandler(this);
    }

}

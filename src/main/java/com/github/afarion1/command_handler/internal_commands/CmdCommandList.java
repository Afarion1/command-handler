package com.github.afarion1.command_handler.internal_commands;

import com.github.afarion1.command_handler.annotations.Config;
import com.github.afarion1.command_handler.command.AbstractCommand;
import com.github.afarion1.command_handler.command.CommandArguments;
import com.github.afarion1.command_handler.command.CommandHandler;
import com.github.afarion1.command_handler.command.config.CommandArgumentConfigBuilder;
import com.github.afarion1.command_handler.command.config.CommandConfig;
import com.github.afarion1.command_handler.command.config.CommandConfigBuilder;
import com.github.afarion1.command_handler.command.config.CommandListType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class CmdCommandList extends AbstractCommand {

    public static final String ARGS_OPTION_SUFFIX = ")";
    public static final String ARGS_OPTION_PREFIX = "(";
    public static final String necessaryArgsSuffix = "]";
    public static final String necessaryArgsPrefix = "[";
    public static final String optionalArgsSuffix = "}";
    public static final String optionalArgsPrefix = "{";
    private static final String argumentsDescription = "Not necessary arguments - " +
            optionalArgsPrefix + optionalArgsSuffix +
            "\nNecessary arguments - " +
            necessaryArgsPrefix + necessaryArgsSuffix +
            "\nArgument's options - " +
            ARGS_OPTION_PREFIX + ARGS_OPTION_SUFFIX;
    private final static int perPage = 7;
    private final static int ARG_PAGE_ID = 0;

    private static final Logger log = LoggerFactory.getLogger(CmdCommandList.class);

    private final Color embedColor;
    private final String prefix;
    private final List<CommandConfig> visibleCommandList;

    public CmdCommandList(CommandHandler handler) {
        super(handler);
        this.embedColor = handler.getCommandListColor();
        this.prefix = handler.getCommandsPrefix();
        visibleCommandList = handler.getVisibleCommandConfigList();
    }

    @Config
    private static CommandConfig config() {
        return new CommandConfigBuilder("command list")
                .addAliases(Arrays.asList("commands", "cmds", "help"))
                .setCommandListType(CommandListType.UNLISTED)
                .setDescription("Displays list of commands")
                .setVerboseDescription("Displays list of commands" +
                        argumentsDescription)
                .addArguments(new CommandArgumentConfigBuilder(ARG_PAGE_ID, "page")
                        .setOptional(true)
                        .setParseToDouble(true)
                        .setDefaultDoubleValue(1)
                        .setArgumentDescription("Page number"))
                .build();
    }

    @Override
    public void execute(MessageReceivedEvent event, CommandArguments args) {
        int page = (int) args.getDoubleArgumentValue(ARG_PAGE_ID);

        MessageChannel ch = event.getChannel();
        int pagesAmount = visibleCommandList.size() / perPage;
        if (visibleCommandList.size() % perPage != 0) pagesAmount++;
        log.trace("Total pages amount: {}", pagesAmount);
        if (page > pagesAmount) {
            String msg = pagesAmount > 1 ? "There's a total of " + pagesAmount + " pages" : "There's a total of " + pagesAmount + " pages";
            ch.sendMessage(msg).queue();
            return;
        }

        log.trace("Displaying command list on page {}", page);
        MessageEmbed embed = setupEmbed(page, pagesAmount);
        ch.sendMessage(embed).queue();
    }

    @NotNull
    private MessageEmbed setupEmbed(int page, int pagesAmount) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Commands:");
        eb.setDescription("Type " + prefix + " before a command name to execute it\n" +
                argumentsDescription);
        eb.setColor(embedColor);

        for (int i = (page - 1) * perPage; i < visibleCommandList.size(); i++) {
            CommandConfig cfg = visibleCommandList.get(i);
            String fieldName = cfg.getCommandArgumentsSignature();
            eb.addField(fieldName, cfg.getDescription(), false);
        }

        eb.setFooter("Page " + page + " out of " + pagesAmount, "https://images.vexels.com/media/users/3/147198/isolated/preview/76aff801de0d17c4d313489fccc55fa6-sunlight-burst-icon-by-vexels.png");
        return eb.build();
    }

}

package afarion.command_handler.internal_commands;

import afarion.command_handler.annotations.Config;
import afarion.command_handler.command.AbstractCommand;
import afarion.command_handler.command.CommandArguments;
import afarion.command_handler.command.CommandHandler;
import afarion.command_handler.command.config.CommandArgumentConfig;
import afarion.command_handler.command.config.CommandArgumentConfigBuilder;
import afarion.command_handler.command.config.CommandConfig;
import afarion.command_handler.command.config.CommandConfigBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Set;

public class CmdInspectCommand extends AbstractCommand {
    private static final Logger log = LoggerFactory.getLogger(CmdInspectCommand.class);
    private static final int ARG_CMD_NAME_ID = 0;

    private final Color embedColor;
    private final CommandHandler handler;
    private final Set<String> commandNames;

    public CmdInspectCommand(CommandHandler handler) {
        super(handler);
        this.embedColor = handler.getInspectCommandColor();
        this.handler = handler;
        this.commandNames = handler.getAllCommandNamesAndAliases();
    }

    @Config
    private static CommandConfig config() {
        return new CommandConfigBuilder("inspect command")
                .addAliases("inspect")
                .setDescription("Shows information about a command")
                .addArguments(
                        new CommandArgumentConfigBuilder(ARG_CMD_NAME_ID, "Command name")
                                .setArgumentDescription("Name of the command you want to inspect")
                                .enableCustomArgumentChoosing(true)
                                .setOptional(true)
                                .setDefaultStringValue("inspect command"))
                .build();
    }


    @Override
    public void execute(MessageReceivedEvent event, CommandArguments args) {
        String argCmdName = args.getStringArgumentValue(ARG_CMD_NAME_ID);
        log.trace("Executing command inspection on {}", argCmdName);

        CommandConfig cfg = handler.findCommandAndGetConfig(argCmdName);

        if (cfg == null) {
            log.debug("Couldn't find command with name or alias {}", argCmdName);
            event.getChannel().sendMessage(argCmdName + " wasn't found.").queue();
            return;
        }

        MessageEmbed embed = setupEmbed(cfg);
        event.getChannel().sendMessage(embed).queue();
    }

    @Override
    public int chooseArgumentSymbols(MessageReceivedEvent event, String arguments, int argumentId) {
        if (argumentId != ARG_CMD_NAME_ID)
            return 0;

        //choose option with the biggest length
        IntList matches = new IntArrayList();
        for (String name : commandNames) {
            int index = arguments.indexOf(name);
            if(index == 0 && arguments.length() == name.length())
                matches.add(index + name.length());

        }
        return matches.stream().min(Integer::compareTo).orElse(0);
    }

    @NotNull
    private MessageEmbed setupEmbed(CommandConfig cfg) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(cfg.getName());
        StringBuilder sb = new StringBuilder();
        if (cfg.getVerboseDesc().length() > 0) {
            sb.append(cfg.getVerboseDesc());
        } else {
            sb.append(cfg.getDescription());
        }
        sb.append("\n");

        if (cfg.getNameAndAliases().size() > 1) {

            sb.append("\nAliases: **");
            sb.append(cfg.getNameAndAliases().get(1));
            sb.append("**");
            for (int i = 2; i < cfg.getNameAndAliases().size(); i++) {
                sb.append(", **");
                sb.append(cfg.getNameAndAliases().get(i));
                sb.append("**");
            }
        }

        if (cfg.getArguments().size() > 0) {
            sb.append("\n\nArguments:\n");
            for (CommandArgumentConfig arg : cfg.getArguments()) {
                String argumentInfo = arg.getArgumentDesc();
                argumentInfo += "\n";
                if (arg.isOptional()) argumentInfo += "Optional";
                if (arg.isOptional() && arg.isInQuotes()) argumentInfo += ", ";
                if (arg.isInQuotes()) argumentInfo += "Should be in quotes";

                eb.addField(arg.getArgumentName(), argumentInfo, false);
            }
        } else if (cfg.isRawArgs()) {
            eb.addField(cfg.getRawArgsName(), cfg.getRawArgsDesc(), false);
        }
        eb.setDescription(sb.toString());

        eb.setFooter("", "https://images.vexels.com/media/users/3/147198/isolated/preview/76aff801de0d17c4d313489fccc55fa6-sunlight-burst-icon-by-vexels.png");
        eb.setColor(embedColor);
        return eb.build();
    }
}

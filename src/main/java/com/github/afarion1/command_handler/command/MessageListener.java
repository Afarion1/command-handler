package com.github.afarion1.command_handler.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

final class MessageListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    private final String prefix;
    private final ExecutorService executor;
    private final CommandHandler handler;

    MessageListener(CommandHandler handler) {
        this.handler = handler;
        this.prefix = handler.getCommandsPrefix();
        this.executor = handler.getExecutor();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        Message msg = event.getMessage();
        String content = msg.getContentRaw();
        log.trace("Received message \"{}\" from user {}in channel {}", content,
                msg.getAuthor().getAsMention(), msg.getChannel().getId());

        //handle command if prefix matches
        if (content.length() > prefix.length() && content.substring(0, prefix.length()).equals(prefix)) {
            //TODO  shorten message on trace logging level
            log.debug("Found prefix, processing message  \"{}\" from user {} in channel id{}", content,
                    msg.getAuthor().getAsMention(), msg.getChannel().getId());
            Runnable processCommand = () -> handler.processCommand(event);

            executor.execute(processCommand);
        }
    }

}

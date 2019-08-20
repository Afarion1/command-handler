package org.afarion.command_handler.annotations;

import org.afarion.command_handler.command.AbstractCommand;
import org.afarion.command_handler.command.CommandHandler;
import org.afarion.command_handler.command.config.CommandConfig;
import org.afarion.command_handler.command.config.CommandConfigBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

/**
 * Used to mark configuration object {@link CommandConfig} for command registration.<br/>
 * The annotation is expected to be inside of a subclass of {@link AbstractCommand}
 * next to a field containing {@link CommandConfig} or a static method returning the
 * {@link CommandConfig}.
 * @see Command
 * @see CommandHandler#registerCommand(Class, Function, CommandConfig)
 * @see CommandConfigBuilder
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
}

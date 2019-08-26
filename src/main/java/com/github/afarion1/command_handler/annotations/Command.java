package com.github.afarion1.command_handler.annotations;

import com.github.afarion1.command_handler.command.AbstractCommand;
import com.github.afarion1.command_handler.command.CommandHandler;
import com.github.afarion1.command_handler.command.config.CommandConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

/**
 * Used to register a subclass of {@link AbstractCommand} to be handled.<br>
 * The command is expected to have a constructor with a single parameter of {@link CommandHandler}
 * and a {@link CommandConfig} instance inside a field/method annotated with {@link Config}.<br>
 * To use other constructor, use {@link CommandHandler#registerCommand(Class, Function, CommandConfig)} instead <br>
 * @see Config
 * @see CommandHandler#registerCommand(Class, Function, CommandConfig)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
}

package com.github.afarion1.command_handler.example;

import com.github.afarion1.command_handler.annotations.Command;
import com.github.afarion1.command_handler.annotations.Config;
import com.github.afarion1.command_handler.command.AbstractCommand;
import com.github.afarion1.command_handler.command.CommandArguments;
import com.github.afarion1.command_handler.command.CommandHandler;
import com.github.afarion1.command_handler.command.config.CommandArgumentConfigBuilder;
import com.github.afarion1.command_handler.command.config.CommandConfig;
import com.github.afarion1.command_handler.command.config.CommandConfigBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Duration;


@Command
public final class CmdCalculator extends AbstractCommand {

    private static final int ID_FIRST_NUMBER = 0;
    private static final int ID_OPERATION_SIGN = 1;
    private static final int ID_SECOND_NUMBER = 2;

    @Config
    private static CommandConfig config = new CommandConfigBuilder("calculate")
            .addAliases("calculator", "calc")
            .setDescription("Performs basic operations on 2 numbers")
            .setVerboseDescription("Performs basic operations on 2 numbers.\nSupported operations: + - * / ^")
            .setCooldown(Duration.ofSeconds(5))
            .addArguments(new CommandArgumentConfigBuilder(ID_FIRST_NUMBER, "first number")
                            .setArgumentDescription("Simple decimal number")
                            .setParseToDouble(true),
                        new CommandArgumentConfigBuilder(ID_OPERATION_SIGN, "operation sign")
                            .setArgumentDescription("Supported operations: + - * / ^")
                            .addStringValidators(s -> s.matches("[+|\\-|*|/|^]")),
                        new CommandArgumentConfigBuilder(ID_SECOND_NUMBER, "second number")
                            .setArgumentDescription("Simple decimal number")
                            .setParseToDouble(true))
            .build();


    public CmdCalculator(CommandHandler handler) {
        super(handler);
    }

    @Override
    public void execute(MessageReceivedEvent event, CommandArguments args) {
        double firstNumber = args.getDoubleArgumentValue(ID_FIRST_NUMBER);
        double secondNumber = args.getDoubleArgumentValue(ID_SECOND_NUMBER);
        String operation = args.getStringArgumentValue(ID_OPERATION_SIGN);
        double result;

        switch (operation){
            case "+":
                result = firstNumber + secondNumber;
                break;
            case "-":
                result = firstNumber - secondNumber;
                break;
            case "*":
                result = firstNumber * secondNumber;
                break;
            case "/":
                result = firstNumber / secondNumber;
                break;
            case "^":
                result = Math.pow(firstNumber,secondNumber);
                break;
            default:
                event.getChannel().sendMessage("Something went wrong while executing the" +
                        " command, seems like regex is wrong").queue();
                return;
        }

        event.getChannel().sendMessage(suppressZeroes(firstNumber) + " " + operation + " " +
                suppressZeroes(secondNumber) + " = " + suppressZeroes(result)).queue();
    }

    private String suppressZeroes(double result) {
        return result % 1 == 0 ? Integer.toString((int)result) : Double.toString(result);
    }
}

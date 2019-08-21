package com.github.afarion1.command_handler.command.config;

import java.util.List;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;

/**
 * Represents configuration of argument
 */
public final class CommandArgumentConfig {

    private final int id;
    private final String argumentName;
    private final List<String> argumentOptions;
    private final List<Predicate<String>> stringValidators;
    private final List<DoublePredicate> doubleValidators;
    private final boolean optional;
    private final String argumentDesc;
    private final boolean parseToDouble;
    private final String defaultStringValue;
    private final double defaultDoubleValue;
    private final boolean inQuotes;
    private final String errorMsg;
    private final boolean multiWordChoosingEnabled;

    CommandArgumentConfig(int id, String argumentName, List<String> argumentOptions, List<Predicate<String>> stringValidators, List<DoublePredicate> doubleValidators, boolean optional, String argumentDesc, boolean parseToDouble, String defaultStringValue, double defaultDoubleValue, boolean inQuotes, String errorMsg, boolean multiWordChoosingEnabled) {
        this.id = id;
        this.argumentName = argumentName;
        this.argumentOptions = argumentOptions;
        this.stringValidators = stringValidators;
        this.doubleValidators = doubleValidators;
        this.optional = optional;
        this.argumentDesc = argumentDesc;
        this.parseToDouble = parseToDouble;
        this.defaultStringValue = defaultStringValue;
        this.defaultDoubleValue = defaultDoubleValue;
        this.inQuotes = inQuotes;
        this.errorMsg = errorMsg;
        this.multiWordChoosingEnabled = multiWordChoosingEnabled;
    }

    public int getId() {
        return id;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public List<String> getArgumentOptions() {
        return argumentOptions;
    }

    public List<Predicate<String>> getStringValidators() {
        return stringValidators;
    }

    public List<DoublePredicate> getDoubleValidators() {
        return doubleValidators;
    }

    public boolean isOptional() {
        return optional;
    }

    public String getArgumentDesc() {
        return argumentDesc;
    }

    public boolean isParseToDouble() {
        return parseToDouble;
    }

    public String getDefaultStringValue() {
        return defaultStringValue;
    }

    public double getDefaultDoubleValue() {
        return defaultDoubleValue;
    }

    public boolean isInQuotes() {
        return inQuotes;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public boolean isMultiWordChoosingEnabled() {
        return multiWordChoosingEnabled;
    }
}

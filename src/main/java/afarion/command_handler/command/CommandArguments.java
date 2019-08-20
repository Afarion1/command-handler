package afarion.command_handler.command;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;

public final class CommandArguments {

    private final String rawArgs;
    private IntList wrongArgsIds = null;
    private Int2ObjectMap<String> strArgValues = null;
    private Int2DoubleMap doubleArgValues = null;
    private boolean rawOnly = false;

    CommandArguments(IntList wrongArgsIds, Int2ObjectMap<String> strArgValues, Int2DoubleMap doubleArgValues, String rawArgs) {
        this.wrongArgsIds = wrongArgsIds;
        this.strArgValues = strArgValues;
        this.doubleArgValues = doubleArgValues;
        this.rawArgs = rawArgs;
    }

    CommandArguments(String rawArgs) {
        this.rawArgs = rawArgs;
        this.rawOnly = true;
    }

    private void validateNotRawOnly() {
        if (rawOnly)
            throw new IllegalStateException("The command is declared as raw args only");
    }

    public String getStringArgumentValue(int argId) {
        validateNotRawOnly();
        return strArgValues.get(argId);
    }

    public double getDoubleArgumentValue(int argId) {
        validateNotRawOnly();
        return doubleArgValues.get(argId);
    }

    public boolean isArgumentPresent(int argId) {
        validateNotRawOnly();
        return strArgValues.get(argId) != null;
    }

    boolean areValid() {
        if (wrongArgsIds == null) return true;
        return wrongArgsIds.isEmpty();
    }

    IntList getWrongArgsIds() {
        validateNotRawOnly();
        return wrongArgsIds;
    }

    public String getRawArgs() {
        return rawArgs;
    }
}

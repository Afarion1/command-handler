package afarion.command_handler.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {

    private final String name;
    private int i = 1;

    public NamedThreadFactory(String threadName) {
        this.name = threadName;
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        return new Thread(r, name + " " + i++);
    }
}

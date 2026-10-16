package dev.lvstrng.aidsfuscator.cli;

import dev.lvstrng.aidsfuscator.context.Context;

public abstract class ArgumentHandler {
    private final String prefix;
    protected Context context;

    public ArgumentHandler(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    public abstract void run(String value);

    public ArgumentHandler provideContext(Context context) {
        this.context = context;
        return this;
    }
}

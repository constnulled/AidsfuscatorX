package dev.lvstrng.aidsfuscator.cli.impl;

import dev.lvstrng.aidsfuscator.cli.ArgumentHandler;

public class JavaPathArgumentHandler extends ArgumentHandler {
    public JavaPathArgumentHandler() {
        super("--javaPath=");
    }

    @Override
    public void run(String value) {
        context.javaPath(value);
    }
}

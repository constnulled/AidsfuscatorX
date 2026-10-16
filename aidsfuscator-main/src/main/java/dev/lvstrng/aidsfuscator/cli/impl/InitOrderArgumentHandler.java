package dev.lvstrng.aidsfuscator.cli.impl;

import dev.lvstrng.aidsfuscator.cli.ArgumentHandler;

public class InitOrderArgumentHandler extends ArgumentHandler {
    public InitOrderArgumentHandler() {
        super("--initOrder=");
    }

    @Override
    public void run(String value) {
        context.initOrderLoader().setPath(value);
    }
}

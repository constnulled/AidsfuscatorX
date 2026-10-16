package dev.lvstrng.aidsfuscator.cli.impl;

import dev.lvstrng.aidsfuscator.cli.ArgumentHandler;
import dev.lvstrng.aidsfuscator.file.impl.ConfigLoader;

public class ConfigArgumentHandler extends ArgumentHandler {
    public ConfigArgumentHandler() {
        super("--config=");
    }

    @Override
    public void run(String value) {
        new ConfigLoader(context, value).load();
    }
}

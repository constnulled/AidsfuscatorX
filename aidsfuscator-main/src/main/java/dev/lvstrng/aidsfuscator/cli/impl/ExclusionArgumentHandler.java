package dev.lvstrng.aidsfuscator.cli.impl;

import dev.lvstrng.aidsfuscator.cli.ArgumentHandler;
import dev.lvstrng.aidsfuscator.file.impl.exclusions.ExclusionLoader;

public class ExclusionArgumentHandler extends ArgumentHandler {
    public ExclusionArgumentHandler() {
        super("--exclusions=");
    }

    @Override
    public void run(String value) {
        new ExclusionLoader(value).load();
    }
}

package dev.lvstrng.aidsfuscator.cli.impl;

import dev.lvstrng.aidsfuscator.cli.ArgumentHandler;
import dev.lvstrng.aidsfuscator.file.impl.references.ReferenceLoader;

public class ReferenceArgumentHandler extends ArgumentHandler {
    public ReferenceArgumentHandler() {
        super("--references=");
    }

    @Override
    public void run(String value) {
        new ReferenceLoader(context, value).load();
    }
}

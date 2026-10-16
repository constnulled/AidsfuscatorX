package dev.lvstrng.aidsfuscator.context.exception;

public class MissingWorkspaceItemException extends RuntimeException {
    public MissingWorkspaceItemException(String item) {
        super("Missing `" + item + "` from `workspace/` path. Please make sure this file exists in the workspace folder.");
    }
}

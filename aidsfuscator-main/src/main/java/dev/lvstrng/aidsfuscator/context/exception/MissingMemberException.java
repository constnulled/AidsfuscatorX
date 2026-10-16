package dev.lvstrng.aidsfuscator.context.exception;

public class MissingMemberException extends RuntimeException {
    public MissingMemberException(String name) {
        super("Missing member `%s`. Please install the correct libraries.".formatted(name));
    }
}

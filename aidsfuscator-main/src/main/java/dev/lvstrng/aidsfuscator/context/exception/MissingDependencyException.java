package dev.lvstrng.aidsfuscator.context.exception;

public class MissingDependencyException extends RuntimeException {
    public MissingDependencyException(int missingCount) {
        super("Hierarchy resolution failed: %d classes could not be resolved and `strictDependencies` is enabled. Add the missing jars to your libs folder, or disable `strictDependencies` to proceed anyway.".formatted(missingCount));
    }
}

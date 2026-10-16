package dev.lvstrng.aidsfuscator.naming;

// SomeClass.someMethod()V -> a
public record Mapping(String key, String value) {
    @Override
    public String toString() {
        return key;
    }
}

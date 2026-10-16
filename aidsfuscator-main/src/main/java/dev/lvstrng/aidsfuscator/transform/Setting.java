package dev.lvstrng.aidsfuscator.transform;

public class Setting<T> {
    private final String key;
    private T value;

    private Setting(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public T value() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public String key() {
        return key;
    }

    public static Setting<Integer> ofInt(String name, int value) {
        return new Setting<>(name, value);
    }

    public static Setting<String> ofString(String name, String value) {
        return new Setting<>(name, value);
    }

    public static Setting<Boolean> ofBoolean(String name, boolean value) {
        return new Setting<>(name, value);
    }
}

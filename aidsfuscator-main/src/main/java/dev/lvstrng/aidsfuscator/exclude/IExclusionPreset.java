package dev.lvstrng.aidsfuscator.exclude;

public interface IExclusionPreset {
    void load();

    default String internal(Class<?> c) {
        return c.getName().replace('.', '/');
    }
}

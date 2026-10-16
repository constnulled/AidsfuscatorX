package dev.lvstrng.aidsfuscator.property;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * The global property container keeps track of object's properties if they don't have a wrapper class like {@link JClass} or {@link JMethod}.
 * This can be done to basically any object. Mostly used for giving AbstractInsnNodes properties to efficiently obfuscate the program.
 * @author lvstrng
 */
public class GlobalPropertyContainer {
    private final Map<Object, PropertyContainer> properties;

    public GlobalPropertyContainer() {
        this.properties = new HashMap<>();
    }

    public <T> T add(T obj, Property... properties) {
        this.properties.computeIfAbsent(obj, _ -> new PropertyContainer()).add(properties);
        return obj;
    }

    public PropertyContainer get(Object obj) {
        // default to an empty container, because then that object has no properties (avoid null checks)
        return properties.getOrDefault(obj, new PropertyContainer());
    }

    public Map<Object, PropertyContainer> properties() {
        return properties;
    }
}

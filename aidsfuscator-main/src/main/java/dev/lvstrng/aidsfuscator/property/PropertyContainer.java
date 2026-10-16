package dev.lvstrng.aidsfuscator.property;

import java.util.ArrayList;
import java.util.List;

/**
 * A property container used to store a bunch of useful information about objects in the obfuscatable jar.
 * @author lvstrng
 */
public class PropertyContainer {
    private final List<Property> properties;

    public PropertyContainer() {
        this.properties = new ArrayList<>();
    }

    public void add(Property... properties) {
        this.properties.addAll(List.of(properties));
    }

    public boolean has(Property... properties) {
        if(properties.length == 1)
            return this.properties.contains(properties[0]);

        for(var property : properties)
            if(this.properties.contains(property))
                return true;

        return false;
    }

    public List<Property> properties() {
        return properties;
    }
}

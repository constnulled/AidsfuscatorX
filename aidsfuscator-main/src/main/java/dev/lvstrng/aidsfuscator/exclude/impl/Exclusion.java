package dev.lvstrng.aidsfuscator.exclude.impl;

import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

public class Exclusion {
    private final StringFilter filter;
    private final boolean inclusion;

    public Exclusion(String pattern) {
        if(pattern.startsWith("!")) {
            inclusion = true;
            pattern = pattern.substring(1);
        } else {
            inclusion = false;
        }

        this.filter = new StringFilter(pattern);
    }

    public boolean test(String str) {
        return inclusion != filter.test(str);
    }

    public boolean matchesAnnotation(String ann) {
        if(Mappings.CLASS.containsNew(ann))
            return test(Mappings.CLASS.retrieveOld(ann));

        return test(ann);
    }

    public boolean matchesClass(JClass clazz) {
        return test(clazz.originalName());
    }

    public boolean matchesMethod(JMethod method) {
        return matchesMethod(method.owner(), method);
    }

    public boolean matchesField(JField field) {
        return matchesField(field.owner(), field);
    }

    public boolean matchesMethod(JClass clazz, JMethod method) {
        return test(clazz.originalName() + "." + method.simpleOriginalName());
    }

    public boolean matchesField(JClass clazz, JField field) {
        return test(clazz.originalName() + "." + field.simpleOriginalName());
    }

    @Override
    public int hashCode() {
        return filter.string().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Exclusion e))
            return false;

        return e.filter.string().equals(filter.string());
    }

    @Override
    public String toString() {
        return (inclusion ? "!" : "") + filter.string();
    }
}
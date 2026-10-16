package dev.lvstrng.aidsfuscator.context.hierarchy;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

import java.util.List;

public interface IHierarchy {
    String object = "java/lang/Object";

    void build();

    void build(JClass clazz);
    void build(JMethod method);
    void build(JField field);

    void buildIfEmpty(JClass clazz);
    void buildIfEmpty(JMethod method);
    void buildIfEmpty(JField field);

    String commonSuperClass(String type1, String type2);

    default void trace(Context context, JClass clazz, List<String> used) {
        if (clazz == null)
            return;

        if (used.contains(clazz.name()))
            return;

        used.add(clazz.name());
        if (clazz.superName() != null && !clazz.isInterface())
            trace(context, context.findByName(clazz.superName()), used);

        for (var itf : clazz.interfaces()) {
            var itfClass = context.findByName(itf);
            trace(context, itfClass, used);
        }
    }
}

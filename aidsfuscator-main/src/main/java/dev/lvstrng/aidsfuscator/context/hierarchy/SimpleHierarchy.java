package dev.lvstrng.aidsfuscator.context.hierarchy;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class SimpleHierarchy implements IHierarchy {
    private final Context context;
    private final Predicate<JMethod> blacklistedMethod = (JMethod::isNonHierarchical);
    private final Predicate<JField>  blacklistedField  = (JField::isNonHierarchical);

    public SimpleHierarchy(Context context) {
        this.context = context;
    }

    @Override
    public void build() {
        Logger.info("Building hierarchy...");

        var classes = context.jarClasses();
        this.clearHierarchy(classes);

        classes.forEach(this::build);
        classes.forEach(clazz -> {
            clazz.methods().forEach(this::build);
            clazz.fields().forEach(this::build);
        });
    }

    @Override
    public void build(JClass clazz) {
        var parents = getTree(clazz).stream()
                .map(context::findByName)
                .filter(e -> e != null && e != clazz)
                .toList();

        clazz.parents().addAll(parents);
        for (var parent : parents) {
            parent.children().add(clazz);
        }
    }

    @Override
    public void build(JMethod method) {
        var owner = method.owner();
        if (blacklistedMethod.test(method))
            return;

        for (var parent : owner.parents()) {
            var opt = parent.findMethod(method.name(), method.desc());
            if (opt.isEmpty())
                continue;

            var parentMethod = opt.get();
            if (parentMethod.isVirtual() != method.isVirtual())
                continue;

            if (blacklistedMethod.test(parentMethod))
                continue;

            method.parents().add(parentMethod);
            parentMethod.children().add(method);
        }

        for (var child : owner.children()) {
            var opt = child.findMethod(method.name(), method.desc());
            if (opt.isEmpty())
                continue;

            var childMethod = opt.get();
            if (childMethod.isVirtual() != method.isVirtual())
                continue;

            if (blacklistedMethod.test(childMethod))
                continue;

            method.children().add(childMethod);
            childMethod.parents().add(method);
        }
    }

    @Override
    public void build(JField field) {
        var owner = field.owner();
        if (blacklistedField.test(field))
            return;

        for (var parent : owner.parents()) {
            var opt = parent.findField(field.name(), field.desc());
            if (opt.isEmpty())
                continue;

            var parentField = opt.get();
            if (parentField.isVirtual() != field.isVirtual())
                continue;

            if (blacklistedField.test(parentField))
                continue;

            field.parents().add(parentField);
            parentField.children().add(field);
        }

        for (var child : owner.children()) {
            var opt = child.findField(field.name(), field.desc());
            if (opt.isEmpty())
                continue;

            var childField = opt.get();
            if (childField.isVirtual() != field.isVirtual())
                continue;

            if (blacklistedField.test(childField))
                continue;

            field.children().add(childField);
            childField.parents().add(field);
        }
    }

    @Override
    public void buildIfEmpty(JClass clazz) {
        if (!clazz.tree().isEmpty())
            return;

        build(clazz);
    }

    @Override
    public void buildIfEmpty(JMethod method) {
        if (!method.tree().isEmpty())
            return;

        build(method);
    }

    @Override
    public void buildIfEmpty(JField field) {
        if (!field.tree().isEmpty())
            return;

        build(field);
    }

    @Override
    public String commonSuperClass(String type1, String type2) {
        if (type1.startsWith("[") || type2.startsWith("[")) {
            if (!type1.startsWith("[") || !type2.startsWith("["))
                return object;

            return commonArray(type1, type2);
        }

        var node  = context.findByName(type1);
        var other = context.findByName(type2);
        if (node == null || other == null)
            return object;

        if (node.isInterface() || other.isInterface())
            return object;

        if (node.name().equals(other.name()))
            return node.name();

        if (node.parents().isEmpty())
            build(node);
        if (other.parents().isEmpty())
            build(other);

        var nodeClasses  = new LinkedHashSet<JClass>();
        var otherClasses = new LinkedHashSet<JClass>();
        getSuperClasses(node, nodeClasses);
        getSuperClasses(other, otherClasses);

        if (otherClasses.contains(node))
            return node.name();
        if (nodeClasses.contains(other))
            return other.name();

        for (var clazz : nodeClasses) {
            if (otherClasses.contains(clazz))
                return clazz.name();
        }

        return object;
    }

    private String objectArray(int dims) {
        if (dims <= 0)
            return object;
        return "[".repeat(dims) + "Ljava/lang/Object;";
    }

    private String commonArray(String type1, String type2) {
        var t1 = Type.getType(type1);
        var t2 = Type.getType(type2);
        if (t1.getDimensions() != t2.getDimensions())
            return objectArray(Math.min(t1.getDimensions(), t2.getDimensions()) - 1);

        var e1 = t1.getElementType();
        var e2 = t2.getElementType();

        if (e1.getSort() != e2.getSort())
            return objectArray(Math.min(t1.getDimensions(), t2.getDimensions()) - 1);

        if (e1.getSort() == Type.OBJECT) {
            var common = commonSuperClass(e1.getInternalName(), e2.getInternalName());
            return "[".repeat(t1.getDimensions()) + "L" + common + ";";
        }

        return "[".repeat(t1.getDimensions()) + e2.getDescriptor();
    }

    private void getSuperClasses(JClass clazz, Set<JClass> classes) {
        if (!classes.add(clazz))
            return;

        if (clazz.superName() == null)
            return;

        var superClazz = context.findByName(clazz.superName());
        if (superClazz == null)
            return;

        getSuperClasses(superClazz, classes);
    }

    private List<String> getTree(JClass clazz) {
        var tree = new ArrayList<String>();
        trace(context, clazz, tree);
        return tree;
    }

    private void clearHierarchy(List<JClass> classes) {
        classes.forEach(clazz -> {
            clazz.clear();

            clazz.methods().forEach(JMethod::clear);
            clazz.fields().forEach(JField::clear);
        });
    }
}

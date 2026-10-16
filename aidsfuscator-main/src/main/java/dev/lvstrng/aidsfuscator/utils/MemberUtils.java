package dev.lvstrng.aidsfuscator.utils;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

public class MemberUtils {
    public static String fullMethod(String owner, String name, String desc) {
        return String.format("%s.%s%s", owner, name, desc);
    }

    public static String fullMethod(MethodInsnNode method) {
        return fullMethod(method.owner, method.name, method.desc);
    }

    public static String fullMethod(JClass clazz, JMethod method) {
        return fullMethod(clazz.name(), method.name(), method.desc());
    }

    public static String methodDesc(String name, String desc) {
        return name + desc;
    }

    public static String methodDesc(JMethod method) {
        return methodDesc(method.name(), method.desc());
    }

    public static String fullField(String owner, String name, String desc) {
        return String.format("%s.%s %s", owner, name, desc);
    }

    public static String fullField(FieldInsnNode field) {
        return fullField(field.owner, field.name, field.desc);
    }

    public static String fullField(JClass owner, JField field) {
        return fullField(owner.name(), field.name(), field.desc());
    }

    public static String fieldDesc(String name, String desc) {
        return String.format("%s %s", name, desc);
    }

    public static String fieldDesc(FieldInsnNode field) {
        return fieldDesc(field.name, field.desc);
    }

    public static String fieldDesc(FieldNode field) {
        return fieldDesc(field.name, field.desc);
    }

    public static boolean hasAnnotation(List<AnnotationNode> annotations, String annotation) {
        if (annotations == null)
            return false;

        for (var ann : annotations) {
            if (ann.desc.equals(annotation) || ann.desc.equals("L" + annotation.replace('.', '/') + ";"))
                return true;
        }

        return false;
    }
}

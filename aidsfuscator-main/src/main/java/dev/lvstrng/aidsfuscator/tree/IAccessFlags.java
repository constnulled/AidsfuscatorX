package dev.lvstrng.aidsfuscator.tree;

import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public interface IAccessFlags {
    int access();

    void setAccess(int flags);

    default void addAccessFlags(int flags) {
        setAccess(access() | flags);
    }

    default void removeAccessFlags(int flags) {
        setAccess(access() & ~(flags));
    }

    default boolean isPublic() {
        return Modifier.isPublic(access());
    }

    default boolean isProtected() {
        return Modifier.isProtected(access());
    }

    default boolean isPrivate() {
        return Modifier.isPrivate(access());
    }

    default boolean isStatic() {
        return Modifier.isStatic(access());
    }

    default boolean isVirtual() {
        return !isStatic();
    }

    default boolean isFinal() {
        return Modifier.isFinal(access());
    }

    default boolean isNative() {
        return Modifier.isNative(access());
    }

    default boolean isAbstract() {
        return Modifier.isAbstract(access());
    }

    default boolean isSynchronized() {
        return Modifier.isSynchronized(access());
    }

    default boolean isVolatile() {
        return Modifier.isVolatile(access());
    }

    default boolean isTransient() {
        return Modifier.isTransient(access());
    }

    default boolean isStrict() {
        return Modifier.isStrict(access());
    }

    default boolean isInterface() {
        return Modifier.isInterface(access());
    }

    default boolean isAnnotation() {
        return isAccess(Opcodes.ACC_ANNOTATION);
    }

    default boolean isRecord() {
        return isAccess(Opcodes.ACC_STRICT);
    }

    default boolean isSynthetic() {
        return isAccess(Opcodes.ACC_SYNTHETIC);
    }

    default boolean isBridge() {
        return isAccess(Opcodes.ACC_BRIDGE);
    }

    default boolean isSuper() {
        return isAccess(Opcodes.ACC_SUPER);
    }

    default boolean isVarargs() {
        return isAccess(Opcodes.ACC_VARARGS);
    }

    default boolean isModule() {
        return isAccess(Opcodes.ACC_MODULE);
    }

    default boolean isAccess(int acc) {
        return (access() & acc) != 0;
    }
}

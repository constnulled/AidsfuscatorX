package dev.lvstrng.aidsfuscator.analysis.interpreter;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

public class SimpleValue implements Value {
    public static final SimpleValue UNINITIALIZED_VALUE = new SimpleValue(null);
    public static final SimpleValue INT_VALUE = new SimpleValue(Type.INT_TYPE);
    public static final SimpleValue FLOAT_VALUE = new SimpleValue(Type.FLOAT_TYPE);
    public static final SimpleValue LONG_VALUE = new SimpleValue(Type.LONG_TYPE);
    public static final SimpleValue DOUBLE_VALUE = new SimpleValue(Type.DOUBLE_TYPE);
    public static final SimpleValue RETURNADDRESS_VALUE = new SimpleValue(Type.VOID_TYPE);

    private final Type type;
    private final AbstractInsnNode uninitializedInsn;
    private boolean isThis, initializedThis;

    public SimpleValue(Type type) {
        this.type = type;
        this.uninitializedInsn = null;
    }

    public SimpleValue(Type type, AbstractInsnNode uninitializedInsn) {
        this.type = type;
        this.uninitializedInsn = uninitializedInsn;
    }

    public SimpleValue setThis() {
        this.isThis = true;
        return this;
    }

    public SimpleValue setInitializedThis() {
        this.initializedThis = true;
        return this;
    }

    public boolean isThis() {
        return isThis;
    }

    public boolean isInitializedThis() {
        return initializedThis;
    }

    @Override
    public int getSize() {
        return type == null ? 1 : type.getSize();
    }

    public Type type() {
        return type;
    }

    public boolean isUninitialized() {
        return type == null;
    }

    public boolean isReference() {
        return isObject() || isArray();
    }

    public boolean isObject() {
        return type != null && type.getSort() == Type.OBJECT;
    }

    public boolean isArray() {
        return type != null && type.getSort() == Type.ARRAY;
    }

    public static SimpleValue of(Type t) {
        return new SimpleValue(t);
    }

    public static SimpleValue reference(String type) {
        return new SimpleValue(Type.getObjectType(type));
    }

    public static SimpleValue reference(Type type) {
        return new SimpleValue(type);
    }

    @Override
    public boolean equals(Object value) {
        if (value == this) {
            return true;
        } else if (value instanceof SimpleValue other) {
            if (uninitializedInsn != other.uninitializedInsn) {
                return false;
            }
            if (type == null) {
                return other.type == null;
            } else {
                return type.equals(other.type);
            }
        }

        return false;
    }

    @Override
    public String toString() {
        if(type == null)
            return "top";

        if (uninitializedInsn != null)
            return type.getDescriptor() + "@" + System.identityHashCode(uninitializedInsn);

        return type.getDescriptor();
    }
}
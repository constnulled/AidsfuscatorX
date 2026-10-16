package dev.lvstrng.aidsfuscator.salt.impl;

import dev.lvstrng.aidsfuscator.salt.ISalt;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

public class ClassSalt implements ISalt {
    private final JClass clazz;
    private final JField field;
    private final int value;

    public ClassSalt(JClass clazz, JField field, int value) {
        this.clazz = clazz;
        this.field = field;
        this.value = value;
    }

    @Override
    public int value() {
        return value;
    }

    @Override
    public AbstractInsnNode load() {
        return new FieldInsnNode(Opcodes.GETSTATIC, clazz.name(), field.name(), "I");
    }

    @Override
    public AbstractInsnNode store() {
        return new FieldInsnNode(Opcodes.PUTSTATIC, clazz.name(), field.name(), "I");
    }
}

package dev.lvstrng.aidsfuscator.salt.impl;

import dev.lvstrng.aidsfuscator.salt.ISalt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MethodSalt implements ISalt {
    private int value;
    private int local;

    public MethodSalt(int value, int local) {
        this.value = value;
        this.local = local;
    }

    public int value() {
        return value;
    }

    public int local() {
        return local;
    }

    public void updateValue(int value) {
        this.value = value;
    }

    public void updateVar(int local) {
        this.local = local;
    }

    @Override
    public AbstractInsnNode load() {
        return new VarInsnNode(Opcodes.ILOAD, local);
    }

    @Override
    public AbstractInsnNode store() {
        return new VarInsnNode(Opcodes.ISTORE, local);
    }
}

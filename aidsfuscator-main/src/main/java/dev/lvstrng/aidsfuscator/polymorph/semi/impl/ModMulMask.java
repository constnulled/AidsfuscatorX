package dev.lvstrng.aidsfuscator.polymorph.semi.impl;

import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.security.SecureRandom;

public class ModMulMask extends IntMask<ModMulMask> {
    private static final SecureRandom secureRandom = new SecureRandom();

    private final int inverse;

    public ModMulMask() {
        int key = secureRandom.nextInt() | 1;
        this.value = key;
        this.inverse = modInverse32(key);
    }

    public static int modInverse32(int a) {
        int x = 1;
        for (int i = 0; i < 5; i++) {
            x = x * (2 - a * x);
        }
        return x;
    }

    @Override
    public int apply(int num) {
        return num * value;
    }

    @Override
    public int applyInverse(int num) {
        return num * inverse;
    }

    @Override
    public InsnList insns() {
        var list = new InsnList();
        list.add(ASMUtils.pushInt(value));
        list.add(new InsnNode(Opcodes.IMUL));
        return list;
    }

    @Override
    public ModMulMask ofValue(int value) {
        throw new UnsupportedOperationException(
                "ModMulMask generates its own key to guarantee invertibility. Use the default constructor.");
    }

    @Override
    public ModMulMask ofRandomValue(int min, int max) {
        throw new UnsupportedOperationException(
                "ModMulMask generates its own key to guarantee invertibility. Use the default constructor.");
    }

    @Override
    public ModMulMask ofRandomValue(int max) {
        throw new UnsupportedOperationException(
                "ModMulMask generates its own key to guarantee invertibility. Use the default constructor.");
    }

    @Override
    public ModMulMask ofRandomValue() {
        return this;
    }
}
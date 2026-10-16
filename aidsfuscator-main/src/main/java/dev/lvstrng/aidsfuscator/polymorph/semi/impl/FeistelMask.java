package dev.lvstrng.aidsfuscator.polymorph.semi.impl;

import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.security.SecureRandom;

public class FeistelMask extends IntMask<FeistelMask> {
    private static final SecureRandom secureRandom = new SecureRandom();

    private final int key1;
    private final int key2;

    public FeistelMask() {
        this.key1 = secureRandom.nextInt() | 1;
        this.key2 = secureRandom.nextInt() | 1;
    }

    public FeistelMask(int key1, int key2) {
        this.key1 = key1 | 1;
        this.key2 = key2 | 1;
    }

    private static int roundFunction(int x, int k) {
        return ((x * k) >>> 5) ^ ((x + k) & 0xFFFF);
    }

    @Override
    public int apply(int num) {
        int left = (num >>> 16) & 0xFFFF;
        int right = num & 0xFFFF;

        left = left ^ roundFunction(right, key1);
        left &= 0xFFFF;

        right = right ^ roundFunction(left, key2);
        right &= 0xFFFF;

        return (left << 16) | right;
    }

    @Override
    public int applyInverse(int num) {
        int left = (num >>> 16) & 0xFFFF;
        int right = num & 0xFFFF;

        right = right ^ roundFunction(left, key2);
        right &= 0xFFFF;

        left = left ^ roundFunction(right, key1);
        left &= 0xFFFF;

        return (left << 16) | right;
    }

    @Override
    public InsnList insns() {
        var list = new InsnList();

        list.add(new InsnNode(Opcodes.DUP));
        list.add(ASMUtils.pushInt(16));
        list.add(new InsnNode(Opcodes.IUSHR));
        list.add(ASMUtils.pushInt(0xFFFF));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(ASMUtils.pushInt(0xFFFF));
        list.add(new InsnNode(Opcodes.IAND));

        list.add(new InsnNode(Opcodes.DUP));

        list.add(new InsnNode(Opcodes.DUP));
        list.add(ASMUtils.pushInt(key1));
        list.add(new InsnNode(Opcodes.IMUL));
        list.add(ASMUtils.pushInt(5));
        list.add(new InsnNode(Opcodes.IUSHR));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(ASMUtils.pushInt(key1));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(ASMUtils.pushInt(0xFFFF));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(new InsnNode(Opcodes.IXOR));

        list.add(new InsnNode(Opcodes.DUP_X2));
        list.add(new InsnNode(Opcodes.POP));
        list.add(new InsnNode(Opcodes.DUP_X2));
        list.add(new InsnNode(Opcodes.POP));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(ASMUtils.pushInt(0xFFFF));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(ASMUtils.pushInt(key2));
        list.add(new InsnNode(Opcodes.IMUL));
        list.add(ASMUtils.pushInt(5));
        list.add(new InsnNode(Opcodes.IUSHR));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(ASMUtils.pushInt(key2));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(ASMUtils.pushInt(0xFFFF));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(new InsnNode(Opcodes.DUP_X2));
        list.add(new InsnNode(Opcodes.POP));
        list.add(new InsnNode(Opcodes.DUP_X2));
        list.add(new InsnNode(Opcodes.POP));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(ASMUtils.pushInt(0xFFFF));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(ASMUtils.pushInt(16));
        list.add(new InsnNode(Opcodes.ISHL));
        list.add(new InsnNode(Opcodes.IOR));

        return list;
    }
}
package dev.lvstrng.aidsfuscator.polymorph.semi.impl;

import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.security.SecureRandom;

public class MBAMask extends IntMask<MBAMask> {
    private static final SecureRandom secureRandom = new SecureRandom();

    private final int variant;
    private final int extraKey;

    public MBAMask() {
        this.variant = secureRandom.nextInt(4);
        this.extraKey = secureRandom.nextInt();
    }

    @Override
    public int apply(int num) {
        return switch (variant) {
            case 0 -> mbaAdd0(num, value);
            case 1 -> mbaAdd1(num, value);
            case 2 -> mbaAdd2(num, value, extraKey);
            case 3 -> mbaAdd3(num, value, extraKey);
            default -> num + value;
        };
    }

    @Override
    public int applyInverse(int num) {
        return num - value;
    }

    private static int mbaAdd0(int x, int y) {
        return (x & y) + (x | y);
    }

    private static int mbaAdd1(int x, int y) {
        return 2 * (x & y) + (x ^ y);
    }

    private static int mbaAdd2(int x, int y, int k) {
        int nx = ~x;
        int ny = ~y;
        int sum = nx + ny + 1;
        return ~sum;
    }

    private static int mbaAdd3(int x, int y, int k) {
        return -(~x + ~y + 2);
    }

    @Override
    public InsnList insns() {
        return switch (variant) {
            case 0 -> emitVariant0();
            case 1 -> emitVariant1();
            case 2 -> emitVariant2();
            case 3 -> emitVariant3();
            default -> emitVariant0();
        };
    }

    private InsnList emitVariant0() {
        var list = new InsnList();
        list.add(new InsnNode(Opcodes.DUP));
        list.add(ASMUtils.pushInt(value));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(ASMUtils.pushInt(value));
        list.add(new InsnNode(Opcodes.IOR));
        list.add(new InsnNode(Opcodes.IADD));
        return list;
    }

    private InsnList emitVariant1() {
        var list = new InsnList();
        list.add(new InsnNode(Opcodes.DUP));
        list.add(ASMUtils.pushInt(value));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(ASMUtils.pushInt(2));
        list.add(new InsnNode(Opcodes.IMUL));
        list.add(new InsnNode(Opcodes.SWAP));
        list.add(ASMUtils.pushInt(value));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(new InsnNode(Opcodes.IADD));
        return list;
    }

    private InsnList emitVariant2() {
        var list = new InsnList();
        list.add(ASMUtils.pushInt(-1));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(ASMUtils.pushInt(~value));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(ASMUtils.pushInt(1));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(ASMUtils.pushInt(-1));
        list.add(new InsnNode(Opcodes.IXOR));
        return list;
    }

    private InsnList emitVariant3() {
        var list = new InsnList();
        list.add(ASMUtils.pushInt(-1));
        list.add(new InsnNode(Opcodes.IXOR));
        list.add(ASMUtils.pushInt(~value + 2));
        list.add(new InsnNode(Opcodes.IADD));
        list.add(new InsnNode(Opcodes.INEG));
        return list;
    }
}
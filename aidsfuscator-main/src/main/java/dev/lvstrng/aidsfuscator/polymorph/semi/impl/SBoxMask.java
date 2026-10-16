package dev.lvstrng.aidsfuscator.polymorph.semi.impl;

import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;

public class SBoxMask extends IntMask<SBoxMask> {
    private static final SecureRandom secureRandom = new SecureRandom();

    private final int[] sbox = new int[256];
    private final int[] inverseSbox = new int[256];

    private final int bytePosition;

    public SBoxMask() {
        this(secureRandom.nextInt(4));
    }

    public SBoxMask(int bytePosition) {
        this.bytePosition = bytePosition & 3;
        generatePermutation();
    }

    private void generatePermutation() {
        var indices = new ArrayList<Integer>(256);
        for (int i = 0; i < 256; i++) indices.add(i);
        Collections.shuffle(indices, secureRandom);

        for (int i = 0; i < 256; i++) {
            sbox[i] = indices.get(i);
            inverseSbox[indices.get(i)] = i;
        }
    }

    @Override
    public int apply(int num) {
        int shift = bytePosition * 8;
        int b = (num >>> shift) & 0xFF;
        int substituted = sbox[b];
        return (num & ~(0xFF << shift)) | (substituted << shift);
    }

    @Override
    public int applyInverse(int num) {
        int shift = bytePosition * 8;
        int b = (num >>> shift) & 0xFF;
        int original = inverseSbox[b];
        return (num & ~(0xFF << shift)) | (original << shift);
    }

    @Override
    public InsnList insns() {
        var list = new InsnList();
        int shift = bytePosition * 8;
        int clearMask = ~(0xFF << shift);

        list.add(new InsnNode(Opcodes.DUP));

        if (shift != 0) {
            list.add(ASMUtils.pushInt(shift));
            list.add(new InsnNode(Opcodes.IUSHR));
        }
        list.add(ASMUtils.pushInt(0xFF));
        list.add(new InsnNode(Opcodes.IAND));

        var dfltLabel = new LabelNode();
        var exitLabel = new LabelNode();
        int[] keys = new int[256];
        var labels = new LabelNode[256];

        for (int i = 0; i < 256; i++) {
            keys[i] = i;
            labels[i] = new LabelNode();
        }

        list.add(new LookupSwitchInsnNode(dfltLabel, keys, labels));

        for (int i = 0; i < 256; i++) {
            list.add(labels[i]);
            list.add(ASMUtils.pushInt(sbox[i]));
            list.add(new JumpInsnNode(Opcodes.GOTO, exitLabel));
        }

        list.add(dfltLabel);
        list.add(ASMUtils.pushInt(0));
        list.add(exitLabel);

        if (shift != 0) {
            list.add(ASMUtils.pushInt(shift));
            list.add(new InsnNode(Opcodes.ISHL));
        }

        list.add(new InsnNode(Opcodes.SWAP));
        list.add(ASMUtils.pushInt(clearMask));
        list.add(new InsnNode(Opcodes.IAND));
        list.add(new InsnNode(Opcodes.IOR));
        return list;
    }
}
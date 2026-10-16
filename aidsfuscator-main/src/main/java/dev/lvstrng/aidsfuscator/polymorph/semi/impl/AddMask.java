package dev.lvstrng.aidsfuscator.polymorph.semi.impl;

import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

public class AddMask extends IntMask<AddMask> {
    @Override
    public int apply(int num) {
        return num + value;
    }

    @Override
    public int applyInverse(int num) {
        return num - value;
    }

    @Override
    public InsnList insns() {
        return new InsnBuilder()._int(value).iadd().result();
    }
}

package dev.lvstrng.aidsfuscator.polymorph.semi.impl;

import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

public class MulMask extends IntMask<MulMask> {
    @Override
    public int apply(int num) {
        return num * value;
    }

    @Override
    public InsnList insns() {
        return new InsnBuilder()._int(value).imul().result();
    }
}

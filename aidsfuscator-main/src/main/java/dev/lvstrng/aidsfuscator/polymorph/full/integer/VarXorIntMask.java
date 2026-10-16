package dev.lvstrng.aidsfuscator.polymorph.full.integer;

import dev.lvstrng.aidsfuscator.polymorph.full.*;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

public class VarXorIntMask extends PolyMask<Integer> implements VariableIntValue {
    private final int index;
    private int value;

    public VarXorIntMask(PolymorphMethodContext context, int index) {
        super(context);
        this.index = index;
    }

    @Override
    public InsnList instructions() {
        return new InsnBuilder()._var(ILOAD, index).ixor().result();
    }

    @Override
    public Integer apply(Integer obj) {
        return obj ^ value;
    }

    @Override
    public Integer applyInverse(Integer obj) {
        return obj ^ value;
    }

    @Override
    public int variableSlot() {
        return index;
    }

    @Override
    public void provideValue(int value) {
        this.value = value;
    }
}

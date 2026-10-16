package dev.lvstrng.aidsfuscator.polymorph.full.integer;


import dev.lvstrng.aidsfuscator.polymorph.full.*;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

public class LoopXorIntMask extends PolyMask<Integer> implements LoopBasedMask<char[]> {
    private final int index;
    private int indexValue;

    public LoopXorIntMask(PolymorphMethodContext context) {
        super(context);
        this.index = context.indexVar();
    }

    @Override
    public InsnList instructions() {
        return new InsnBuilder()._var(ILOAD, index).ixor().result();
    }

    @Override
    public Integer apply(Integer obj) {
        return obj ^ indexValue;
    }

    @Override
    public Integer applyInverse(Integer obj) {
        return apply(obj);
    }

    @Override
    public char[] modify(char[] array) {
        for(int i = 0; i < array.length; i++) {
            this.indexValue = i;
            array[i] = (char) ((int) apply((int) array[i]));
        }

        return array;
    }

    @Override
    public char[] modifyInverse(char[] array) {
        return modify(array);
    }
}

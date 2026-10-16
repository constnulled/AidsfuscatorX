package dev.lvstrng.aidsfuscator.polymorph.full.integer;

import dev.lvstrng.aidsfuscator.polymorph.full.*;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

/**
 * Generates a switch based on the keys integer array like seen in default string decryptor.
 */
public class LoopBasedXorIntMask extends PolyMask<Integer> implements LoopBasedMask<char[]> {
    private final int[] keys;
    private int indexValue;

    public LoopBasedXorIntMask(PolymorphMethodContext context, int[] keys) {
        super(context);
        this.keys = keys;
    }

    @Override
    public InsnList instructions() {
        var builder = new InsnBuilder();
        var end = new LabelNode();
        var routeBuilder = new InsnBuilder();
        var lbls = new LabelNode[keys.length];

        for(int i = 0; i < keys.length; i++) {
            var route = new LabelNode();
            lbls[i] = route;

            routeBuilder
                    .label(route)
                    .add(ASMUtils.pushInt(keys[i]))
                    ._goto(end);
        }

        return builder
                ._var(ILOAD, context.indexVar())
                ._int(keys.length)
                .irem()
                .tableswitch(lbls[0], 0, keys.length - 1, lbls)
                .add(routeBuilder)
                .label(end)
                .ixor()
                .result();
    }

    @Override
    public Integer apply(Integer obj) {
        return obj ^ keys[indexValue % keys.length];
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

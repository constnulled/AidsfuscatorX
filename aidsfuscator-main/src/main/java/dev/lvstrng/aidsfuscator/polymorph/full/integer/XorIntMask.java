package dev.lvstrng.aidsfuscator.polymorph.full.integer;

import dev.lvstrng.aidsfuscator.polymorph.full.*;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;

public class XorIntMask extends PolyMask<Integer> {
    private final int value;

    public XorIntMask(PolymorphMethodContext context) {
        super(context);
        this.value = random.nextInt();
    }

    public XorIntMask(PolymorphMethodContext context, int max) {
        super(context);
        this.value = random.nextInt(max);
    }

    public XorIntMask(PolymorphMethodContext context, int min, int max) {
        super(context);
        this.value = random.nextInt(min, max);
    }

    @Override
    public InsnList instructions() {
        return new InsnBuilder()
                ._int(value)
                .ixor()
                .result();
    }

    @Override
    public Integer apply(Integer obj) {
        return obj ^ value;
    }

    @Override
    public Integer applyInverse(Integer obj) {
        return obj ^ value;
    }
}

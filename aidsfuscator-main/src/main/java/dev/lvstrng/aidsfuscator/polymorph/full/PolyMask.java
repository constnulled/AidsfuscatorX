package dev.lvstrng.aidsfuscator.polymorph.full;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;

import java.util.List;
import java.util.Random;

public abstract class PolyMask<T> implements Opcodes {
    protected static final Random random = new Random();
    protected final PolymorphMethodContext context;

    public PolyMask(PolymorphMethodContext context) {
        this.context = context;
    }

    public abstract InsnList instructions();

    public abstract T apply(T obj);
    public abstract T applyInverse(T obj);
}

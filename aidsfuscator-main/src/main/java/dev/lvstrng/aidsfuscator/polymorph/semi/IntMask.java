package dev.lvstrng.aidsfuscator.polymorph.semi;

import org.objectweb.asm.tree.InsnList;

import java.security.SecureRandom;

@SuppressWarnings("unchecked")
public abstract class IntMask<T extends IntMask<T>> {
    private static final SecureRandom random = new SecureRandom();
    protected int value;

    public abstract int apply(int num);
    public abstract InsnList insns();

    public int applyInverse(int num) {
        throw new RuntimeException("No Implementation for applyInverse in class (%s)".formatted(getClass()));
    }

    public T ofValue(int value) {
        this.value = value;
        return (T) this;
    }

    public T ofRandomValue(int min, int max) {
        this.value = random.nextInt(min, max);
        return (T) this;
    }

    public T ofRandomValue(int max) {
        this.value = random.nextInt(max);
        return (T) this;
    }

    public T ofRandomValue() {
        this.value = random.nextInt();
        return (T) this;
    }
}

package dev.lvstrng.aidsfuscator.polymorph.full;

/**
 * Used for masks that are used in loops and require the loop's index.
 */
public interface LoopBasedMask<T> {
    T modify(T array);

    T modifyInverse(T array);
}

package dev.lvstrng.aidsfuscator.salt;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleValue;
import org.objectweb.asm.tree.analysis.Frame;

public interface ISaltable<T extends ISalt> {
    boolean hasSalt();

    T salt();

    default boolean canSalt(Frame<SimpleValue> frame) {
        return true;
    }

    default boolean canSalt(Block block) {
        return true;
    }
}

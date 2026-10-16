package dev.lvstrng.aidsfuscator.salt;

import org.objectweb.asm.tree.AbstractInsnNode;

public interface ISalt {
    int value();

    AbstractInsnNode load();
    AbstractInsnNode store();
}

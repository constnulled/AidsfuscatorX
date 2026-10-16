package dev.lvstrng.aidsfuscator.analysis.ref.nodes;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public record ClassReference(JClass callerClass, JMethod caller, JClass clazz, AbstractInsnNode insn) {
    public boolean initializesClass() {
        return !(insn instanceof LdcInsnNode);
    }

    @Override
    public String toString() {
        return "{ " + caller + " -> " + clazz + " }";
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}

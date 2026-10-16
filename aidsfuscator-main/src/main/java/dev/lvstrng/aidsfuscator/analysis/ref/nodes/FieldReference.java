package dev.lvstrng.aidsfuscator.analysis.ref.nodes;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public record FieldReference(JClass callerClass, JMethod caller, JField field, AbstractInsnNode insn) {
    public boolean isGetter() {
        var op = insn.getOpcode();
        return op == Opcodes.GETFIELD || op == Opcodes.GETSTATIC;
    }

    public boolean isSetter() {
        var op = insn.getOpcode();
        return op == Opcodes.PUTFIELD || op == Opcodes.PUTSTATIC;
    }

    @Override
    public String toString() {
        return "{ " + caller + " -> " + field + " }";
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}

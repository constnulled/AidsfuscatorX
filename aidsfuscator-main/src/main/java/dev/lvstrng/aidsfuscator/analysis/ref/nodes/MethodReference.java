package dev.lvstrng.aidsfuscator.analysis.ref.nodes;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public record MethodReference(JClass callerClass, JMethod caller, JMethod method, AbstractInsnNode insn) {
    public boolean isDynamic() {
        var isCondy = insn instanceof LdcInsnNode ldc && ldc.cst instanceof ConstantDynamic;
        var isIndy = insn instanceof InvokeDynamicInsnNode;
        return isIndy || isCondy;
    }

    public boolean canEdit() {
        if(isDynamic())
            return false;

        return !method.isLibrary();
    }

    public boolean cantEdit() {
        return !canEdit();
    }

    @Override
    public String toString() {
        return "{ " + caller + " -> " + method + " }";
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}

package dev.lvstrng.aidsfuscator.analysis.ref.collector;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.tree.AbstractInsnNode;

public interface IReferenceCollector {
    void collect(Context context, ReferenceGraph graph, JClass callerClass, JMethod caller, AbstractInsnNode insn);

    boolean isOfType(AbstractInsnNode insn);
}

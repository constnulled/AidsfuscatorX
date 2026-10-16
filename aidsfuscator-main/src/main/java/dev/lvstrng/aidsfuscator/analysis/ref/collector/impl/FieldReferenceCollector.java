package dev.lvstrng.aidsfuscator.analysis.ref.collector.impl;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.analysis.ref.collector.IReferenceCollector;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

public class FieldReferenceCollector implements IReferenceCollector {
    @Override
    public void collect(Context context, ReferenceGraph graph, JClass callerClass, JMethod caller, AbstractInsnNode insn) {
        var field = (FieldInsnNode) insn;
        var node = graph.constructField(callerClass, caller, insn, field.owner, field.name, field.desc);
        if(node == null)
            return;

        graph.add(node);
    }

    @Override
    public boolean isOfType(AbstractInsnNode insn) {
        return insn instanceof FieldInsnNode;
    }
}

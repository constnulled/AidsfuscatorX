package dev.lvstrng.aidsfuscator.analysis.ref.collector.impl;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.analysis.ref.collector.IReferenceCollector;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.TypeUtils;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

@SuppressWarnings("all")
public class ClassReferenceCollector implements IReferenceCollector {
    @Override
    public void collect(Context context, ReferenceGraph graph, JClass callerClass, JMethod caller, AbstractInsnNode insn) {
        switch (insn) {
            case LdcInsnNode ldc when ldc.cst instanceof Type t -> {
                while (t.getSort() == Type.ARRAY) {
                    t = TypeUtils.getElementType(t);
                }

                var node = graph.constructClass(callerClass, caller, insn, t.getInternalName());
                if(node == null)
                    break;

                graph.add(node);
            }
            default -> throw new IllegalStateException("Unexpected type of instruction (Reference Graph): " + insn);
        }
    }

    @Override
    public boolean isOfType(AbstractInsnNode insn) {
        var isType = insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type;
        return isType;
    }
}

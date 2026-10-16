package dev.lvstrng.aidsfuscator.analysis.ref.collector.impl;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.analysis.ref.collector.IReferenceCollector;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class MethodReferenceCollector implements IReferenceCollector {
    @Override
    public void collect(Context context, ReferenceGraph graph, JClass callerClass, JMethod caller, AbstractInsnNode insn) {
        switch (insn) {
            case MethodInsnNode call -> {
                if(call.name.equals("clone") && call.desc.startsWith("()"))
                    break;

                var node = graph.construct(callerClass, caller, insn, call.owner, call.name, call.desc);
                if(node == null)
                    break;

                graph.add(node);
            }
            case InvokeDynamicInsnNode indy -> {
                graph.handleHandle(callerClass, caller, insn, indy.bsm);

                for(var arg : indy.bsmArgs) {
                    if(!(arg instanceof Handle h))
                        continue;

                    graph.handleHandle(callerClass, caller, insn, h);
                }
            }
            case LdcInsnNode ldc when ldc.cst instanceof Handle handle -> {
                graph.handleHandle(callerClass, caller, insn, handle);
            }
            case LdcInsnNode ldc when ldc.cst instanceof ConstantDynamic condy -> {
                graph.handleHandle(callerClass, caller, insn, condy.getBootstrapMethod());

                for(int i = 0; i < condy.getBootstrapMethodArgumentCount(); i++) {
                    var arg = condy.getBootstrapMethodArgument(i);
                    if(!(arg instanceof Handle h))
                        continue;

                    graph.handleHandle(callerClass, caller, insn, h);
                }
            }
            default -> throw new IllegalStateException("Unexpected type of instruction (Reference Graph): " + insn);
        }
    }

    @Override
    public boolean isOfType(AbstractInsnNode insn) {
        var isMethod = insn instanceof MethodInsnNode;
        var isIndy = insn instanceof InvokeDynamicInsnNode;
        var isCondy = insn instanceof LdcInsnNode ldc && (ldc.cst instanceof ConstantDynamic || ldc.cst instanceof Handle);
        return isMethod || isIndy || isCondy;
    }
}

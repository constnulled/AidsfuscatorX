package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize.IOptimizationPass;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;
import java.util.function.BiPredicate;

/**
 * Clears all unused variables in a JMethod. (Doesn't check for reassigning or variables).
 * TODO add pop and pop2 clearing.
 */
public class UnusedLocalVariableCleanTransformer implements IOptimizationPass {
    private static final BiPredicate<JMethod, AbstractInsnNode> badInsn = (method, insn) -> {
        var op = insn.getOpcode();
        /*if(op <= DUP2_X2 && op >= DUP)
            return true;*/

        if(insn instanceof MethodInsnNode || (insn instanceof FieldInsnNode field && !field.owner.equals(method.name())) || insn instanceof InvokeDynamicInsnNode)
            return true;

        return op == NEW || op == NEWARRAY || op == ANEWARRAY || op == MULTIANEWARRAY;
    };

    @Override
    public void optimize(Context context, JMethod method) {
        if(!method.traps().isEmpty())
            return;

        var loadsForSlot = new HashMap<Integer, Integer>();
        var storesForSlot = new HashMap<Integer, Integer>();

        // ---- FIND USAGES ----
        for(var insn : method.insns()) {
            if(ASMUtils.isVarLoad(insn)) {
                var loc = (VarInsnNode) insn;
                loadsForSlot.compute(loc.var, (_, v) -> (v == null) ? 1 : v + 1);
            } else if(ASMUtils.isVarStore(insn)) {
                var loc = (VarInsnNode) insn;
                storesForSlot.compute(loc.var, (_, v) -> (v == null) ? 1 : v + 1);
            } else if(insn instanceof IincInsnNode iinc) {
                storesForSlot.compute(iinc.var, (_, v) -> (v == null) ? 1 : v + 1);
            }
        }

        var removed = new HashSet<Integer>();
        for(var slot : storesForSlot.keySet()) {
            var loadCount = loadsForSlot.get(slot);
            if(loadCount != null && loadCount != 0)
                continue;

            var frames = analyzeSource(method);
            if(frames == null)
                break; // if analyzer failed then it won't unfail itself, break out of the shitty loop

            Arrays.stream(method.insns().toArray()).filter(e -> e instanceof VarInsnNode varInsn && varInsn.var == slot && ASMUtils.isVarStore(varInsn)).forEach(insn -> {
                var frame = frames.get(insn);
                if(frame == null)
                    return;

                var value = frame.getStack(frame.getStackSize() - 1);
                if(value.insns.stream().anyMatch(e -> badInsn.test(method, e)))
                    return;

                var toRemove = new HashSet<AbstractInsnNode>();
                collect(insn, toRemove, frames);
                toRemove.forEach(method.insns()::remove);
            });
            removed.add(slot);
        }
    }

    private void collect(AbstractInsnNode insn, Set<AbstractInsnNode> out, Map<AbstractInsnNode, Frame<SourceValue>> frames) {
        var frame = frames.get(insn);
        if(frame == null)
            return;

        if(!out.add(insn))
            return;

        var op = insn.getOpcode();
        if(op <= DUP2_X2 && op >= DUP) // avoid removal of dup instruction producer tree instead of excluding entire producer tree
            return;

        if(frame.getStackSize() <= 0)
            return;

        var top = frame.getStack(frame.getStackSize() - 1);
        for(var producer : top.insns) {
            collect(producer, out, frames);
        }
    }

    private Map<AbstractInsnNode, Frame<SourceValue>> analyzeSource(JMethod method) {
        try {
            var frameArr = new Analyzer<>(new SourceInterpreter()).analyzeAndComputeMaxs(method.owner().name(), method.core());
            var map = new HashMap<AbstractInsnNode, Frame<SourceValue>>();

            for(int i = 0; i < method.insns().size(); i++) {
                map.put(method.insns().get(i), frameArr[i]);
            }

            return map;
        } catch (AnalyzerException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }
}

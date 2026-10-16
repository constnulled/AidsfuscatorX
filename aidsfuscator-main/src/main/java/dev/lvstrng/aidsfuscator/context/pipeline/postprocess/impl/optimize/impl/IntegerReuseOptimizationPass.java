package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize.IOptimizationPass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Moves integer values that are reused multiple times via LDC instructions to a local variable to save a little bit of file size.
 * Used mostly to move method.seed() values (not salt) to local variables.
 * @author lvstrng
 */
public class IntegerReuseOptimizationPass implements IOptimizationPass {
    @Override
    public void optimize(Context context, JMethod method) {
        var ldcInts = Arrays.stream(method.insns().toArray())
                .filter(e -> e instanceof LdcInsnNode ldc && ldc.cst instanceof Integer)
                .filter(e -> !method.isUnsafe(e))
                .map(e -> (LdcInsnNode) e)
                .toList();

        var grouped = new HashMap<Integer, List<LdcInsnNode>>();
        for(var ldc : ldcInts) {
            grouped.computeIfAbsent((Integer) ldc.cst, _ -> new ArrayList<>()).add(ldc);
        }

        for(var entry : grouped.entrySet()) {
            var value = entry.getKey();
            var insns = entry.getValue();

            if(insns.size() < 5)
                continue;

            var varIdx = method.allocVar(Type.INT_TYPE);
            var start = new InsnBuilder()
                    ._int(value)
                    ._var(ISTORE, varIdx);

            for(var insn : insns) {
                method.insns().set(insn, new VarInsnNode(ILOAD, varIdx));
            }

            method.addUnsafeInstructions(start.result()); // probably won't be needed, but just in case we later need to reinitialize these instructions
            method.insns().insert(start.result());
        }
    }
}

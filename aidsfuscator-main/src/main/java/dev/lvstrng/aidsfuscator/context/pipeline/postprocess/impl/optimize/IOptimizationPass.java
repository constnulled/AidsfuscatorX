package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Opcodes;

public interface IOptimizationPass extends Opcodes {
    void optimize(Context context, JMethod method);
}

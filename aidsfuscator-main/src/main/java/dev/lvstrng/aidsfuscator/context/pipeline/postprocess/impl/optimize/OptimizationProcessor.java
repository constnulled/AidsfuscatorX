package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize.impl.IntegerReuseOptimizationPass;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize.impl.UnusedLocalVariableCleanTransformer;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.util.List;

public class OptimizationProcessor implements IProcessor {
    private static final List<IOptimizationPass> optimizationPasses = List.of(
            new IntegerReuseOptimizationPass(),
            new UnusedLocalVariableCleanTransformer()
    );

    @Override
    public void run(Context context) {
        Logger.info("Running optimization passes");
        context.classes().forEach(clazz -> clazz.methods().forEach(method -> optimizationPasses.forEach(pass -> pass.optimize(context, method))));
        Logger.success("Ran optimization passes successfully");
    }
}

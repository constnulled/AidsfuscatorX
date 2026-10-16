package dev.lvstrng.aidsfuscator.context.pipeline.postprocess;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IPass;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.AidsfuscatorAnnotationProcessor;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.ArtifactExportProcessor;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.FinishingProcessor;
import dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl.optimize.OptimizationProcessor;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.util.List;
import java.util.function.Supplier;

public class PostProcessorPass implements IPass {
    private static final List<Supplier<IProcessor>> postprocessors = List.of(
            OptimizationProcessor::new,
            AidsfuscatorAnnotationProcessor::new,
            ArtifactExportProcessor::new,
            FinishingProcessor::new
    );

    @Override
    public void run(Context context) {
        Logger.passStart("Post-Processing");
        for (var processor : postprocessors) {
            processor.get().run(context);
        }
        Logger.passComplete("Post-Processing");
    }
}

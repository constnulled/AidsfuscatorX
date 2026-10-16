package dev.lvstrng.aidsfuscator.context.pipeline.preprocess;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IPass;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.context.pipeline.preprocess.impl.ArtifactImportProcessor;
import dev.lvstrng.aidsfuscator.context.pipeline.preprocess.impl.ObfuscatorInitializeProcessor;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.util.List;
import java.util.function.Supplier;

public class PreProcessorPass implements IPass {
    private final List<Supplier<IProcessor>> preprocessors = List.of(
            ArtifactImportProcessor::new,
            ObfuscatorInitializeProcessor::new
    );

    @Override
    public void run(Context context) {
        Logger.passStart("Pre-Processing");
        for (var processor : preprocessors) {
            processor.get().run(context);
        }
        Logger.passComplete("Pre-Processing");
    }
}

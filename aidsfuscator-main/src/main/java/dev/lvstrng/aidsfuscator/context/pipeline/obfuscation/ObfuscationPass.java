package dev.lvstrng.aidsfuscator.context.pipeline.obfuscation;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IPass;
import dev.lvstrng.aidsfuscator.log.Logger;

public class ObfuscationPass implements IPass {
    @Override
    public void run(Context context) {
        Logger.passStart("Obfuscation Pass");

        for (var transformer : context.transformers()) {
            Logger.transformerStart(transformer.name());
            transformer.transform(context);
            Logger.transformerDone(transformer.name(), transformer.changes());
        }

        Logger.passComplete("Obfuscation");
    }
}

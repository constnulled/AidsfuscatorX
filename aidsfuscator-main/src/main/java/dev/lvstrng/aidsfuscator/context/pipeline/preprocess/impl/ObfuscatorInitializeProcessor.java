package dev.lvstrng.aidsfuscator.context.pipeline.preprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.dictionary.AggressiveDictionary;
import dev.lvstrng.aidsfuscator.naming.dictionary.SimpleDictionary;

public class ObfuscatorInitializeProcessor implements IProcessor {
    @Override
    public void run(Context context) {
        if (!context.doesComputeFrames()) {
            Logger.warn("------------------------------------------------");
            Logger.warn("You've disabled frame computation, you will not receive any support. Enable it in config with computeFrames");
            Logger.warn("------------------------------------------------");
        }

        if(context.aggressiveOverload()) {
            context.setDictionary(new AggressiveDictionary(context, context.dictionaryString()));
        } else {
            context.setDictionary(new SimpleDictionary(context, context.dictionaryString()));
        }

        context.libraryLoader().setJavaPath(context.javaPath());
        context.libraryLoader().loadLibraries(context.libs());
        context.hierarchy().build();

        if(context.initOrderLoader().path() != null && !context.initOrderLoader().path().isEmpty())
            context.initOrderLoader().load();
    }
}

package dev.lvstrng.aidsfuscator.file.impl;

import com.google.gson.*;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Writer;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.TransformerOrder;

import java.io.IOException;
import java.nio.file.Files;

public class ConfigWriter implements Writer {
    private final String configPath;
    private final Context context;

    public ConfigWriter(Context context, String configPath) {
        this.context = context;
        this.configPath = configPath;
    }

    @Override
    public void write() throws IOException {
        var configFile = Context.getFromWorkspace(configPath);
        if(!configFile.exists() && !configFile.createNewFile())
            throw new IOException("Couldn't create config file " + configPath);

        var configObject = new JsonObject();
        var transformerJson = new JsonObject();

        configObject.add("in", new JsonPrimitive(context.in()));
        configObject.add("out", new JsonPrimitive(context.out()));
        configObject.add("libs", new JsonPrimitive(context.libs()));
        configObject.add("dictionary", new JsonPrimitive(context.dictionaryString()));
        configObject.add("computeFrames", new JsonPrimitive(context.doesComputeFrames()));
        configObject.add("watermark", new JsonPrimitive(context.watermark()));
        configObject.add("mappingFormat", new JsonPrimitive(context.mappingFormat()));
        configObject.add("aggressiveOverload", new JsonPrimitive(context.aggressiveOverload()));
        configObject.add("exclusionPresets", presetArray());

        for(var transformer : TransformerOrder.transformers()) {
            var transformerObject = new JsonObject();
            transformerObject.add("enabled", new JsonPrimitive(context.transformers().contains(transformer)));

            for(var setting : transformer.settings()) {
                switch (setting.value()) {
                    case String s -> transformerObject.add(setting.key(), new JsonPrimitive(s));
                    case Integer i -> transformerObject.add(setting.key(), new JsonPrimitive(i));
                    case Boolean b -> transformerObject.add(setting.key(), new JsonPrimitive(b));
                    default -> {}
                }
            }
            transformerJson.add(transformer.key(), transformerObject);
        }

        configObject.add("transformers", transformerJson);

        var str = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
                .toJson(configObject);

        Files.writeString(configFile.toPath(), str);
        Logger.success("Saved config successfully!");
    }

    private JsonElement presetArray() {
        var presetArray = new JsonArray();
        for(var preset : context.presetLoader().getKeys()) {
            presetArray.add(preset);
        }
        return presetArray;
    }
}

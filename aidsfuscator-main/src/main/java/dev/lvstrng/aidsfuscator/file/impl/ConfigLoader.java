package dev.lvstrng.aidsfuscator.file.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Loader;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.TransformerOrder;

public class ConfigLoader implements Loader {
    private final String configPath;
    private final Context context;

    public ConfigLoader(Context context, String configPath) {
        this.configPath = configPath;
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void load() {
        var file = Context.getFromWorkspace(configPath);
        if(!file.exists()) {
            Logger.warn("No config file found. Running with no obfuscation.");
            throw new RuntimeException();
        }
        Logger.info("Loading config (%s)...", configPath);

        // ---- CONFIG ----
        var configObject = new Gson().fromJson(Context.readWorkspaceString(configPath), JsonObject.class);
        context.in(configObject.get("in").getAsString())
                .out(configObject.get("out").getAsString())
                .libs(configObject.get("libs").getAsString())
                .setDictionary(configObject.get("dictionary").getAsString())
                .setWatermark(configObject.get("watermark").getAsString());

        if(configObject.has("aggressiveOverload"))
            context.setAggressiveOverload(configObject.get("aggressiveOverload").getAsBoolean());

        if(configObject.has("mappingFormat"))
            context.setMappingFormat(configObject.get("mappingFormat").getAsString());

        if(configObject.get("computeFrames").getAsBoolean())
            context.computeFrames();

        if(configObject.has("exclusionPresets")) {
            context.presetLoader().withKeys(
                    configObject.get("exclusionPresets").getAsJsonArray().asList().stream().map(JsonElement::getAsString).toList()
            );
        }

        // ---- TRANSFORMERS ----
        var transformers = configObject.get("transformers").getAsJsonObject();
        for(var transformer : TransformerOrder.transformers()) {
            var e = transformers.get(transformer.key());
            if(e == null)
                continue;

            var obj = e.getAsJsonObject();
            if(obj == null)
                continue;

            var enabled = obj.get("enabled").getAsBoolean();
            if(enabled)
                context.transformers().add(transformer);

            // ---- SETTINGS ----
            for(var setting : transformer.settings()) {
                var settingObj = obj.get(setting.key());
                if(settingObj == null)
                    continue;

                switch (setting.value()) {
                    case Boolean _ -> ((Setting<Boolean>) setting).setValue(settingObj.getAsBoolean());
                    case String _ -> ((Setting<String>) setting).setValue(settingObj.getAsString());
                    case Integer _ -> ((Setting<Integer>) setting).setValue(settingObj.getAsInt());
                    default -> {}
                }
            }
        }
    }

    public Context result() {
        return context;
    }
}

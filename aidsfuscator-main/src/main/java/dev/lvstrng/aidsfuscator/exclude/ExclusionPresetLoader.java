package dev.lvstrng.aidsfuscator.exclude;

import dev.lvstrng.aidsfuscator.exclude.preset.AnnotationExclusionPreset;
import dev.lvstrng.aidsfuscator.exclude.preset.MinecraftFabricExclusionPreset;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ExclusionPresetLoader {
    private static final Map<String, Supplier<IExclusionPreset>> presets = Map.of(
            "api", AnnotationExclusionPreset::new,
            "mc-fabric", MinecraftFabricExclusionPreset::new
    );

    private final List<String> keys;

    public ExclusionPresetLoader() {
        this.keys = new ArrayList<>();
    }

    public void withKeys(List<String> keys) {
        this.keys.addAll(keys);
    }

    public void loadAll() {
        for(var key : keys) {
            if(!presets.containsKey(key))
                continue;

            Logger.info("Loading exclusion preset `%s`", key);
            presets.get(key).get().load();
        }
    }

    public List<String> getKeys() {
        return keys;
    }
}

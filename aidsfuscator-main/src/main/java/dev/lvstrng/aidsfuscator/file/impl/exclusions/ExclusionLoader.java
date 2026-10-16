package dev.lvstrng.aidsfuscator.file.impl.exclusions;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.file.Loader;
import dev.lvstrng.aidsfuscator.log.Logger;

public class ExclusionLoader implements Loader {
    private final String exclusionPath;

    public ExclusionLoader(String exclusionPath) {
        this.exclusionPath = exclusionPath;
    }

    @Override
    public void load() {
        var file = Context.getFromWorkspace(exclusionPath);
        if(!file.exists()) {
            Logger.warn("Couldn't find exclusions config. Running with no set exclusions.");
            return;
        }

        var configObject = new Gson().fromJson(Context.readWorkspaceString(exclusionPath), JsonObject.class);
        for(var elem : configObject.entrySet()) {
            var exclusion = Exclusions.fromKey(elem.getKey());
            var exclusionObject = elem.getValue().getAsJsonObject();

            for(var type : exclusionObject.entrySet()) {
                var typeStr = type.getKey();
                var arr = type.getValue().getAsJsonArray();

                switch (typeStr) {
                    case "class" -> {
                        for(var e : arr) {
                            exclusion.addClass(e.getAsString());
                        }
                    }
                    case "field" -> {
                        for(var e : arr) {
                            exclusion.addField(e.getAsString());
                        }
                    }
                    case "method" -> {
                        for(var e : arr) {
                            exclusion.addMethod(e.getAsString());
                        }
                    }
                    case "annotation" -> {
                        for(var e : arr) {
                            exclusion.addAnnotation(e.getAsString());
                        }
                    }
                }
            }
        }
    }
}

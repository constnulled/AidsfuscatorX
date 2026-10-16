package dev.lvstrng.aidsfuscator.file.impl.exclusions;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.file.Writer;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;
import java.nio.file.Files;

public class ExclusionWriter implements Writer {
    private final String exclusionPath;

    public ExclusionWriter(String exclusionPath) {
        this.exclusionPath = exclusionPath;
    }

    @Override
    public void write() throws IOException {
        var configFile = Context.getFromWorkspace(exclusionPath);
        if(!configFile.exists() && !configFile.createNewFile())
            throw new IOException("Couldn't create exclusion config file " + exclusionPath);

        var configObject = new JsonObject();
        for(var exclusion : Exclusions.values()) {
            if(exclusion.classExclusions().isEmpty() && exclusion.fieldExclusions().isEmpty() && exclusion.methodExclusions().isEmpty())
                continue;

            var excludeObject = new JsonObject();

            if(!exclusion.classExclusions().isEmpty()) {
                var arr = new JsonArray(exclusion.classExclusions().size());
                for(var excl : exclusion.classExclusions()) {
                    arr.add(new JsonPrimitive(excl.toString()));
                }

                excludeObject.add("class", arr);
            }

            if(!exclusion.fieldExclusions().isEmpty()) {
                var arr = new JsonArray(exclusion.fieldExclusions().size());
                for(var excl : exclusion.fieldExclusions()) {
                    arr.add(new JsonPrimitive(excl.toString()));
                }

                excludeObject.add("field", arr);
            }

            if(!exclusion.methodExclusions().isEmpty()) {
                var arr = new JsonArray(exclusion.methodExclusions().size());
                for(var excl : exclusion.methodExclusions()) {
                    arr.add(new JsonPrimitive(excl.toString()));
                }

                excludeObject.add("method", arr);
            }

            if(!exclusion.annotationExclusions().isEmpty()) {
                var arr = new JsonArray(exclusion.annotationExclusions().size());
                for(var excl : exclusion.annotationExclusions()) {
                    arr.add(new JsonPrimitive(excl.toString()));
                }

                excludeObject.add("annotation", arr);
            }

            configObject.add(exclusion.key(), excludeObject);
        }

        var str = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
                .toJson(configObject);

        Files.writeString(configFile.toPath(), str);
        Logger.success("Saved exclusion config successfully!");
    }
}

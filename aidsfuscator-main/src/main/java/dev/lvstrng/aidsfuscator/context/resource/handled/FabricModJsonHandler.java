package dev.lvstrng.aidsfuscator.context.resource.handled;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.resource.HandledResource;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mappings;

import java.io.IOException;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class FabricModJsonHandler implements HandledResource {
    @Override
    public void handle(Context context, JarOutputStream jos, String name, byte[] bytes) throws IOException {
        var gson = new Gson();
        var modJson = gson.fromJson(new String(bytes), JsonObject.class);

        var entrypoints = modJson.getAsJsonObject("entrypoints");
        if (entrypoints == null) {
            jos.putNextEntry(new ZipEntry(name));
            jos.write(bytes);
            jos.closeEntry();
            Logger.warn("Ignoring `" + name + "`, no entry points found");
            return;
        }

        for (Map.Entry<String, com.google.gson.JsonElement> typeEntry : entrypoints.entrySet()) {
            var value = typeEntry.getValue();
            if (value == null || !value.isJsonArray())
                continue;

            var entrypointArray = value.getAsJsonArray();
            for (int i = 0; i < entrypointArray.size(); i++) {
                var element = entrypointArray.get(i);
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    var remapped = remapEntrypoint(element.getAsString());
                    entrypointArray.set(i, new JsonPrimitive(remapped));
                } else if (element.isJsonObject()) {
                    var obj = element.getAsJsonObject();
                    if (obj.has("value") && obj.get("value").isJsonPrimitive() && obj.get("value").getAsJsonPrimitive().isString()) {
                        var remapped = remapEntrypoint(obj.get("value").getAsString());
                        obj.addProperty("value", remapped);
                    }
                }
            }
        }

        jos.putNextEntry(new ZipEntry(name));
        jos.write(gson.toJson(modJson).getBytes());
        jos.closeEntry();
    }

    private String remapEntrypoint(String raw) {
        if (raw == null) return null;
        String className = raw;
        String methodSuffix = "";
        int separator = raw.indexOf("::");
        if (separator != -1) {
            className = raw.substring(0, separator);
            methodSuffix = raw.substring(separator);
        }
        String internalName = className.replace('.', '/');
        var mapping = Mappings.CLASS.retrieve(internalName);
        if (mapping != null && mapping.value() != null) {
            return mapping.value().replace('/', '.') + methodSuffix;
        }
        return raw;
    }
}

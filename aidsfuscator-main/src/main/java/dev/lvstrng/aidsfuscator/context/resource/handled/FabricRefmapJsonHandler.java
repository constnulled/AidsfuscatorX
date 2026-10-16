package dev.lvstrng.aidsfuscator.context.resource.handled;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.resource.HandledResource;
import dev.lvstrng.aidsfuscator.naming.Mappings;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class FabricRefmapJsonHandler implements HandledResource {
    @Override
    public void handle(Context context, JarOutputStream jos, String name, byte[] bytes) throws IOException {
        var gson = new Gson();
        var modJson = gson.fromJson(new String(bytes), JsonObject.class);

        if(modJson.has("mappings"))
            handle(modJson, "mappings");

        if(modJson.has("data")) {
            var data = modJson.get("data").getAsJsonObject();
            if(data.has("named:intermediary"))
                handle(data, "named:intermediary");
        }

        jos.putNextEntry(new ZipEntry(name));
        jos.write(gson.toJson(modJson).getBytes());
        jos.closeEntry();
    }

    private void handle(JsonObject obj, String key) {
        var entryObj = obj.get(key).getAsJsonObject();

        for(var entry : new HashSet<>(entryObj.getAsJsonObject().entrySet())) {
            var name = entry.getKey();
            if(!Mappings.CLASS.containsOld(name))
                continue;

            entryObj.asMap().remove(name);
            entryObj.asMap().put(Mappings.CLASS.retrieve(name).value(), entry.getValue());
        }
    }
}

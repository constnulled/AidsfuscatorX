package dev.lvstrng.aidsfuscator.file.impl.references;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Writer;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;
import java.nio.file.Files;

public class ReferenceWriter implements Writer {
    public final Context context;
    private final String path;

    public ReferenceWriter(Context context, String path) {
        this.context = context;
        this.path = path;
    }

    @Override
    public void write() throws IOException {
        var file = Context.getFromWorkspace(path);
        if(!file.exists())
            file.createNewFile();

        var configObject = new JsonObject();
        var methodArray = new JsonArray();
        var fieldArray = new JsonArray();

        for(var method : context.referenceManager().methodReferences()) {
            methodArray.add(new JsonPrimitive(method.getFilterString()));
        }

        for(var field : context.referenceManager().fieldReferences()) {
            fieldArray.add(new JsonPrimitive(field.getFilterString()));
        }

        configObject.add("methods", methodArray);
        configObject.add("fields", fieldArray);

        var str = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
                .toJson(configObject);

        Files.writeString(file.toPath(), str);
        Logger.success("Saved reference config successfully!");
    }
}

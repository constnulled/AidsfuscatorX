package dev.lvstrng.aidsfuscator.file.impl.initOrder;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Writer;

import java.io.IOException;
import java.nio.file.Files;

public class ClassInitOrderWriter implements Writer {
    private final Context context;
    private final String orderPath;

    public ClassInitOrderWriter(Context context, String orderPath) {
        this.context = context;
        this.orderPath = orderPath;
    }

    @Override
    public void write() throws IOException {
        var configFile = Context.getFromWorkspace(orderPath);
        if(!configFile.exists() && !configFile.createNewFile())
            throw new IOException("Couldn't create config file " + orderPath);

        var arr = new JsonArray();
        for(var pair : context.initOrder().pairs()) {
            var pairJson = new JsonArray();

            pairJson.add(pair.first.originalName());
            pairJson.add(pair.second.originalName());

            arr.add(pairJson);
        }

        var str = new GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
                .toJson(arr);

        Files.writeString(configFile.toPath(), str);
    }
}

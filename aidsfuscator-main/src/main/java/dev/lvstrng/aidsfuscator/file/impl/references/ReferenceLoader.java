package dev.lvstrng.aidsfuscator.file.impl.references;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Loader;
import dev.lvstrng.aidsfuscator.log.Logger;

public class ReferenceLoader implements Loader {
    private final Context context;
    private final String path;

    public ReferenceLoader(Context context, String path) {
        this.context = context;
        this.path = path;
    }

    @Override
    public void load() {
        var file = Context.getFromWorkspace(path);
        if(!file.exists())
            return;

        var configObject = new Gson().fromJson(Context.readWorkspaceString(path), JsonObject.class);
        if(configObject.has("methods")) {
            var methodObject = configObject.get("methods");
            if(!methodObject.isJsonArray()) {
                Logger.error("Method references are supposed to be an array, not adding any references.");
                return;
            }

            var methods = methodObject.getAsJsonArray();
            for(var elem : methods) {
                context.referenceManager().addMethodCandidate(elem.getAsString());
            }
        }

        if(configObject.has("fields")) {
            var fieldObject = configObject.get("fields");
            if(!fieldObject.isJsonArray()) {
                Logger.error("Field references are supposed to be an array, not adding field references.");
                return;
            }

            var fields = fieldObject.getAsJsonArray();
            for(var elem : fields) {
                context.referenceManager().addFieldCandidate(elem.getAsString());
            }
        }
    }
}

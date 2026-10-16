package dev.lvstrng.aidsfuscator.context.resource;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.resource.handled.FabricMixinJsonHandler;
import dev.lvstrng.aidsfuscator.context.resource.handled.FabricModJsonHandler;
import dev.lvstrng.aidsfuscator.context.resource.handled.FabricRefmapJsonHandler;
import dev.lvstrng.aidsfuscator.context.resource.handled.ManifestHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Class responsible for writing resources after obfuscation.
 * @author lvstrng
 */
public class ResourceHandler {
    private final Context context;
    private final Map<String, byte[]> resources = new HashMap<>();

    private final Map<String, Supplier<HandledResource>> handledResources = Map.of(
            "MANIFEST.MF", ManifestHandler::new,
            "fabric.mod.json", FabricModJsonHandler::new,
            "mixins.json", FabricMixinJsonHandler::new,
            "refmap.json", FabricRefmapJsonHandler::new
    );

    public ResourceHandler(Context context) {
        this.context = context;
    }

    public void handle(JarOutputStream jos) throws IOException {
        for(var resource : resources.entrySet()) {
            var name = resource.getKey();
            var bytes = resource.getValue();

            var handled = false;
            for(var k : handledResources.keySet()) {
                if(name.endsWith(k)) {
                    handledResources.get(k).get().handle(context, jos, name, bytes);

                    handled = true;
                    break;
                }
            }

            if(handled)
                continue;

            jos.putNextEntry(new ZipEntry(name));
            jos.write(bytes);
            jos.closeEntry();
        }
    }

    public void add(String name, byte[] bytes) {
        if (name == null) return;
        resources.put(name, bytes);
    }

    public Map<String, byte[]> resources() {
        return resources;
    }
}
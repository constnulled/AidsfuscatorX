package dev.lvstrng.aidsfuscator.file.mapping;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Writer;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Exports old-to-new names of obfuscated members to make keeping track of crashes and other things easier after obfuscation.
 * @author lvstrng
 */
public class MappingExport implements Writer {
    private final Context context;
    private final StringBuilder builder;

    public MappingExport(Context context) {
        this.context = context;
        this.builder = new StringBuilder();
    }

    @Override
    public void write() throws IOException {
        var file = Context.getFromWorkspace("mappings/latest.txt");
        if(!file.getParentFile().exists() && !file.getParentFile().mkdir()) {
            Logger.error("Couldn't create mappings folder");
            return;
        }

        if(!file.exists() && !file.createNewFile()) {
            Logger.error("Couldn't create mapping file");
            return;
        }

        for(var clazz : context.classes()) {
            sb().append(clazz.originalName()).append("\n");
            if(!clazz.originalName().equals(clazz.name()))
                sb(1).append("new name: ").append(clazz.name()).append("\n"); // if original name is not equal to current name, means remapped

            if(clazz.hasSalt())
                sb(1).append("salt value: ").append(clazz.salt().value()).append("\n");

            if(!clazz.properties().properties().isEmpty()) {
                sb(3).append("properties: \n");

                for(var property : clazz.properties().properties()) {
                    sb(4).append(property.name()).append("\n");
                }
            }

            if(!clazz.fields().isEmpty())
                writeFields(clazz);

            if(!clazz.methods().isEmpty())
                writeMethods(clazz);

            sb().append("\n");
        }

        Files.writeString(file.toPath(), builder.toString());
    }

    private void writeMethods(JClass clazz) {
        sb(1).append("\nmethods: \n");
        for(var method : clazz.methods()) {
            sb(2).append(method.simpleOriginalName()).append("\n");

            if(!method.simpleName().equals(method.simpleOriginalName()))
                sb(3).append("new name: ").append(method.simpleName()).append("\n");

            if(method.hasSalt()) {
                sb(3).append("salt value: ").append(method.salt().value()).append("\n");
                sb(3).append("salt local index: ").append(method.salt().local()).append("\n");
            }

            if(method.properties().properties().isEmpty())
                continue;

            sb(3).append("properties: \n");
            for(var property : method.properties().properties()) {
                sb(4).append(property.name()).append("\n");
            }
        }
    }

    private void writeFields(JClass clazz) {
        sb(1).append("\nfields: \n");
        for(var field : clazz.fields()) {
            sb(2).append(field.simpleOriginalName()).append("\n");

            if(!field.simpleName().equals(field.simpleOriginalName()))
                sb(3).append("new name: ").append(field.simpleName()).append("\n");

            if(field.properties().properties().isEmpty())
                continue;

            sb(3).append("properties: \n");
            for(var property : field.properties().properties()) {
                sb(4).append(property.name()).append("\n");
            }
        }
    }

    private StringBuilder sb() {
        return builder;
    }

    private StringBuilder sb(int tab) {
        return builder.append("\t".repeat(tab));
    }
}

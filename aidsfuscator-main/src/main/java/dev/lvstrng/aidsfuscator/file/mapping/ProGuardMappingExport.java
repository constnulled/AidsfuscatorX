package dev.lvstrng.aidsfuscator.file.mapping;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.file.Writer;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProGuardMappingExport implements Writer {
    private static final String MAPPING_FILE = "mappings/mapping.txt";

    private final Context context;
    private final StringBuilder builder;

    public ProGuardMappingExport(Context context) {
        this.context = context;
        this.builder = new StringBuilder();
    }

    @Override
    public void write() throws IOException {
        var file = Context.getFromWorkspace(MAPPING_FILE);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            Logger.error("Couldn't create mappings folder");
            return;
        }

        if (!file.exists() && !file.createNewFile()) {
            Logger.error("Couldn't create ProGuard mapping file");
            return;
        }

        appendHeader();

        for (var clazz : context.classes()) {
            writeClass(clazz);
        }

        for (var clazz : context.excluded().values()) {
            writeClass(clazz);
        }

        Files.writeString(file.toPath(), builder.toString());
        Logger.success("Exported ProGuard mapping file to %s", MAPPING_FILE);
    }

    private void appendHeader() {
        builder.append("# Lapisfuscator ProGuard-style mapping file\n");
        builder.append("# Generated: ").append(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())
        ).append("\n");
        builder.append("# Format: ProGuard (compatible with ReTrace, R8, Firebase Crashlytics)\n");
        builder.append("#\n");
    }

    private void writeClass(JClass clazz) {
        var originalName = toJavaClassName(clazz.originalName());
        var obfuscatedName = toJavaClassName(clazz.name());

        // Class line: original.ClassName -> obfuscated.ClassName:
        builder.append(originalName).append(" -> ").append(obfuscatedName).append(":\n");

        for (var field : clazz.fields()) {
            var originalFieldName = field.originalName();
            var currentFieldName = field.name();

            var fieldType = toJavaType(field.originalDesc());

            builder.append("    ")
                    .append(fieldType).append(' ')
                    .append(originalFieldName).append(" -> ")
                    .append(currentFieldName)
                    .append('\n');
        }

        for (var method : clazz.methods()) {
            var originalMethodName = method.originalName();
            var currentMethodName = method.name();

            if (originalMethodName.equals("<clinit>"))
                continue;

            var returnType = toJavaType(Type.getReturnType(method.originalDesc()));
            var paramTypes = buildParamList(Type.getArgumentTypes(method.originalDesc()));

            builder.append("    ")
                    .append(returnType).append(' ')
                    .append(originalMethodName).append('(').append(paramTypes).append(')')
                    .append(" -> ")
                    .append(currentMethodName)
                    .append('\n');
        }
    }

    private static String toJavaClassName(String internalName) {
        return internalName.replace('/', '.');
    }

    static String toJavaType(String descriptor) {
        return toJavaType(Type.getType(descriptor));
    }

    static String toJavaType(Type type) {
        return switch (type.getSort()) {
            case Type.VOID    -> "void";
            case Type.BOOLEAN -> "boolean";
            case Type.CHAR    -> "char";
            case Type.BYTE    -> "byte";
            case Type.SHORT   -> "short";
            case Type.INT     -> "int";
            case Type.FLOAT   -> "float";
            case Type.LONG    -> "long";
            case Type.DOUBLE  -> "double";
            case Type.ARRAY   -> toJavaType(type.getElementType()) + "[]".repeat(type.getDimensions());
            case Type.OBJECT  -> type.getClassName();
            case Type.METHOD  -> toJavaType(type.getReturnType()); // fallback for method types
            default           -> type.getDescriptor(); // shouldn't happen
        };
    }

    private static String buildParamList(Type[] argTypes) {
        if (argTypes.length == 0)
            return "";

        var sb = new StringBuilder();
        for (int i = 0; i < argTypes.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(toJavaType(argTypes[i]));
        }
        return sb.toString();
    }
}

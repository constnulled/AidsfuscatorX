package dev.lvstrng.aidsfuscator.context.asm;

import dev.lvstrng.aidsfuscator.context.Context;
import org.objectweb.asm.ClassWriter;

public class HierarchyClassWriter extends ClassWriter {
    private final Context context;

    public HierarchyClassWriter(Context context) {
        this(context, context.writerFlags());
    }

    public HierarchyClassWriter(Context context, int flags) {
        super(flags);
        this.context = context;

        if (!context.watermark().isEmpty()) {
            this.newUTF8(context.watermark());
        }
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        return context.hierarchy().commonSuperClass(type1, type2);
    }
}

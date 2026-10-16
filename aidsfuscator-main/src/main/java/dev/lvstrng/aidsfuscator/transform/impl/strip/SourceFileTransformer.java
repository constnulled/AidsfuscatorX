package dev.lvstrng.aidsfuscator.transform.impl.strip;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class SourceFileTransformer extends Transformer {
    private final Setting<Boolean> fake = setting("fake", false);

    public SourceFileTransformer() {
        super("Source File Strip", "SourceFileTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.SOURCE_FILE.excluded(clazz))
                continue;

            if (fake.value()) {
                clazz.setSourceFile(context.dictionary().newClassName() + ".java");
            } else {
                clazz.setSourceFile(null);
            }

            clazz.setSourceDebug(null);
            if (clazz.core().innerClasses != null)
                clazz.core().innerClasses.clear();
            clazz.core().outerClass = null;
            clazz.core().outerMethod = null;
            clazz.core().outerMethodDesc = null;

            markChange();
        }
    }
}
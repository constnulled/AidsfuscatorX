package dev.lvstrng.aidsfuscator.transform.impl.misc;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;

import java.util.ArrayList;

public class WatermarkTransformer extends Transformer {

    private final Setting<String> text = setting("text", "");

    public WatermarkTransformer() {
        super("Watermark", "WatermarkTransformer");
    }

    @Override
    public void transform(Context context) {
        var watermarkText = text.value();
        if (watermarkText.isEmpty()) {
            return;
        }

        var classes = new ArrayList<>(context.classes());

        for (var clazz : classes) {
            var fieldName = context.dictionary().newFieldName(clazz, "Ljava/lang/String;");
            var access = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;
            clazz.createField(access, fieldName, "Ljava/lang/String;", watermarkText);
            markChange();
        }
    }
}
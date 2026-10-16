package dev.lvstrng.aidsfuscator.transform.impl.strip;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class LocalVariableNameTransformer extends Transformer {
    public LocalVariableNameTransformer() {
        super("Remove Local Names", "LocalNameTransformer");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(Exclusions.LOCAL_NAMES.excluded(clazz))
                continue;

            clazz.methods().forEach(e -> {
                if(Exclusions.LOCAL_NAMES.excluded(e))
                    return;

                e.core().parameters = null;
                e.localVariables().clear();
                markChange();
            });
        }
    }
}

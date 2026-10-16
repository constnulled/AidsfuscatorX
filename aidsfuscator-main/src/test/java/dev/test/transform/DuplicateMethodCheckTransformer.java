package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class DuplicateMethodCheckTransformer extends Transformer {
    public DuplicateMethodCheckTransformer() {
        super("Duplicate Methods", "duplicateMethods");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var other = clazz.methods().stream()
                        .filter(e -> e.name().equals(method.name()))
                        .filter(e -> e.desc().equals(method.desc()))
                        .toList();

                if(other.size() == 1)
                    continue;

                Logger.error("Found duplicate method for method `%s` (original name: %s) in class `%s`. Duplicate method: %s",
                        method.fullName(),
                        method.originalName(),
                        clazz,
                        other.stream().filter(e -> e != method).findFirst().orElseThrow().originalName()
                );
                markChange();
            }
        }
    }
}

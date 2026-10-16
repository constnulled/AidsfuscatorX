package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class OldNameTest extends Transformer {
    public OldNameTest() {
        super("Old Name Test", "oldNameTest");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                Logger.info(method.fullName());
                Logger.info("%s.%s%s", clazz.originalName(), method.originalName(), method.originalDesc());
                System.out.println();
            }
        }
    }
}

package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class NamingTest extends Transformer {
    public NamingTest() {
        super("Naming", "namingTest");
    }

    @Override
    public void transform(Context context) {
        for(var entry : Mappings.METHOD.getMappings().entrySet()) {
            var key = entry.getKey();
            var mapping = entry.getValue();
            Logger.info("%s -> [%s -> %s]", key, mapping.key(), mapping.value());
        }
    }
}

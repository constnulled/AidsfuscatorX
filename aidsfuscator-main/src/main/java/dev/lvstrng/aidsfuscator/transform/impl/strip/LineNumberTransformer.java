package dev.lvstrng.aidsfuscator.transform.impl.strip;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.LineNumberNode;

public class LineNumberTransformer extends Transformer {
    private final Setting<Boolean> remove = setting("remove", true);

    public LineNumberTransformer() {
        super("Line Number Mutation", "LineNumberTransformer");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(Exclusions.LINE_NUMBERS.excluded(clazz))
                continue;

            for(var method : clazz.methods()) {
                if(Exclusions.LINE_NUMBERS.excluded(method))
                    continue;

                for(var insn : method.insns()) {
                    if(!(insn instanceof LineNumberNode ln))
                        continue;

                    if(remove.value()) {
                        method.insns().remove(ln);
                    } else {
                        ln.line = random.nextInt(Short.MAX_VALUE);
                    }
                    markChange();
                }
            }
        }
    }
}

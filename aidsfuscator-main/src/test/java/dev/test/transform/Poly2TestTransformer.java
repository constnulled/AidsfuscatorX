package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.polymorph.full.PolymorphMethodContext;
import dev.lvstrng.aidsfuscator.polymorph.full.args.KeyType;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors.Poly2StringDecryptor;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.HashMap;

public class Poly2TestTransformer extends Transformer {
    public Poly2TestTransformer() {
        super("Poly2 Test", "poly2test");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            var dec = new Poly2StringDecryptor();

            var name = context.dictionary().newMethodName(clazz, dec.getDescriptor());
            dec.setName(name);
            dec.generate(context, clazz, "a", "a");
            prepareValues(dec.context(), clazz.methods().getFirst());

            for(var method : clazz.methods()) {
                for(var insn : method.insns()) {
                    if(!(insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s))
                        continue;

                    if(s.isEmpty())
                        continue;

                    var encrypted = dec.context().encrypt(s);
                    var decrypted = dec.context().decrypt(encrypted);

                    if(!s.equals(decrypted))
                        throw new IllegalStateException("Unequal strings: str = %s; encrypted = %s; decrypted = %s;".formatted(s, encrypted, decrypted));
                    else {
                        Logger.info("encrypted = %s; decrypted = %s;", encrypted, decrypted);
                    }
                }
            }
        }
    }

    private void prepareValues(PolymorphMethodContext context, JMethod method) {
        var className = method.owner().name().replace('/', '.');
        var trace = ((className.hashCode() ^ method.name().hashCode()) >> 16) ^ context.traceXorKey();

        var vals = new HashMap<Integer, Integer>();
        for(var arg : context.args().list()) {
            if(arg.keyType() == KeyType.INDEX_KEY) {
                vals.put(context.args().slot(arg), context.indexXorKey());
                continue;
            }

            vals.put(context.args().slot(arg), arg.type().randomValue());
        }

        context.setValues(trace, vals);
    }
}

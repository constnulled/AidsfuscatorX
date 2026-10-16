package dev.lvstrng.aidsfuscator.transform.impl.data.integers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.decryptors.DefaultIntegerDecryptor;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.initializers.DefaultIntegerInitializer;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.initializers.VarlessIntegerInitializer;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A simple number encryption transformer using XOR.
 */
public class IntegerEncryptTransformer extends Transformer {
    private final List<Supplier<IIntegerInitializer>> initializers = List.of(
            DefaultIntegerInitializer::new,
            VarlessIntegerInitializer::new
    );

    private final List<Supplier<IIntegerDecryptor>> decryptors = List.of(
            DefaultIntegerDecryptor::new
    );
    private final Setting<Boolean> excludeLowSignificant = setting("excludeLowSignificant", true);

    public IntegerEncryptTransformer() {
        super("Encrypt Integer Constants", "IntegerEncryptTransformer");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            if(clazz.isInterface() && clazz.version() < V1_8)
                continue;

            if(Exclusions.INTEGER_ENCRYPTION.excluded(clazz))
                continue;

            var initializer = initializers.get(random.nextInt(initializers.size())).get();
            var decryptor = decryptors.get(random.nextInt(decryptors.size())).get();

            var numbers = new ArrayList<Integer>();
            var decryptorName = context.dictionary().newMethodName(clazz, decryptor.getDescriptor());
            var fieldName = context.dictionary().newFieldName(clazz, "[I");

            decryptor.setName(decryptorName);
            for(var method : clazz.methods()) {
                if(Exclusions.INTEGER_ENCRYPTION.excluded(method))
                    continue;
                var frames = method.frames(context);
                for(var insn : method.insns()) {
                    if(!ASMUtils.isIntPush(insn))
                        continue;

                    if(ASMUtils.isIconst(insn) && excludeLowSignificant.value())
                        continue;

                    if(context.properties().get(insn).has(Property.IGNORE_INTEGER))
                        continue;

                    var num = ASMUtils.getInt(insn);
                    method.insns().insertBefore(insn, decryptor.addAndCall(context, method, insn, frames, numbers, num));
                    method.insns().remove(insn);
                    markChange();
                }
                method.reinitUnsafeInstructions();
            }

            if(numbers.isEmpty()) {
                context.dictionary().revertMethod();
                context.dictionary().revertField();
                continue;
            }

            int access = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;
            var f = clazz.createField(access, fieldName, "[I");
            clazz.reinsertRandomly(random, f);

            decryptor.generate(clazz, fieldName);
            initializer.generate(context, clazz, fieldName, numbers);
        }
    }
}

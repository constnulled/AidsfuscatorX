package dev.lvstrng.aidsfuscator.transform.impl.data.longs;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.longs.decryptors.DefaultLongDecryptor;
import dev.lvstrng.aidsfuscator.transform.impl.data.longs.initializers.DefaultLongInitializer;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LongEncryptTransformer extends Transformer {
    private final List<Supplier<ILongInitializer>> initializers = List.of(
            DefaultLongInitializer::new
    );

    private final List<Supplier<ILongDecryptor>> decryptors = List.of(
            DefaultLongDecryptor::new
    );

    private final Setting<Boolean> excludeLconst = setting("excludeLconst", true);

    public LongEncryptTransformer() {
        super("Encrypt Long Constants", "LongEncryptTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (clazz.isInterface() && clazz.version() < V1_8)
                continue;

            if (Exclusions.LONG_ENCRYPTION.excluded(clazz))
                continue;

            var initializer = initializers.get(random.nextInt(initializers.size())).get();
            var decryptor   = decryptors.get(random.nextInt(decryptors.size())).get();

            var numbers      = new ArrayList<Long>();
            var decryptorName = context.dictionary().newMethodName(clazz, decryptor.getDescriptor());
            var fieldName     = context.dictionary().newFieldName(clazz, "[J");

            decryptor.setName(decryptorName);

            for (var method : clazz.methods()) {
                if (Exclusions.LONG_ENCRYPTION.excluded(method))
                    continue;

                if (method.properties().has(Property.STRING_DECRYPTOR, Property.INTEGER_DECRYPTOR))
                    continue;

                var frames = method.frames(context);
                for (var insn : method.insns()) {
                    if (!ASMUtils.isLongPush(insn))
                        continue;
                    if (ASMUtils.isLconst(insn) && excludeLconst.value())
                        continue;
                    if (context.properties().get(insn).has(Property.IGNORE_LONG))
                        continue;

                    var num = ASMUtils.getLong(insn);
                    method.insns().insertBefore(insn, decryptor.addAndCall(context, method, insn, frames, numbers, num));
                    method.insns().remove(insn);
                    markChange();
                }

                method.reinitUnsafeInstructions();
            }

            if (numbers.isEmpty()) {
                context.dictionary().revertMethod();
                context.dictionary().revertField();
                continue;
            }

            int access = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;
            var f = clazz.createField(access, fieldName, "[J");
            clazz.reinsertRandomly(random, f);

            decryptor.generate(clazz, fieldName);
            initializer.generate(context, clazz, fieldName, numbers);
        }
    }
}

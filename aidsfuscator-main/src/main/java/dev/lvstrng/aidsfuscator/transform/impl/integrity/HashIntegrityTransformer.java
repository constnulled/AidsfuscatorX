package dev.lvstrng.aidsfuscator.transform.impl.integrity;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;

import java.util.ArrayList;
import java.util.List;

public class HashIntegrityTransformer extends Transformer {

    private static final int MIN_DECOYS = 1;
    private static final int MAX_DECOYS = 3;

    public HashIntegrityTransformer() {
        super("Hash Integrity Checks", "HashIntegrityTransformer");
    }

    @Override
    public void transform(Context context) {
        context.hashIntegrityClass().init();

        for (var clazz : context.classes()) {
            obfuscateClass(context, clazz);
        }

        if (changes() != 0) {
            context.addArtificial(context.hashIntegrityClass().get());
        }
    }

    private void obfuscateClass(Context context, JClass clazz) {
        if (Exclusions.HASH_INTEGRITY.excluded(clazz))
            return;

        var desc = "I";
        var fieldAccess = (clazz.isInterface() ? ACC_PUBLIC : ACC_PRIVATE) | ACC_STATIC | ACC_FINAL;

        JField realField = null;
        List<JField> decoyFields = null;

        var value = context.hashIntegrityClass().classValue(clazz);

        for (var method : clazz.methods()) {
            if (Exclusions.HASH_INTEGRITY.excluded(method))
                continue;

            for (var insn : method.insns()) {
                if (!ASMUtils.isIntPush(insn))
                    continue;

                if (!context.properties().get(insn).has(Property.SENSITIVE_CONSTANT))
                    continue;

                if (realField == null) {
                    realField = clazz.createField(fieldAccess, context.dictionary().newFieldName(clazz, desc), desc);
                    decoyFields = createDecoyFields(context, clazz, fieldAccess, desc);
                }

                var v = ASMUtils.getInt(insn);

                var ls = new InsnBuilder()
                        .field(GETSTATIC, clazz.name(), realField.name(), realField.desc())
                        ._int(value ^ v).addProps(context, Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                        .ixor();

                method.insns().insertBefore(insn, ls.result());
                method.insns().remove(insn);
                markChange();
            }
        }

        if (realField != null) {
            injectClinit(context, clazz, realField, decoyFields);
        }
    }

    private List<JField> createDecoyFields(Context context, JClass clazz, int access, String desc) {
        int count = MIN_DECOYS + random.nextInt(MAX_DECOYS - MIN_DECOYS + 1);
        var fields = new ArrayList<JField>(count);
        for (int i = 0; i < count; i++) {
            fields.add(clazz.createField(access, context.dictionary().newFieldName(clazz, desc), desc));
        }
        return fields;
    }

    private void injectClinit(Context context, JClass clazz, JField realField, List<JField> decoyFields) {
        var clinit = clazz.findOrCreateClinit();
        var integrity = context.hashIntegrityClass();

        var list = new InsnBuilder()
                .label()
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._int(integrity.paramValue(clazz))
                .method(INVOKESTATIC, integrity.get().name(), integrity.retriever().name(), integrity.retriever().desc())
                .field(PUTSTATIC, clazz.name(), realField.name(), realField.desc());

        for (var decoy : decoyFields) {
            appendDecoyInit(context, clazz, list, decoy);
        }

        clinit.insns().insert(list.result());
    }

    private void appendDecoyInit(Context context, JClass clazz, InsnBuilder list, JField decoy) {
        int strategy = random.nextInt(3);

        switch (strategy) {
            case 0 -> {
                list._int(random.nextInt()).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                    .field(PUTSTATIC, clazz.name(), decoy.name(), decoy.desc());
            }
            case 1 -> {
                int a = random.nextInt();
                int b = random.nextInt();
                list._int(a).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                    ._int(b).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                    .ixor()
                    .field(PUTSTATIC, clazz.name(), decoy.name(), decoy.desc());
            }
            case 2 -> {
                int a = random.nextInt();
                int b = random.nextInt();
                list._int(a).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                    ._int(b).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                    .iand()
                    .field(PUTSTATIC, clazz.name(), decoy.name(), decoy.desc());
            }
        }

        appendDecoyRead(context, clazz, list, decoy);
    }

    private void appendDecoyRead(Context context, JClass clazz, InsnBuilder list, JField decoy) {
        if (!random.nextBoolean())
            return;

        int noise = random.nextInt();
        list.field(GETSTATIC, clazz.name(), decoy.name(), decoy.desc())
            ._int(noise).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
            .ixor()
            .pop();
    }
}

package dev.lvstrng.aidsfuscator.transform.impl.shuffle;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ShuffleMembersTransformer extends Transformer {
    private final Setting<Boolean> shuffleMethods = setting("shuffleMethods", true);
    private final Setting<Boolean> shuffleFields = setting("shuffleFields", true);
    private final Setting<Boolean> skipEnums = setting("skipEnums", true);
    private final Setting<Boolean> skipRecords = setting("skipRecords", true);

    public ShuffleMembersTransformer() {
        super("Shuffle Members", "ShuffleMembersTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.SHUFFLE_MEMBERS.excluded(clazz))
                continue;
            if (clazz.isModule())
                continue;
            if (skipEnums.value() && clazz.isEnum())
                continue;
            if (skipRecords.value() && clazz.isRecord())
                continue;
            if (clazz.isAnnotation())
                continue;

            var seed = random.nextLong();
            var shuffled = false;

            if (shuffleFields.value() && !clazz.fields().isEmpty()) {
                shuffled = shuffleFields(clazz, seed) || shuffled;
            }

            if (shuffleMethods.value() && !clazz.methods().isEmpty()) {
                shuffled = shuffleMethods(clazz, seed) || shuffled;
            }

            if (shuffled) {
                markChange();
            }
        }
    }

    private boolean shuffleFields(JClass clazz, long seed) {
        var rng = new Random(seed);
        var fields = new ArrayList<>(clazz.fields());

        Collections.shuffle(fields, rng);

        clazz.fields().clear();
        clazz.fields().addAll(fields);

        clazz.core().fields.clear();
        for (var f : fields) {
            clazz.core().fields.add(f.core());
        }

        return true;
    }

    private boolean shuffleMethods(JClass clazz, long seed) {
        var rng = new Random(seed ^ 0xDEADBEEFL);

        JMethod clinit = null;
        var inits = new ArrayList<JMethod>();
        var others = new ArrayList<JMethod>();

        for (var method : clazz.methods()) {
            if (method.name().equals("<clinit>")) {
                clinit = method;
            } else if (method.name().equals("<init>")) {
                inits.add(method);
            } else {
                others.add(method);
            }
        }

        Collections.shuffle(others, rng);

        var result = new ArrayList<JMethod>();
        if (clinit != null) result.add(clinit);
        result.addAll(inits);
        result.addAll(others);

        clazz.methods().clear();
        clazz.methods().addAll(result);
        clazz.core().methods.clear();
        for (var m : result) {
            clazz.core().methods.add(m.core());
        }

        return !others.isEmpty();
    }
}
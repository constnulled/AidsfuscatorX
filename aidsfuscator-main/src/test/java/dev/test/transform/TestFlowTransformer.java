package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;

import java.util.HashSet;
import java.util.List;

public class TestFlowTransformer extends Transformer {
    public TestFlowTransformer() {
        super("Test Flow", "testFlow");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            var desc = "[Ljava/lang/Object;";
            var field = clazz.createField(ACC_PRIVATE | ACC_STATIC, context.dictionary().newFieldName(clazz, desc), desc);
            var validMethods = clazz.methods().stream().filter(JMethod::isSpecial).toList();

            initializeField(clazz, field, validMethods);

            for(var method : validMethods) {

            }
        }
    }

    private void initializeField(JClass clazz, JField field, List<JMethod> validMethods) {
        var clinit = clazz.findOrCreateClinit();
        var builder = new InsnBuilder()
                .label()
                ._int(validMethods.size())
                .anewarray("java/lang/StackTraceElement")
                .field(PUTSTATIC, clazz.name(), field.name(), field.desc());

        clinit.addUnsafeInstructions(builder.result());
    }
}

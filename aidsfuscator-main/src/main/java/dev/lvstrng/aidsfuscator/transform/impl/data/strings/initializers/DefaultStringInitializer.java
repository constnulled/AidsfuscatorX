package dev.lvstrng.aidsfuscator.transform.impl.data.strings.initializers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringInitializer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;

import java.util.List;

/**
 * The default string encryption initializer. It has hardly any security to it, but it does the job. It is planned to move away from this in the future (or update it to be more secure).
 * @author lvstrng
 */
@Deprecated
public class DefaultStringInitializer implements IStringInitializer {
    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName, List<String> strings) {
        // ---- INIT ----
        int key = random.nextInt(Short.MAX_VALUE);
        var strBuilder = new StringBuilder();
        var lengthStr = new StringBuilder();

        for(var str : strings) {
            strBuilder.append(str);
            lengthStr.append((char) (str.length() ^ key));
        }

        var theStr = strBuilder.toString();
        var lenStr = lengthStr.toString();

        var method = clazz.findOrCreateClinit();

        // ---- LOCALS ----
        var keyVar = method.allocVar(Type.INT_TYPE);
        var strVar = method.allocVar();
        var lenArrVar = method.allocVar();
        var strArrVar = method.allocVar();
        var iVar = method.allocVar(Type.INT_TYPE);
        var offsetVar = method.allocVar(Type.INT_TYPE);
        var lenVar = method.allocVar(Type.INT_TYPE);
        var statusVar = method.allocVar(Type.INT_TYPE);

        // ---- CODE ----
        var loop = new LabelNode();
        var varLbl = new LabelNode();

        var builder = new InsnBuilder()
                .label();
        if(clazz.hasSalt()) {
            builder.add(context.properties().add(ASMUtils.pushInt(key ^ clazz.salt().value()), Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER))
                    .add(clazz.salt().load())
                    .ixor();
        } else {
            builder.add(context.properties().add(ASMUtils.pushInt(key), Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER));
        }

        builder._var(ISTORE, keyVar)
                .insertRandomly(new InsnBuilder().label()._const(theStr)._var(ASTORE, strVar))
                .insertRandomly(new InsnBuilder().label()._const(lenStr).method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")._var(ASTORE, lenArrVar))

                .label()
                ._var(ALOAD, lenArrVar)
                .arraylength()
                .anewarray("java/lang/String")
                ._var(ASTORE, strArrVar)

                .label()
                ._int(-1)
                ._var(ISTORE, statusVar)
                ._goto(varLbl)

                // loop start
                .label(loop)
                ._var(ALOAD, lenArrVar)
                ._var(ILOAD, iVar)
                .caload()
                ._var(ILOAD, keyVar)
                .ixor()
                ._var(ISTORE, lenVar)

                .label()
                ._var(ALOAD, strArrVar)
                ._var(ILOAD, iVar)

                ._var(ALOAD, strVar) // str
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, lenVar)
                .iadd()
                .method(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;") // target method
                .aastore()

                .label()
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, lenVar)
                .iadd()
                ._var(ISTORE, offsetVar)

                .label()
                .iinc(iVar, 1)

                .label()
                ._var(ILOAD, iVar)
                ._var(ALOAD, lenArrVar)
                .arraylength()
                .jump(IF_ICMPLT, loop)

                // loop end
                .label()
                ._int(0)
                ._var(ISTORE, statusVar)

                .label(varLbl)
                ._int(0)
                ._var(ISTORE, iVar)

                .label()
                ._int(0)
                ._var(ISTORE, offsetVar)

                .label()
                ._int(0)
                ._var(ISTORE, lenVar)

                .label()
                ._var(ILOAD, statusVar)
                .jump(IFNE, loop)

                .label()
                ._var(ALOAD, strArrVar)
                .field(PUTSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")

                .label()
                ._var(ALOAD, strArrVar)
                .arraylength()
                .anewarray("java/lang/Object")
                .field(PUTSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;");

        method.insertSafe(builder.result());
        method.reinitUnsafeInstructions();
    }
}

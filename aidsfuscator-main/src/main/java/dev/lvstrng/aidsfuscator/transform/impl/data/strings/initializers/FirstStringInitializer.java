package dev.lvstrng.aidsfuscator.transform.impl.data.strings.initializers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringInitializer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.CryptUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;

import java.util.List;

/**
 * String initializer that uses a loop and a switch with a set of keys to XOR strings.
 * @author lvstrng
 */
public class FirstStringInitializer implements IStringInitializer {
    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName, List<String> strings) {
        var keys = CryptUtils.generateKeys(random, random.nextInt(5, 7), 255);
        var key = random.nextInt(Short.MAX_VALUE);

        var strBuilder = new StringBuilder();
        var lengthStr = new StringBuilder();
        var twice = random.nextBoolean();

        for (var str : strings) {
            var enc = CryptUtils.xor(str, twice ? key : 0, keys);
            strBuilder.append(enc);
            if (twice) {
                lengthStr.append((char) enc.length());
            } else {
                lengthStr.append((char) (enc.length() ^ key));
            }
        }

        var theStr = strBuilder.toString();
        var lenStr = lengthStr.toString();

        var method = clazz.findOrCreateClinit();

        var keyVar     = method.allocVar(Type.INT_TYPE);
        var strVar     = method.allocVar();
        var lenArrVar  = method.allocVar();
        var strArrVar  = method.allocVar();
        var iVar       = method.allocVar(Type.INT_TYPE);
        var offsetVar  = method.allocVar(Type.INT_TYPE);
        var lenVar     = method.allocVar(Type.INT_TYPE);
        var statusVar  = method.allocVar(Type.INT_TYPE);
        var charsVar   = method.allocVar();
        var jVar       = method.allocVar(Type.INT_TYPE);
        var xorKey     = method.allocVar(Type.INT_TYPE);

        var innerLoop = new LabelNode();
        var loop      = new LabelNode();
        var varLbl    = new LabelNode();

        var builder = new InsnBuilder()
                .label();
        if (clazz.hasSalt()) {
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
                .insertRandomly(new InsnBuilder().label()._int(-1)._var(ISTORE, statusVar))
                ._goto(varLbl)

                .label(loop)
                ._var(ALOAD, lenArrVar)
                ._var(ILOAD, iVar)
                .caload();
        if (!twice)
            builder._var(ILOAD, keyVar).ixor();
        builder._var(ISTORE, lenVar)

                .label()
                ._var(ALOAD, strVar)
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, offsetVar)
                ._var(ILOAD, lenVar)
                .iadd()
                .method(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;")
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, charsVar)

                ._int(0)
                ._var(ISTORE, jVar)

                .label(innerLoop)
                ._var(ILOAD, jVar)
                .add(context.properties().add(ASMUtils.pushInt(keys.length), Property.IGNORE_INTEGER))
                .irem()
        ;

        var end          = new LabelNode();
        var routeBuilder = new InsnBuilder();
        var lbls         = new LabelNode[keys.length];

        for (int i = 0; i < keys.length; i++) {
            var route = new LabelNode();
            lbls[i] = route;

            routeBuilder.label(route)
                    .add(context.properties().add(ASMUtils.pushInt(keys[i]), Property.IGNORE_INTEGER))
                    ._goto(end);
        }

        builder.tableswitch(lbls[0], 0, keys.length - 1, lbls)
                .add(routeBuilder)

                .label(end)
                ._var(ISTORE, xorKey)

                .label()
                ._var(ALOAD, charsVar)
                ._var(ILOAD, jVar)
                .dup2()
                .caload()
                ._var(ILOAD, xorKey)
                .ixor();
        if (twice)
            builder._var(ILOAD, keyVar).ixor();
        builder.i2c()
                .castore()

                .label()
                .iinc(jVar, 1)

                .label()
                ._var(ILOAD, jVar)
                ._var(ALOAD, charsVar)
                .arraylength()
                .jump(IF_ICMPLT, innerLoop)

                .label()
                ._var(ALOAD, strArrVar)
                ._var(ILOAD, iVar)
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, charsVar)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                .method(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;")
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
        clazz.reinsertRandomly(random, method);
    }
}

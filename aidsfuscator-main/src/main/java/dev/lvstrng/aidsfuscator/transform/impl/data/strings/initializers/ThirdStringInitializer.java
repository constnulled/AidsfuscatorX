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

public class ThirdStringInitializer implements IStringInitializer {
    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName, List<String> strings) {
        var method = clazz.findOrCreateClinit();
        var key = random.nextInt(Short.MAX_VALUE);

        var strBuilder = new StringBuilder();
        var bits = random.nextInt(2, 6); // to save constantpool entry size, use small values for bit count
        var keys = CryptUtils.generateKeys(random, random.nextInt(5, 8), 255);

        for(var string : strings) {
            strBuilder
                    .append((char) (string.length())) // len
                    .append(encrypt(string, key, bits, keys)); // str
        }
        var blobStr = strBuilder.toString();

        // ---- LOCALS ----
        var blobVar = method.allocVar();
        var blobLenVar = method.allocVar(Type.INT_TYPE);
        var strArrVar = method.allocVar();
        var iVar = method.allocVar(Type.INT_TYPE);
        var ptrVar = method.allocVar(Type.INT_TYPE);
        var keyVar = method.allocVar(Type.INT_TYPE);

        var lenVar = method.allocVar(Type.INT_TYPE);
        var chars = method.allocVar();
        var xVar = method.allocVar(Type.INT_TYPE);

        var charVar = method.allocVar(Type.INT_TYPE);

        var keyBuilder = new InsnBuilder().label();
        if(clazz.hasSalt()) {
            keyBuilder
                    ._int(key ^ clazz.salt().value()).addProps(context, Property.IGNORE_INTEGER, Property.SENSITIVE_CONSTANT)
                    .add(clazz.salt().load())
                    .ixor();
        } else {
            keyBuilder._int(key).addProps(context, Property.IGNORE_INTEGER, Property.SENSITIVE_CONSTANT);
        }
        keyBuilder._var(ISTORE, keyVar);

        var loopLabel = new LabelNode();
        var body = new InsnBuilder()
                .label()
                ._const(blobStr)
                .dup()
                .method(INVOKEVIRTUAL, "java/lang/String", "length", "()I")
                ._var(ISTORE, blobLenVar)
                ._var(ASTORE, blobVar)
                .insertRandomly(new InsnBuilder()._int(strings.size()).anewarray("java/lang/String")._var(ASTORE, strArrVar))
                .insertRandomly(new InsnBuilder()._int(0)._var(ISTORE, iVar))
                .insertRandomly(new InsnBuilder()._int(0)._var(ISTORE, ptrVar))
                .insertRandomly(keyBuilder)
                ;

        var loopBody = new InsnBuilder()
                .label(loopLabel)
                ._var(ALOAD, blobVar)
                ._var(ILOAD, ptrVar)
                .method(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C")
                ._var(ISTORE, lenVar)

                .label()
                ._var(ALOAD, blobVar)
                ._var(ILOAD, ptrVar)
                ._int(1)
                .iadd()
                .dup()
                ._var(ILOAD, lenVar)
                .iadd()
                .method(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;")
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, chars)
                .insertRandomly(new InsnBuilder().label()._int(0)._var(ISTORE, xVar).label())
                ;

        var exitLabel = new LabelNode();
        var odd = new LabelNode();
        var secondLoop = new LabelNode();

        var secondLoopBody = new InsnBuilder()
                .label(secondLoop)
                ._var(ALOAD, chars)
                ._var(ILOAD, xVar)
                .caload()
                ._var(ISTORE, charVar);

        var end = new LabelNode();
        var routeBuilder = new InsnBuilder();
        var lbls = new LabelNode[keys.length];

        for(int i = 0; i < keys.length; i++) {
            var route = new LabelNode();
            lbls[i] = route;

            routeBuilder.label(route)
                    .add(context.properties().add(ASMUtils.pushInt(keys[i]), Property.IGNORE_INTEGER))
                    ._goto(end);
        }

        secondLoopBody.label()
                ._var(ILOAD, xVar)
                ._int(keys.length).addProps(context, Property.IGNORE_INTEGER)
                .irem()
                .tableswitch(lbls[0], 0, keys.length - 1, lbls)
                .add(routeBuilder)

                .label(end)
                .i2c()
                ._var(ILOAD, charVar)
                .ixor()
                ._var(ISTORE, charVar)
        ;

        secondLoopBody.label()
                ._var(ILOAD, xVar)
                ._int(2)
                .irem()
                .jump(IFNE, odd)

                // if number is even (((c >>> bits) | (c << (16 - bits))) & 0xFFFF)
                .label()
                ._var(ALOAD, chars)
                ._var(ILOAD, xVar)
                ._var(ILOAD, charVar)
                ._int(bits)
                .iushr()
                ._var(ILOAD, charVar)
                ._int(16 - bits).addProps(context, Property.IGNORE_FLOW_INTS)
                .ishl()
                .ior()
                ._int(0xFFFF).addProps(context, Property.IGNORE_FLOW_INTS)
                .iand()
                ._var(ILOAD, keyVar)
                .ixor()
                .i2c()
                .castore()
                ._goto(exitLabel)

                //if number is odd (((c << bits) | (c >>> (16 - bits))) & 0xFFFF)
                .label(odd)
                ._var(ALOAD, chars)
                ._var(ILOAD, xVar)
                ._var(ILOAD, charVar)
                ._int(bits)
                .ishl()
                ._var(ILOAD, charVar)
                ._int(16 - bits).addProps(context, Property.IGNORE_FLOW_INTS)
                .iushr()
                .ior()
                ._int(0xFFFF).addProps(context, Property.IGNORE_FLOW_INTS)
                .iand()
                ._var(ILOAD, keyVar)
                .ixor()
                .i2c()
                .castore()

                .label(exitLabel)
                .iinc(xVar, 1)

                .label()
                ._var(ILOAD, xVar)
                ._var(ALOAD, chars)
                .arraylength()
                .jump(IF_ICMPLT, secondLoop)
                ;

        var afterLoop = new InsnBuilder()
                .label()
                ._var(ALOAD, strArrVar)
                ._var(ILOAD, iVar)
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, chars)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                .aastore()

                .label()
                .iinc(iVar, 1)

                .label()
                ._var(ILOAD, ptrVar)
                ._var(ILOAD, lenVar)
                .iadd()
                ._int(1)
                .iadd()
                ._var(ISTORE, ptrVar)

                .label()
                ._var(ILOAD, ptrVar)
                ._var(ILOAD, blobLenVar)
                .jump(IF_ICMPLT, loopLabel)

                .label()
                ._var(ALOAD, strArrVar)
                .field(PUTSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")

                .label()
                ._var(ALOAD, strArrVar)
                .arraylength()
                .anewarray("java/lang/Object")
                .field(PUTSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;");

        secondLoopBody.add(afterLoop);
        loopBody.add(secondLoopBody);
        body.add(loopBody);

        method.insertSafe(body.result());
        method.reinitUnsafeInstructions();
        clazz.reinsertRandomly(random, method);
    }

    private static String encrypt(String s, int key, int bits, int[] keys) {
        var chars = s.toCharArray();

        for(int i = 0; i < chars.length; i++) {
            var c = chars[i];

            if(i % 2 == 0) {
                c = left((char) (c ^ key), bits);
            } else {
                c = right((char) (c ^ key), bits);
            }

            c ^= (char) keys[i % keys.length];
            chars[i] = c;
        }

        return new String(chars);
    }

    private static char left(char c, int bits) {
        return (char) (((c << bits) | (c >>> (16 - bits))) & 0xFFFF);
    }

    private static char right(char c, int bits) {
        return (char) (((c >>> bits) | (c << (16 - bits))) & 0xFFFF);
    }
}

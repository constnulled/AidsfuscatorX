package dev.lvstrng.aidsfuscator.transform.impl.data.longs.initializers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.longs.ILongInitializer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DefaultLongInitializer implements ILongInitializer {

    @Override
    public void generate(Context context, JClass clazz, String fieldName, List<Long> numbers) {
        var clinit  = clazz.findOrCreateClinit();
        var key     = random.nextLong();
        var theStr  = new StringBuilder();

        for (var num : numbers) {
            theStr.append(new String(longToBytes(num ^ key), StandardCharsets.ISO_8859_1));
        }

        var bytesVar  = clinit.allocVar();
        var lenVar    = clinit.allocVar(Type.INT_TYPE);
        var keyVar    = clinit.allocVar(Type.LONG_TYPE);
        var iVar      = clinit.allocVar(Type.INT_TYPE);
        var iVarReal  = clinit.allocVar(Type.INT_TYPE);
        var valVar    = clinit.allocVar(Type.LONG_TYPE);

        var loop = new LabelNode();

        var builder = new InsnBuilder().label();

        if (!clazz.hasSalt()) {
            builder.add(context.properties().add(ASMUtils.pushLong(key), Property.SENSITIVE_CONSTANT, Property.IGNORE_LONG));
        } else {
            builder.add(context.properties().add(ASMUtils.pushLong(key ^ clazz.salt().value()), Property.SENSITIVE_CONSTANT, Property.IGNORE_LONG))
                    .add(clazz.salt().load())
                    .i2l()
                    .lxor();
        }

        builder._var(LSTORE, keyVar)
                .label()
                .add(context.properties().add(new LdcInsnNode(theStr.toString()), Property.IGNORE_STRING))
                .add(context.properties().add(new LdcInsnNode("ISO-8859-1"), Property.IGNORE_STRING))
                .method(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B")
                ._var(ASTORE, bytesVar)

                .label()
                ._var(ALOAD, bytesVar)
                .arraylength()
                ._int(8)
                .idiv()
                ._var(ISTORE, lenVar)

                .label()
                ._var(ILOAD, lenVar)
                .add(new org.objectweb.asm.tree.IntInsnNode(NEWARRAY, T_LONG))
                .field(PUTSTATIC, clazz.name(), fieldName, "[J")

                .label()
                ._int(0)
                ._var(ISTORE, iVar)
                .label()
                ._int(0)
                ._var(ISTORE, iVarReal)

                .label(loop);

        for (int byteOff = 0; byteOff < 8; byteOff++) {
            int shift = (7 - byteOff) * 8;
            builder._var(ALOAD, bytesVar);
            if (byteOff == 0) {
                builder._var(ILOAD, iVar);
            } else {
                builder._var(ILOAD, iVar)
                        ._int(byteOff)
                        .iadd();
            }
            builder.baload()
                    .i2l()
                    ._long(0xFFL).addProps(context, Property.IGNORE_LONG)
                    .land();
            if (shift > 0) {
                builder._int(shift).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                        .lshl();
            }
            if (byteOff > 0) {
                builder.lor();
            }
        }

        builder._var(LSTORE, valVar)

                .label()
                ._var(LLOAD, valVar)
                ._var(LLOAD, keyVar)
                .lxor()
                ._var(LSTORE, valVar)

                .field(GETSTATIC, clazz.name(), fieldName, "[J")
                ._var(ILOAD, iVarReal)
                ._var(LLOAD, valVar)
                .lastore()

                .label()
                .iinc(iVar, 8)
                .iinc(iVarReal, 1)
                ._var(ILOAD, iVarReal)
                ._var(ILOAD, lenVar)
                .jump(IF_ICMPLT, loop);

        clinit.insertSafe(builder.result());
        clinit.reinitUnsafeInstructions();

        clazz.reinsertRandomly(random, clinit);
    }
}

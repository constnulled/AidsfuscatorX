package dev.lvstrng.aidsfuscator.transform.impl.data.integers.initializers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.IIntegerInitializer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DefaultIntegerInitializer implements IIntegerInitializer {
    @Override
    public void generate(Context context, JClass clazz, String fieldName, List<Integer> numbers) {
        var clinit = clazz.findOrCreateClinit();
        var key = random.nextInt();
        var theStr = new StringBuilder();

        for(var num : numbers) {
            theStr.append(new String(intToBytes(num ^ key), StandardCharsets.ISO_8859_1));
        }

        var bytesVar = clinit.allocVar();
        var lenVar = clinit.allocVar(Type.INT_TYPE);
        var keyVar = clinit.allocVar(Type.INT_TYPE);
        var iVar = clinit.allocVar(Type.INT_TYPE);
        var iVarReal = clinit.allocVar(Type.INT_TYPE);
        var valVar = clinit.allocVar(Type.INT_TYPE);

        var loop = new LabelNode();

        var builder = new InsnBuilder().label();
        if(!clazz.hasSalt()) {
            builder.add(context.properties().add(ASMUtils.pushInt(key), Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER));
        } else {
            builder.add(context.properties().add(ASMUtils.pushInt(key ^ clazz.salt().value()), Property.SENSITIVE_CONSTANT, Property.IGNORE_INTEGER))
                    .add(clazz.salt().load())
                    .ixor();
        }
        builder._var(ISTORE, keyVar)
                .label()
                .add(context.properties().add(new LdcInsnNode(theStr.toString()), Property.IGNORE_STRING))
                .add(context.properties().add(new LdcInsnNode("ISO-8859-1"), Property.IGNORE_STRING))
                .method(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B")
                ._var(ASTORE, bytesVar)

                .label()
                ._var(ALOAD, bytesVar)
                .arraylength()
                ._int(4)
                .idiv()
                ._var(ISTORE, lenVar)

                .label()
                ._var(ILOAD, lenVar)
                .newarray(T_INT)
                .field(PUTSTATIC, clazz.name(), fieldName, "[I")

                .label()
                ._int(0)
                ._var(ISTORE, iVar)

                .label()
                ._int(0)
                ._var(ISTORE, iVarReal)

                .label(loop)

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                .baload()
                ._int(255)
                .iand()
                ._int(24)
                .ishl()

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._int(1)
                .iadd()
                .baload()
                ._int(255)
                .iand()
                ._int(16)
                .ishl()
                .ior()

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._int(2)
                .iadd()
                .baload()
                ._int(255)
                .iand()
                ._int(8)
                .ishl()
                .ior()

                ._var(ALOAD, bytesVar)
                ._var(ILOAD, iVar)
                ._int(3)
                .iadd()
                .baload()
                ._int(255)
                .iand()
                .ior()
                ._var(ISTORE, valVar)

                .label()
                ._var(ILOAD, valVar)
                ._var(ILOAD, keyVar)
                .ixor()
                ._var(ISTORE, valVar)

                .field(GETSTATIC, clazz.name(), fieldName, "[I")
                ._var(ILOAD, iVarReal)
                ._var(ILOAD, valVar)
                .iastore()

                .label()
                .iinc(iVar, 4)
                .iinc(iVarReal, 1)
                ._var(ILOAD, iVarReal)
                ._var(ILOAD, lenVar)
                .jump(IF_ICMPLT, loop);

        clinit.insertSafe(builder.result());
        clinit.reinitUnsafeInstructions();

        clazz.reinsertRandomly(random, clinit);
    }
}

package dev.lvstrng.aidsfuscator.transform.impl.data.integers.initializers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.IIntegerInitializer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.LabelNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class VarlessIntegerInitializer implements IIntegerInitializer {
    @Override
    public void generate(Context context, JClass clazz, String fieldName, List<Integer> numbers) {
        var clinit = clazz.findOrCreateClinit();
        var key = random.nextInt();
        var theStr = new StringBuilder();

        for(var num : numbers) {
            theStr.append(new String(intToBytes(num ^ key), StandardCharsets.ISO_8859_1));
        }

        var loopLbl = new LabelNode();
        var builder = new InsnBuilder()
                .label()
                ._int(0) // idx
                ._int(0) // ptr
                ._const(theStr.toString()).addProps(context, Property.IGNORE_STRING)
                ._const("ISO-8859-1").addProps(context, Property.IGNORE_STRING)
                .method(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B")
                .insertRandomly(new InsnBuilder()
                        .label()
                        ._int(numbers.size())
                        .newarray(T_INT)
                        .field(PUTSTATIC, clazz.name(), fieldName, "[I")
                        .label()
                )

                .label(loopLbl) //idx, ptr, byte[]
                .dup_x1() // idx, byte[], ptr, byte[]
                .swap() // idx, byte[], byte[], ptr
                .dup_x1() // idx, byte[], ptr, byte[], ptr

                // load bytes
                .label()
                .baload() // idx, byte[], ptr, byte1
                ._int(0xFF).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                .iand()
                ._int(24).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                .ishl() // idx, byte[], ptr, (byte1 & 0xFF) << 24
                .dup_x2()
                .pop() // idx, byte1, byte[], ptr

                .label()
                .dup2()
                ._int(1)
                .iadd()
                .baload() // idx, byte1, byte[], ptr, byte2
                ._int(0xFF).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                .iand()
                ._int(16).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                .ishl() // idx, byte1, byte[], ptr, (byte2 & 0xFF) << 16
                .dup_x2()
                .pop() // idx, byte1, byte2, byte[], ptr

                .label()
                .dup2() // idx, byte1, byte2, byte[], ptr, byte[], ptr
                ._int(2)
                .iadd()
                .baload() // idx, byte1, byte2, byte[], ptr, byte3
                ._int(0xFF).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                .iand()
                ._int(8).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                .ishl() // idx, byte1, byte2, byte[], ptr, (byte3 & 0xFF) << 8
                .dup_x2()
                .pop() // idx, byte1, byte2, byte3, byte[], ptr

                .label()
                .dup2() // idx, byte1, byte2, byte3, byte[], ptr, byte[], ptr
                ._int(3)
                .iadd()
                .baload() // idx, byte1, byte2, byte3, byte[], ptr, byte4
                ._int(0xFF).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                .iand() // idx, byte1, byte2, byte3, byte[], ptr, (byte4 & 0xFF)
                .dup_x2()
                .pop() // idx, byte1, byte2, byte3, byte4, byte[], ptr

                // OR gate all of them together
                .label()
                .dup2_x2() // idx, byte1, byte2, byte[], ptr, byte3, byte4, byte[], ptr
                .pop2()
                .ior() // idx, byte1, byte2, byte[], ptr, byte3_4
                .dup_x2()
                .pop() // idx, byte1, byte2, byte3_4, byte[], ptr
                .dup2_x2()
                .pop2() // idx, byte1, byte[], ptr, byte2, byte3_4
                .ior() // idx, byte1, byte[], ptr, byte2_3_4
                .dup_x2()
                .pop() // idx, byte1, byte2_3_4, byte[], ptr
                .dup2_x2()
                .pop2() // idx, byte[], ptr, byte1, byte2_3_4
                .ior() // idx, byte[], ptr, byte1_2_3_4
                .dup2_x2()
                .pop2() // ptr, int, idx, byte[]
                .dup_x2()
                .pop() // ptr, byte[], int, idx
                .dup_x1()
                .swap() // ptr, byte[], idx, idx, int
                .label();
        if(clazz.hasSalt()) {
            builder._int(key ^ clazz.salt().value()).addProps(context, Property.IGNORE_INTEGER, Property.SENSITIVE_CONSTANT)
                    .add(clazz.salt().load())
                    .ixor().label()
            ;
        } else {
            builder._int(key).addProps(context, Property.SENSITIVE_CONSTANT)
                    .label();
        }

        builder
                .ixor()
                .field(GETSTATIC, clazz.name(), fieldName, "[I") // ptr, byte[], idx, idx, int, int[]
                .dup_x2()
                .pop() // ptr, byte[], idx, int[], idx, int
                .iastore() // ptr, byte[], idx
                ._int(1)
                .iadd() // ptr, byte[], idx + 1
                .dup_x2()
                .pop() //idx, ptr, byte[]
                .swap()
                ._int(4)
                .iadd()
                .swap()
                .dup2()
                .arraylength()
                .label()
                .jump(IF_ICMPLT, loopLbl)
                .label()
                .pop2()
                .pop()
        ;

        clinit.insertSafe(builder.result());
        clinit.reinitUnsafeInstructions();

        clazz.reinsertRandomly(random, clinit);
    }
}

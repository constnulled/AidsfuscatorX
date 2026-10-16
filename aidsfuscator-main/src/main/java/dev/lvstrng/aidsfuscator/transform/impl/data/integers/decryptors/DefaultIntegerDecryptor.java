package dev.lvstrng.aidsfuscator.transform.impl.data.integers.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.polymorph.semi.IntPolymorphStack;
import dev.lvstrng.aidsfuscator.polymorph.semi.impl.AddMask;
import dev.lvstrng.aidsfuscator.polymorph.semi.impl.SubMask;
import dev.lvstrng.aidsfuscator.polymorph.semi.impl.XorMask;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.IIntegerDecryptor;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DefaultIntegerDecryptor implements IIntegerDecryptor {
    private String name;
    private final int idxXor;
    private final IntPolymorphStack stack;

    public DefaultIntegerDecryptor() {
        this.idxXor = random.nextInt();
        this.stack = new IntPolymorphStack();

        int masks = random.nextInt(5, 10) + 1;
        List<Supplier<IntMask<?>>> types = List.of(
                () -> new XorMask().ofRandomValue(Character.MAX_VALUE),
                () -> new SubMask().ofRandomValue(Character.MAX_VALUE),
                () -> new AddMask().ofRandomValue(Character.MAX_VALUE)
        );

        for(int i = 0; i < masks; i++) {
            stack.push(types.get(random.nextInt(types.size())).get());
        }
    }

    @Override
    public void generate(JClass clazz, String fieldName) {
        int access = (clazz.isInterface())
                ? ACC_PUBLIC | ACC_STATIC
                : ACC_PRIVATE | ACC_STATIC;
        var method = clazz.createMethod(access, name, getDescriptor());
        var idxVal = method.allocVar(Type.INT_TYPE);
        var key = method.allocVar(Type.INT_TYPE);
        var value = method.allocVar(Type.INT_TYPE);

        new InsnBuilder(method.insns())
                .label()
                .field(GETSTATIC, clazz.name(), fieldName, "[I")
                ._var(ILOAD, idxVal)
                ._int(idxXor)
                .ixor()
                .iaload()
                ._var(ILOAD, key)
                .ixor()
                ._var(ILOAD, idxVal)
                .ixor()
                ._var(ISTORE, value)
                ._var(ILOAD, value)
                .add(stack.dumpWithList(() -> new InsnBuilder()._var(ISTORE, value)._var(ILOAD, value).result()))
                ._ireturn()
        ;

        clazz.reinsertRandomly(random, method);
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescriptor() {
        return "(II)I";
    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<Integer> numbers, int num) {
        var key = random.nextInt();
        int idxValue = numbers.size() ^ idxXor;
        num = stack.applyInverse(ASMUtils.getInt(callSite)) ^ key ^ idxValue;
        numbers.add(num);

        var builder = new InsnBuilder()
                ._int(idxValue)
                .add(method.protectedIntPush(context, key));

        builder.method(INVOKESTATIC, method.owner().name(), name, getDescriptor(), method.owner().isInterface()).addProps(context, Property.IGNORE_REF_OBFUSCATION);
        return builder.result();
    }
}

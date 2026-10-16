package dev.lvstrng.aidsfuscator.transform.impl.data.longs.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.longs.ILongDecryptor;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.List;
import java.util.Map;

public class DefaultLongDecryptor implements ILongDecryptor {
    private String name;
    private final long idxXor;

    public DefaultLongDecryptor() {
        this.idxXor = random.nextLong();
    }

    @Override
    public void generate(JClass clazz, String fieldName) {
        int access = clazz.isInterface()
                ? ACC_PUBLIC | ACC_STATIC
                : ACC_PRIVATE | ACC_STATIC;
        var method = clazz.createMethod(access, name, getDescriptor());

        var idxVar  = method.allocVar(Type.LONG_TYPE);
        var keyVar  = method.allocVar(Type.LONG_TYPE);
        var valVar  = method.allocVar(Type.LONG_TYPE);

        new InsnBuilder(method.insns())
                .label()
                .field(GETSTATIC, clazz.name(), fieldName, "[J")
                ._var(LLOAD, idxVar)
                ._long(idxXor)
                .lxor()
                .l2i()
                .laload()
                ._var(LLOAD, keyVar)
                .lxor()
                ._var(LLOAD, idxVar)
                .lxor()
                ._var(LSTORE, valVar)
                ._var(LLOAD, valVar)
                ._lreturn();

        clazz.reinsertRandomly(random, method);
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescriptor() {
        return "(JJ)J";
    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite,
                               Map<AbstractInsnNode, SimpleFrame> frames, List<Long> numbers, long num) {
        var key = random.nextLong();
        long rawIdx = numbers.size();
        long idxValue = rawIdx ^ idxXor;

        long stored = num ^ key ^ idxValue;
        numbers.add(stored);

        var builder = new InsnBuilder()
                ._long(idxValue).addProps(context, Property.IGNORE_LONG)
                ._long(key).addProps(context, Property.IGNORE_LONG);

        builder.method(INVOKESTATIC, method.owner().name(), name, getDescriptor(),
                method.owner().isInterface()).addProps(context, Property.IGNORE_REF_OBFUSCATION);

        return builder.result();
    }
}

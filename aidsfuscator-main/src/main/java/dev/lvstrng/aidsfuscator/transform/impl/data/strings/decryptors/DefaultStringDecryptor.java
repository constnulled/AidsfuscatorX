package dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringDecryptor;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.CryptUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;
import java.util.Map;

public class DefaultStringDecryptor implements IStringDecryptor {
    private String decryptorName;
    private final int[] keys;
    private final int idxXor, traceXor;

    public DefaultStringDecryptor() {
        this.keys = CryptUtils.generateKeys(random, 32, 255);
        this.idxXor = random.nextInt();
        this.traceXor = random.nextInt();
    }

    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName) {
        var method = clazz.createMethod(ACC_PRIVATE | ACC_STATIC, decryptorName, "(II)Ljava/lang/String;");

        var idxValVar       = method.allocVar(Type.INT_TYPE);
        var xorValueVar     = method.allocVar(Type.INT_TYPE);
        var cachedTraceVar  = method.allocVar(Type.INT_TYPE);
        var idxVar          = method.allocVar(Type.INT_TYPE);
        var charArrVar      = method.allocVar();
        var stackElementsVar = method.allocVar();
        var elementVar      = method.allocVar();
        var hashVar         = method.allocVar(Type.INT_TYPE);
        var iVar            = method.allocVar(Type.INT_TYPE);
        var xorKey          = method.allocVar(Type.INT_TYPE);

        var loop          = new LabelNode();
        var newTraceLabel = new LabelNode();
        var exitLabel     = new LabelNode();

        var builder = new InsnBuilder()
                .label(new LabelNode())
                ._var(ILOAD, idxValVar)
                .add(context.properties().add(ASMUtils.pushInt(idxXor), Property.IGNORE_INTEGER))
                .ixor()
                ._var(ISTORE, idxVar)
                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")
                ._var(ILOAD, idxVar)
                .aaload()
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, charArrVar)
                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
                ._var(ILOAD, idxVar)
                .aaload()
                .type(CHECKCAST, "[Ljava/lang/StackTraceElement;")
                ._var(ASTORE, cachedTraceVar)
                .label(new LabelNode())
                ._var(ALOAD, cachedTraceVar)
                .jump(IFNULL, newTraceLabel)
                .label(new LabelNode())
                ._var(ALOAD, cachedTraceVar)
                ._var(ASTORE, stackElementsVar)
                ._goto(exitLabel)
                .label(newTraceLabel)
                .type(NEW, "java/lang/Throwable")
                .dup()
                .method(INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V")
                .method(INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;")
                ._var(ASTORE, stackElementsVar)
                .label(new LabelNode())
                .field(GETSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
                ._var(ILOAD, idxVar)
                ._var(ALOAD, stackElementsVar)
                .aastore()
                .label(exitLabel)
                ._var(ALOAD, stackElementsVar)
                ._const(1)
                .aaload()
                ._var(ASTORE, elementVar)
                .label(new LabelNode())
                ._var(ALOAD, elementVar)
                .method(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;")
                .method(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I")
                ._var(ALOAD, elementVar)
                .method(INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;")
                .method(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I")
                .ixor()
                ._const(16)
                .ishr()
                ._const(traceXor)
                .ixor()
                ._var(ISTORE, hashVar)
                .label(new LabelNode())
                ._int(0)
                ._var(ISTORE, iVar)
                .label(loop)
                ._var(ILOAD, iVar)
                .add(context.properties().add(ASMUtils.pushInt(keys.length - 1), Property.IGNORE_INTEGER))
                .iand();

        var end          = new LabelNode();
        var routeBuilder = new InsnBuilder();
        var lbls         = new LabelNode[keys.length];

        for (int i = 0; i < keys.length; i++) {
            var route = new LabelNode();
            lbls[i] = route;

            routeBuilder
                    .label(route)
                    .add(context.properties().add(ASMUtils.pushInt(keys[i]), Property.IGNORE_INTEGER))
                    ._var(ISTORE, xorKey)
                    ._goto(end);
        }

        builder.tableswitch(lbls[0], 0, keys.length - 1, lbls)
                .add(routeBuilder)
                .label(end)
                ._var(ALOAD, charArrVar)
                ._var(ILOAD, iVar)
                ._var(ALOAD, charArrVar)
                ._var(ILOAD, iVar)
                .caload()
                ._var(ILOAD, xorKey)
                .ixor()
                ._var(ILOAD, xorValueVar)
                .add(context.properties().add(ASMUtils.pushInt(16), Property.IGNORE_INTEGER))
                .ishr()
                .ixor()
                ._var(ILOAD, hashVar)
                .ixor()
                .castore()
                .label(new LabelNode())
                .iinc(iVar, 1)
                .label(new LabelNode())
                ._var(ILOAD, iVar)
                ._var(ALOAD, charArrVar)
                .arraylength()
                .jump(IF_ICMPLT, loop)
                .label(new LabelNode())
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, charArrVar)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                .method(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;")
                ._areturn();

        method.insns().add(builder.result());
        method.properties().add(Property.STRING_DECRYPTOR);
        clazz.reinsertRandomly(random, method);
    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<String> strings, String str) {
        var callerClass  = method.owner().name().replace('/', '.');
        var callerMethod = method.name();
        var traceKey     = ((callerClass.hashCode() ^ callerMethod.hashCode()) >> 16) ^ traceXor;

        var key             = random.nextInt() >> 16;
        var encryptedString = CryptUtils.xor(str, key ^ traceKey, keys);
        var idx             = strings.size();
        var idxVal          = idx ^ idxXor;

        var builder = new InsnBuilder()
                .add(context.properties().add(ASMUtils.pushInt(idxVal), Property.IGNORE_INTEGER))
                .add(method.protectedIntPush(context, (key << 16) | (method.hasSalt() ? 0 : random.nextInt(Short.MAX_VALUE))))
                .method(INVOKESTATIC, method.owner().name(), decryptorName, "(II)Ljava/lang/String;", method.owner().isInterface()).addProps(context, Property.IGNORE_REF_OBFUSCATION);

        strings.add(encryptedString);
        return builder.result();
    }

    @Override
    public void setName(String name) {
        this.decryptorName = name;
    }

    @Override
    public String getDescriptor() {
        return "(II)Ljava/lang/String;";
    }
}

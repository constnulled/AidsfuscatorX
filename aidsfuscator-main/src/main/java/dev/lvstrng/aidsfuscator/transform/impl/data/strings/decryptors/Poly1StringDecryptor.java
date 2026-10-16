package dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.polymorph.semi.IntMask;
import dev.lvstrng.aidsfuscator.polymorph.semi.IntPolymorphStack;
import dev.lvstrng.aidsfuscator.polymorph.semi.impl.AddMask;
import dev.lvstrng.aidsfuscator.polymorph.semi.impl.SubMask;
import dev.lvstrng.aidsfuscator.polymorph.semi.impl.XorMask;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringDecryptor;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Poly1StringDecryptor implements IStringDecryptor {
    private String name;
    private final int idxXor, traceXor;
    private final IntPolymorphStack stack;
    private final List<Arg> args = new ArrayList<>(List.of(
            Arg.INDEX, Arg.KEY1, Arg.KEY2)
    );

    public Poly1StringDecryptor() {
        this.idxXor = random.nextInt(Character.MAX_VALUE);
        this.traceXor = random.nextInt(Short.MAX_VALUE);

        this.stack = new IntPolymorphStack();
        int masks = random.nextInt(3, 10) + 1;

        List<Supplier<IntMask<?>>> types = List.of(
                () -> new XorMask().ofRandomValue(Character.MAX_VALUE),
                () -> new SubMask().ofRandomValue(Character.MAX_VALUE),
                () -> new AddMask().ofRandomValue(Character.MAX_VALUE)
        );

        for(int i = 0; i < masks; i++) {
            stack.push(types.get(random.nextInt(types.size())).get());
        }
        args.forEach(Arg::reset);
        Collections.shuffle(args);
    }

    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName) {
        var method = clazz.createMethod(ACC_PRIVATE | ACC_STATIC, name, getDescriptor());

        var idxValVar = -1; // index
        var xorValueVar = -1; // key1
        var keyParamVar = -1; // key2

        for(var arg : args) {
            switch (arg) {
                case INDEX -> idxValVar = method.allocVar(Type.INT_TYPE);
                case KEY1 -> xorValueVar = method.allocVar(Type.INT_TYPE);
                case KEY2 -> keyParamVar = method.allocVar(Type.INT_TYPE);
            }
        }

        var idxVar = method.allocVar(Type.INT_TYPE);
        var charArrVar = method.allocVar();
        var cachedTraceVar = method.allocVar();
        var stackElementsVar = method.allocVar();
        var elementVar = method.allocVar();
        var hashVar = method.allocVar(Type.INT_TYPE);
        var iVar = method.allocVar(Type.INT_TYPE);
        var valueVar = method.allocVar(Type.INT_TYPE);
        var loop = new LabelNode();
        var newTraceLabel = new LabelNode();
        var exitLabel = new LabelNode();

        var body = new InsnBuilder(method.insns())
                .label()
                ._var(ILOAD, idxValVar)
                .add(context.properties().add(ASMUtils.pushInt(idxXor), Property.IGNORE_INTEGER))
                .ixor()
                ._var(ISTORE, idxVar)
                .label()
                .field(GETSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")
                ._var(ILOAD, idxVar)
                .aaload()
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, charArrVar)
                .label()
                .field(GETSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
                ._var(ILOAD, idxVar)
                .aaload()
                .type(CHECKCAST, "[Ljava/lang/StackTraceElement;")
                ._var(ASTORE, cachedTraceVar)
                .label()
                ._var(ALOAD, cachedTraceVar)
                .jump(IFNULL, newTraceLabel)
                .label()
                ._var(ALOAD, cachedTraceVar)
                ._var(ASTORE, stackElementsVar)
                ._goto(exitLabel)
                .label(newTraceLabel)
                .type(NEW, "java/lang/Throwable")
                .dup()
                .method(INVOKESPECIAL, "java/lang/Throwable", "<init>", "()V")
                .method(INVOKEVIRTUAL, "java/lang/Throwable", "getStackTrace", "()[Ljava/lang/StackTraceElement;")
                ._var(ASTORE, stackElementsVar)
                .label()
                .field(GETSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
                ._var(ILOAD, idxVar)
                ._var(ALOAD, stackElementsVar)
                .aastore()
                .label(exitLabel)
                ._var(ALOAD, stackElementsVar)
                ._const(1)
                .aaload()
                ._var(ASTORE, elementVar)
                .label()
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
                .label()
                ._int(0)
                ._var(ISTORE, iVar)
                .label(loop)
                ._var(ALOAD, charArrVar)
                ._var(ILOAD, iVar)
                .dup2()
                .caload();

        body.add(stack.dumpWithList(() -> new InsnBuilder()._var(ISTORE, valueVar)._var(ILOAD, valueVar).result()));

        body._var(ILOAD, hashVar)
                .ixor()
                ._var(ILOAD, xorValueVar)
                .ixor()
                ._var(ILOAD, keyParamVar)
                .add(context.properties().add(ASMUtils.pushInt(16), Property.IGNORE_INTEGER))
                .ishr()
                .ixor()
                .i2c()
                .castore()
                .label()
                .iinc(iVar, 1)
                .label()
                ._var(ILOAD, iVar)
                ._var(ALOAD, charArrVar)
                .arraylength()
                .jump(IF_ICMPLT, loop)
                .label()
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, charArrVar)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                .method(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;")
                ._areturn()
        ;

        clazz.reinsertRandomly(random, method);
        clazz.properties().add(Property.STRING_DECRYPTOR);
    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<String> strings, String str) {
        var callerClass = method.owner().name().replace('/', '.');
        var callerMethod = method.name();
        var traceKey = ((callerClass.hashCode() ^ callerMethod.hashCode()) >> 16) ^ traceXor;
        var key = method.hasSalt() ? method.salt().value() >> 16 : random.nextInt() >> 16;
        var idx = strings.size();

        var idxVal = idx ^ idxXor;
        var firstKey = random.nextInt(Character.MAX_VALUE);

        var list = new InsnBuilder();
        for(var arg : args) {
            switch (arg) {
                case INDEX -> list._int(idxVal).addProps(context, Property.IGNORE_INTEGER);
                case KEY1 -> list._int(firstKey).addProps(context, Property.IGNORE_INTEGER);
                case KEY2 -> list.add(method.protectedIntPush(context, (key << 16) | (method.hasSalt() ? 0 : random.nextInt(Short.MAX_VALUE))));
            }
        }
        list.method(INVOKESTATIC, method.owner().name(), name, getDescriptor(), method.owner().isInterface()).addProps(context, Property.IGNORE_REF_OBFUSCATION);

        var chars = str.toCharArray();
        for(int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] ^ traceKey ^ firstKey ^ key);
            chars[i] = (char) stack.applyInverse(chars[i]);
        }

        strings.add(new String(chars));
        return list.result();
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescriptor() {
        var format = new StringBuilder("(");
        for(var arg : args) {
            format.append(arg.type());
        }
        return format + ")Ljava/lang/String;";
    }

    private enum Arg {
        INDEX("CCI"), KEY1("CCSSI"), KEY2("I");
        private final String possibleTypes;
        private String type;

        Arg(String possibleTypes) {
            this.possibleTypes = possibleTypes;
            reset();
        }

        public String type() {
            return type;
        }

        public void reset() {
            this.type = String.valueOf(possibleTypes.charAt(random.nextInt(possibleTypes.length())));
        }
    }
}

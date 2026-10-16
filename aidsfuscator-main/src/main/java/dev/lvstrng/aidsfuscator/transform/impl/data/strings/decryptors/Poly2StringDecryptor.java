package dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.polymorph.full.PolymorphMethodContext;
import dev.lvstrng.aidsfuscator.polymorph.full.args.ArgType;
import dev.lvstrng.aidsfuscator.polymorph.full.args.KeyType;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringDecryptor;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import dev.lvstrng.aidsfuscator.utils.Utils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Poly2StringDecryptor implements IStringDecryptor {
    private String name;
    private final PolymorphMethodContext ctx = new PolymorphMethodContext(1, 3).initialize();

    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName) {
        var method = clazz.createMethod(ACC_PRIVATE | ACC_STATIC, name, getDescriptor());
        method.setMaxLocals(ctx.maxLocals() + 1);

        var traceVar = ctx.traceVar();
        var charArrVar = ctx.charArrVar();

        var strIdxVar = method.allocVar(Type.INT_TYPE);
        var cachedTraceVar = method.allocVar();
        var stackElementsVar = method.allocVar();
        var elementVar = method.allocVar();

        var newTraceLabel = new LabelNode();
        var exitLabel = new LabelNode();

        var builder = new InsnBuilder(method.insns())
                .label()
                ._var(ILOAD, ctx.args().slot(ctx.args().indexParam()))
                ._int(ctx.indexXorKey())
                .ixor()
                ._var(ISTORE, strIdxVar)

                .label()
                .field(GETSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")
                ._var(ILOAD, strIdxVar)
                .aaload()
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, charArrVar)

                .label()
                .field(GETSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
                ._var(ILOAD, strIdxVar)
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
                ._var(ILOAD, strIdxVar)
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
                ._const(ctx.traceXorKey())
                .ixor()
                ._var(ISTORE, traceVar)
                ;

        builder.add(ctx.instructions());
        builder.label()
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, charArrVar)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                .method(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;")
                ._areturn();

        clazz.properties().add(Property.STRING_DECRYPTOR);
        clazz.reinsertRandomly(random, method);
    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<String> strings, String str) {
        var idx = strings.size();
        var args = prepareValues(ctx, method, idx);
        strings.add(ctx.encrypt(str));

        var builder = new InsnBuilder();
        int saltChance = 50;
        for(int i = 0; i < args.length; i++) {
            var value = args[i];
            var arg = ctx.args().list().get(i);

            if(!method.hasSalt() || !Utils.chance(random, saltChance)) {
                builder._int(value);
                saltChance += 25;
            } else {
                var masked = method.salt().value() | method.seed();
                builder
                        .add(method.salt().load())
                        ._int(method.seed()).addProps(context, Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS)
                        .ior()
                        ._int(masked ^ value).addProps(context, Property.IGNORE_INTEGER)
                        .ixor();
                saltChance = 50;
            }
            if (arg.type() != ArgType.INT && Utils.chance(random, 50)) {
                builder.add(new InsnNode(switch (arg.type()) {
                    case BYTE -> I2B;
                    case CHAR -> I2C;
                    case SHORT -> I2S;
                    default -> throw new RuntimeException("Unhandled type %s".formatted(arg.type()));
                }));
            }
        }

        builder.method(INVOKESTATIC, method.owner().name(), name, getDescriptor(), method.owner().isInterface()).addProps(context, Property.IGNORE_REF_OBFUSCATION);
        return builder.result();
    }

    private int[] prepareValues(PolymorphMethodContext context, JMethod method, int index) {
        var className = method.owner().name().replace('/', '.');
        var trace = ((className.hashCode() ^ method.name().hashCode()) >> 16) ^ context.traceXorKey();
        var args = new int[context.args().list().size()];

        var vals = new HashMap<Integer, Integer>();
        for(int i = 0; i < context.args().list().size(); i++) {
            var arg = context.args().list().get(i);

            if(arg.keyType() == KeyType.INDEX_KEY) {
                vals.put(context.args().slot(arg), index ^ context.indexXorKey());
                args[i] = index ^ context.indexXorKey();
                continue;
            }

            var val = arg.type().randomValue();
            vals.put(context.args().slot(arg), val);
            args[i] = val;
        }

        context.setValues(trace, vals);
        return args;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescriptor() {
        return ctx.args().descriptor();
    }

    public PolymorphMethodContext context() {
        return ctx;
    }
}

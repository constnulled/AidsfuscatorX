package dev.lvstrng.aidsfuscator.classgen.impl.ref;

import dev.lvstrng.aidsfuscator.classgen.IClassGen;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import dev.lvstrng.aidsfuscator.utils.SwitchUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;

import java.lang.invoke.*;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class ReferenceObfuscationClassGenerator implements IClassGen {
    private static final String invokerDesc = MethodType.methodType(
            CallSite.class,
            MethodHandles.Lookup.class,
            String.class,
            MethodType.class
    ).toMethodDescriptorString();

    private static final String middleInvokerDesc = MethodType.methodType(
            Object.class,
            MethodHandles.Lookup.class,
            MutableCallSite.class,
            String.class,
            MethodType.class,
            Object[].class
    ).toMethodDescriptorString();

    private static final String mainInvokerDesc = MethodType.methodType(
            MethodHandle.class, //return type (method handle)
            MethodHandles.Lookup.class, // lookup for the class in which the dynamic occurs in
            MutableCallSite.class, // callsite
            String.class, //name of callSite method that got called when indy got invoked
            MethodType.class, //method descriptor specified in the callsite specifier
            int.class, //method index key
            int.class // decryption key
    ).toMethodDescriptorString();

    private final char[] chars; // {v, s, vg, sg, vs, ss}
    private final int indexKey;

    public JMethod outerInvoker;
    public JField refField;

    public ReferenceObfuscationClassGenerator(char[] chars, int indexKey) {
        this.chars = chars;
        this.indexKey = indexKey;
    }

    @Override
    public JClass create(Context context) {
        var clazz = context.createClass("java/lang/Object", ACC_PUBLIC | ACC_SUPER);
        var midInvokerName = context.dictionary().newMethodName(clazz, middleInvokerDesc);
        var mainInvokerName = context.dictionary().newMethodName(clazz, mainInvokerDesc);
        var decryptorName = context.dictionary().newMethodName(clazz, "(II)Ljava/lang/String;");
        var refFieldName = context.dictionary().newFieldName(clazz, "[Ljava/lang/String;");
        refField = clazz.createField(ACC_STATIC, refFieldName, "[Ljava/lang/String;");

        createOuterInvoker(context, clazz, midInvokerName);
        createMiddleInvoker(context, clazz, midInvokerName, mainInvokerName);
        createMainInvoker(context, clazz, mainInvokerName, decryptorName);
        createDecryptor(clazz, decryptorName);
        context.addArtificial(clazz);
        return clazz;
    }

    public void generateClinit(Context context, JClass clazz, List<String> references) {
        var maxReferencesPer = 2000;
        var count = (int) Math.ceil((double) references.size() / maxReferencesPer);

        var clinit = clazz.findOrCreateClinit();
        var clinitBuilder = new InsnBuilder()
                .label()
                ._int(references.size())
                .anewarray("java/lang/String")
                .field(PUTSTATIC, clazz.name(), refField.name(), refField.desc());

        int j = 0;
        for(var i = 0; i < count; i++) {
            var name = context.dictionary().newMethodName(clazz, "()V");
            var method = clazz.createMethod(ACC_STATIC, name, "()V");
            var methodBuilder = new InsnBuilder(method.insns())
                    .field(GETSTATIC, clazz.name(), refField.name(), refField.desc());

            for(int n = Math.min(j + maxReferencesPer, references.size()); j < n; j++) {
                var ref = references.get(j);
                methodBuilder
                        .dup()
                        ._int(j)
                        ._const(ref)
                        .aastore();
            }

            methodBuilder.pop()._return();
            clinitBuilder.method(INVOKESTATIC, clazz.name(), name, method.desc());
        }

        clinit.insns().insert(clinitBuilder.result());
    }

    private void createDecryptor(JClass clazz, String name) {
        var method = clazz.createMethod(ACC_STATIC, name, "(II)Ljava/lang/String;");
        // ---- LOCALS ----
        var idxParam = method.allocVar(Type.INT_TYPE);
        var keyParam = method.allocVar(Type.INT_TYPE);

        var sb = method.allocVar();

        // ---- CODE ----
        var loop = new LabelNode();
        var builder = new InsnBuilder(method.insns())
                .label()
                .type(NEW, "java/lang/StringBuilder")
                .dup()
                .field(GETSTATIC, clazz.name(), refField.name(), refField.desc())
                ._var(ILOAD, idxParam)
                .aaload()
                .method(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V")
                ._var(ASTORE, sb)

                ._int(0)

                .label(loop)
                ._var(ALOAD, sb)
                .swap()
                .dup_x1()
                .method(INVOKEVIRTUAL, "java/lang/StringBuilder", "charAt", "(I)C")
                ._var(ILOAD, keyParam)
                ._int(16)
                .iushr()
                .ixor()

                ._var(ALOAD, sb) // i, ch ^ key, sb
                .dup2_x1()
                .pop2() // ch ^ key, sb, i
                .dup_x2().pop() // i, ch, sb
                .dup_x2().pop() // sb, i, ch
                .swap() // sb, ch, i
                .dup_x2()
                .swap()
                .method(INVOKEVIRTUAL, "java/lang/StringBuilder", "setCharAt", "(IC)V")

                .label()
                ._int(1)
                .iadd()
                .dup() // i, i

                ._var(ALOAD, sb)
                .method(INVOKEVIRTUAL, "java/lang/StringBuilder", "length", "()I")
                .jump(IF_ICMPLT, loop)

                .pop()
                ._var(ALOAD, sb)
                .method(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;")
                ._areturn();


    }

    private void createOuterInvoker(Context context, JClass clazz, String midInvokeName) {
        var name = context.dictionary().newMethodName(clazz, invokerDesc);
        var method = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, invokerDesc);
        var builder = new InsnBuilder(method.insns())
                .label() // var v3 = new MutableCallSite();
                .type(NEW, "java/lang/invoke/MutableCallSite")
                .dup()
                ._var(ALOAD, 2)
                .method(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodType;)V")
                ._var(ASTORE, 3)

                .label()
                ._var(ALOAD, 3)
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "lookup", "()Ljava/lang/invoke/MethodHandles$Lookup;")
                ._const(clazz.type())
                ._const(midInvokeName)
                ._const(Type.getObjectType("java/lang/Object"))
                ._const(Type.getObjectType("java/lang/invoke/MethodHandles$Lookup"))
                ._int(4)
                .anewarray("java/lang/Class")

                .dup()
                ._int(0)
                ._const(Type.getObjectType("java/lang/invoke/MutableCallSite"))
                .aastore()

                .dup()
                ._int(1)
                ._const(Type.getObjectType("java/lang/String"))
                .aastore()

                .dup()
                ._int(2)
                ._const(Type.getObjectType("java/lang/invoke/MethodType"))
                .aastore()

                .dup()
                ._int(3)
                ._const(Type.getObjectType("[Ljava/lang/Object;"))
                .aastore()

                .method(INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;")
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                ._const(Type.getObjectType("[Ljava/lang/Object;"))
                ._var(ALOAD, 2)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "parameterCount", "()I")
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asCollector", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;")
                ._int(0)
                ._int(4)
                .anewarray("java/lang/Object")

                .dup()
                ._int(0)
                ._var(ALOAD, 0)
                .aastore()

                .dup()
                ._int(1)
                ._var(ALOAD, 3)
                .aastore()

                .dup()
                ._int(2)
                ._var(ALOAD, 1)
                .aastore()

                .dup()
                ._int(3)
                ._var(ALOAD, 2)
                .aastore()

                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "insertArguments", "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;")
                ._var(ALOAD, 2)
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "explicitCastArguments", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKEVIRTUAL, "java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V")
                ._var(ALOAD, 3)
                ._areturn();

        outerInvoker = method;
    }

    private void createMiddleInvoker(Context context, JClass clazz, String name, String mainInvokerName) {
        var method = clazz.createMethod(ACC_PRIVATE | ACC_STATIC, name, middleInvokerDesc);
        var builder = new InsnBuilder(method.insns())
                .label()
                ._var(ALOAD, 4)
                .dup()
                .arraylength()
                ._int(2)
                .isub()
                .aaload()
                .type(CHECKCAST, "java/lang/Integer")
                .method(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I")
                ._var(ISTORE, 5) //index key

                .label()
                ._var(ALOAD, 4)
                .dup()
                .arraylength()
                ._int(1)
                .isub()
                .aaload()
                .type(CHECKCAST, "java/lang/Integer")
                .method(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I")
                ._var(ISTORE, 6) //dec key

                .label()
                ._var(ALOAD, 1)
                ._var(ALOAD, 0)
                ._var(ALOAD, 1)
                ._var(ALOAD, 2)
                ._var(ALOAD, 3)
                ._var(ILOAD, 5)
                ._var(ILOAD, 6)
                .method(INVOKESTATIC, clazz.name(), mainInvokerName, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/invoke/MutableCallSite;Ljava/lang/String;Ljava/lang/invoke/MethodType;II)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, 7)

                .label()
                ._var(ALOAD, 7)
                ._var(ALOAD, 3)
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "explicitCastArguments", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKEVIRTUAL, "java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V")

                .label()
                ._var(ALOAD, 7)
                ._const(Type.getObjectType("[Ljava/lang/Object;"))
                ._var(ALOAD, 4)
                .arraylength()
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asSpreader", "(Ljava/lang/Class;I)Ljava/lang/invoke/MethodHandle;")
                ._var(ALOAD, 4)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;")
                ._areturn();

    }

    private void createMainInvoker(Context context, JClass clazz, String mainInvokerName, String decryptorName) {
        var method = clazz.createMethod(ACC_PRIVATE | ACC_STATIC, mainInvokerName, mainInvokerDesc);
        // ---- LOCALS ----
        // -(args)
        var lookupParam = method.allocVar();
        var mutableCallSiteParam = method.allocVar();
        var nameParam = method.allocVar();
        var methodTypeParam = method.allocVar();
        var indexXorKeyParam = method.allocVar(Type.INT_TYPE);
        var decryptionKeyParam = method.allocVar(Type.INT_TYPE);

        // -(real locals)
        var indexVar = method.allocVar(Type.INT_TYPE);
        var typeVar = method.allocVar(Type.INT_TYPE);
        var stringsVar = method.allocVar();
        var classVar = method.allocVar();
        var nameVar = method.allocVar();
        var targetType = method.allocVar();
        var handle = method.allocVar();

        var builder = new InsnBuilder(method.insns())
                .label()
                ._var(ILOAD, indexXorKeyParam)
                ._int(indexKey)
                .ixor()
                ._var(ISTORE, indexVar)
                .insertRandomly(new InsnBuilder()
                        .label()
                        ._var(ALOAD, nameParam)
                        ._int(0)
                        .method(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C")
                        ._var(ISTORE, typeVar)
                )

                .label() // var strings = refField[idx].split(":");
                ._var(ILOAD, indexVar)
                ._var(ILOAD, decryptionKeyParam)
                .method(INVOKESTATIC, clazz.name(), decryptorName, "(II)Ljava/lang/String;")
                ._const(":")
                .method(INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;")
                ._var(ASTORE, stringsVar)

                .label() // var clazz = Class.forName(strings[0]);
                ._var(ALOAD, stringsVar)
                ._int(0)
                .aaload()
                .method(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;")
                ._var(ASTORE, classVar)

                .label() // var name = strings[1];
                ._var(ALOAD, stringsVar)
                ._int(1)
                .aaload()
                ._var(ASTORE, nameVar)

                .label() // var targetType = MethodType.fromMethodDescriptorString(var2[2], Dispatcher.class.getClassLoader());
                ._var(ALOAD, stringsVar)
                ._int(2)
                .aaload()
                ._const(clazz.type())
                .method(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
                .method(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;")
                ._var(ASTORE, targetType)

                .label();

        // {v, s, vg, sg, vs, ss}
        var virtualLabel = new LabelNode();
        var staticLabel = new LabelNode();
        var virtualFieldLabel = new LabelNode();
        var virtualGetter = new LabelNode();
        var staticGetter = new LabelNode();
        var staticFieldLabel = new LabelNode();
        var dflt = new LabelNode();
        var exit = new LabelNode();

        builder
                ._var(ILOAD, typeVar)
                .lookupswitch(SwitchUtils.createLookup(dflt, Map.of(
                        (int) chars[0], virtualLabel,
                        (int) chars[1], staticLabel,
                        (int) chars[2], virtualFieldLabel,
                        (int) chars[3], staticFieldLabel,
                        (int) chars[4], virtualFieldLabel,
                        (int) chars[5], staticFieldLabel
                )))
                .label(virtualLabel)
                ._var(ALOAD, lookupParam)
                ._var(ALOAD, classVar)
                ._var(ALOAD, nameVar)
                ._var(ALOAD, targetType)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, handle)
                ._goto(exit)

                .label(staticLabel)
                ._var(ALOAD, lookupParam)
                ._var(ALOAD, classVar)
                ._var(ALOAD, nameVar)
                ._var(ALOAD, targetType)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, handle)
                ._goto(exit)

                .label(virtualFieldLabel)
                ._var(ALOAD, lookupParam)
                ._var(ALOAD, classVar)
                ._var(ALOAD, nameVar)
                ._var(ALOAD, targetType)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "returnType", "()Ljava/lang/Class;")
                ._var(ILOAD, typeVar)
                ._const(chars[2])
                .jump(IF_ICMPEQ, virtualGetter)

                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, handle)
                ._goto(exit)

                .label(virtualGetter)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, handle)
                ._goto(exit)

                .label(staticFieldLabel)
                ._var(ALOAD, lookupParam)
                ._var(ALOAD, classVar)
                ._var(ALOAD, nameVar)
                ._var(ALOAD, targetType)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "returnType", "()Ljava/lang/Class;")
                ._var(ILOAD, typeVar)
                ._const(chars[3])
                .jump(IF_ICMPEQ, staticGetter)

                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, handle)
                ._goto(exit)

                .label(staticGetter)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, handle)
                ._goto(exit)

                .label(dflt)
                ._null()
                .athrow()

                // asd
                .label(exit)
                ._var(ALOAD, handle)

                ._var(ALOAD, methodTypeParam)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "parameterCount", "()I")
                ._int(2)
                .isub()

                ._int(2)
                .anewarray("java/lang/Class")

                .dup()
                ._int(0)
                .field(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;")
                .aastore()

                .dup()
                ._int(1)
                .field(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;")
                .aastore()
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "dropArguments", "(Ljava/lang/invoke/MethodHandle;I[Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;")
                ._areturn();
    }
}

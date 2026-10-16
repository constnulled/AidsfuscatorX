package dev.lvstrng.aidsfuscator.classgen.impl.indy;

import dev.lvstrng.aidsfuscator.classgen.IClassGen;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import static org.objectweb.asm.Opcodes.*;

public class ConstantIndyClassGenerator implements IClassGen {
    public static final String BSM_STRING_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;II)Ljava/lang/invoke/CallSite;";
    public static final String BSM_CLASS_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;III)Ljava/lang/invoke/CallSite;";
    public static final String BSM_INT_DESC = PrimitiveIndyBootstrapGenerator.BSM_INT_DESC;
    public static final String BSM_LONG_DESC = PrimitiveIndyBootstrapGenerator.BSM_LONG_DESC;
    public static final String BSM_FLOAT_DESC = PrimitiveIndyBootstrapGenerator.BSM_FLOAT_DESC;
    public static final String BSM_DOUBLE_DESC = PrimitiveIndyBootstrapGenerator.BSM_DOUBLE_DESC;
    public static final String BSM_MATH_INT_DESC = MathIndyBootstrapGenerator.BSM_MATH_INT_DESC;
    public static final String BSM_MATH_LONG_DESC = MathIndyBootstrapGenerator.BSM_MATH_LONG_DESC;

    public JMethod bsmStringMethod;
    public JMethod bsmClassMethod;
    public JMethod bsmIntMethod;
    public JMethod bsmLongMethod;
    public JMethod bsmFloatMethod;
    public JMethod bsmDoubleMethod;
    public JMethod bsmMathIntMethod;
    public JMethod bsmMathLongMethod;

    @Override
    public JClass create(Context context) {
        var clazz = context.createClass("java/lang/Object", ACC_PUBLIC | ACC_SUPER);

        var init = clazz.createMethod(ACC_PUBLIC, "<init>", "()V");
        new InsnBuilder(init.insns())
                .label()
                ._var(ALOAD, 0)
                .method(INVOKESPECIAL, "java/lang/Object", "<init>", "()V")
                ._return();

        var bsmStringName = context.dictionary().newMethodName(clazz, BSM_STRING_DESC);
        createBsmString(clazz, bsmStringName);

        var bsmClassName = context.dictionary().newMethodName(clazz, BSM_CLASS_DESC);
        createBsmClass(clazz, bsmClassName);

        var bsmIntName = context.dictionary().newMethodName(clazz, BSM_INT_DESC);
        bsmIntMethod = PrimitiveIndyBootstrapGenerator.createBsmInt(clazz, bsmIntName);

        var bsmLongName = context.dictionary().newMethodName(clazz, BSM_LONG_DESC);
        bsmLongMethod = PrimitiveIndyBootstrapGenerator.createBsmLong(clazz, bsmLongName);

        var bsmFloatName = context.dictionary().newMethodName(clazz, BSM_FLOAT_DESC);
        bsmFloatMethod = PrimitiveIndyBootstrapGenerator.createBsmFloat(clazz, bsmFloatName);

        var bsmDoubleName = context.dictionary().newMethodName(clazz, BSM_DOUBLE_DESC);
        bsmDoubleMethod = PrimitiveIndyBootstrapGenerator.createBsmDouble(clazz, bsmDoubleName);

        var intMathNames = MathIndyBootstrapGenerator.createIntMathMethods(clazz, context);
        var bsmMathIntName = context.dictionary().newMethodName(clazz, BSM_MATH_INT_DESC);
        bsmMathIntMethod = MathIndyBootstrapGenerator.createBsmMathInt(clazz, bsmMathIntName, intMathNames);

        var longMathNames = MathIndyBootstrapGenerator.createLongMathMethods(clazz, context);
        var bsmMathLongName = context.dictionary().newMethodName(clazz, BSM_MATH_LONG_DESC);
        bsmMathLongMethod = MathIndyBootstrapGenerator.createBsmMathLong(clazz, bsmMathLongName, longMathNames);

        context.addArtificial(clazz);
        return clazz;
    }

    private void createBsmString(JClass clazz, String name) {
        bsmStringMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_STRING_DESC);
        var loop = new LabelNode();
        var exitLoop = new LabelNode();

        new InsnBuilder(bsmStringMethod.insns())
                .label()
                ._var(ALOAD, 3)
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, 6)
                ._int(0)
                ._var(ISTORE, 7)
                .label(loop)
                ._var(ILOAD, 7)
                ._var(ALOAD, 6)
                .arraylength()
                .jump(IF_ICMPGE, exitLoop)
                ._var(ALOAD, 6)
                ._var(ILOAD, 7)
                ._var(ALOAD, 6)
                ._var(ILOAD, 7)
                .caload()
                ._var(ILOAD, 4)
                .ixor()
                ._var(ILOAD, 5)
                ._var(ILOAD, 7)
                .iadd()
                .ixor()
                .i2c()
                .castore()
                .iinc(7, 1)
                ._goto(loop)
                .label(exitLoop)
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, 6)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                ._var(ASTORE, 8)
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                ._const(Type.getType("Ljava/lang/String;"))
                ._var(ALOAD, 8)
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn();
    }

    private void createBsmClass(JClass clazz, String name) {
        bsmClassMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_CLASS_DESC);
        var loop = new LabelNode();
        var exitLoop = new LabelNode();
        var loaderNull = new LabelNode();
        var classResolved = new LabelNode();
        var tryStart = new LabelNode();
        var tryEnd = new LabelNode();
        var catchLabel = new LabelNode();

        bsmClassMethod.core().tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchLabel, "java/lang/ClassNotFoundException"));

        new InsnBuilder(bsmClassMethod.insns())
                .label()
                ._var(ALOAD, 3)
                .method(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C")
                ._var(ASTORE, 7)
                ._int(0)
                ._var(ISTORE, 8)
                .label(loop)
                ._var(ILOAD, 8)
                ._var(ALOAD, 7)
                .arraylength()
                .jump(IF_ICMPGE, exitLoop)
                ._var(ALOAD, 7)
                ._var(ILOAD, 8)
                ._var(ALOAD, 7)
                ._var(ILOAD, 8)
                .caload()
                ._var(ILOAD, 4)
                .ixor()
                ._var(ILOAD, 5)
                ._var(ILOAD, 8)
                .iadd()
                .ixor()
                .i2c()
                .castore()
                .iinc(8, 1)
                ._goto(loop)
                .label(exitLoop)
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, 7)
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V")
                ._var(ASTORE, 9)
                ._var(ALOAD, 0)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;")
                .method(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
                ._var(ASTORE, 10)
                .label(tryStart)
                ._var(ALOAD, 10)
                .jump(IFNULL, loaderNull)
                ._var(ALOAD, 9)
                ._const(0)
                ._var(ALOAD, 10)
                .method(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")
                ._goto(classResolved)
                .label(loaderNull)
                ._var(ALOAD, 9)
                .method(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;")
                .label(classResolved)
                ._var(ASTORE, 11)
                .label(tryEnd)
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                ._const(Type.getType("Ljava/lang/Class;"))
                ._var(ALOAD, 11)
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn()
                .label(catchLabel)
                .type(NEW, "java/lang/NoClassDefFoundError")
                .dup()
                .method(INVOKESPECIAL, "java/lang/NoClassDefFoundError", "<init>", "()V")
                .athrow();
    }
}

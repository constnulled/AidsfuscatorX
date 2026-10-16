package dev.lvstrng.aidsfuscator.classgen.impl.indy;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import static org.objectweb.asm.Opcodes.*;

public class MathIndyBootstrapGenerator {
    public static final String BSM_MATH_INT_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;IIII)Ljava/lang/invoke/CallSite;";
    public static final String BSM_MATH_LONG_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;JIII)Ljava/lang/invoke/CallSite;";

    public static String[] createIntMathMethods(JClass clazz, Context context) {
        int[] opcodes = { IADD, ISUB, IMUL, IXOR, IOR, IAND, ISHL, ISHR, IUSHR };
        String[] names = new String[opcodes.length];

        for (int i = 0; i < opcodes.length; i++) {
            String name = context.dictionary().newMethodName(clazz, "(II)I");
            names[i] = name;
            var method = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, "(II)I");
            new InsnBuilder(method.insns())
                    .label()
                    ._var(ILOAD, 0)
                    ._var(ILOAD, 1)
                    .add(new InsnNode(opcodes[i]))
                    ._ireturn();
        }

        return names;
    }

    public static String[] createLongMathMethods(JClass clazz, Context context) {
        int[] opcodes = { LADD, LSUB, LMUL, LXOR, LOR, LAND };
        String[] names = new String[opcodes.length];

        for (int i = 0; i < opcodes.length; i++) {
            String name = context.dictionary().newMethodName(clazz, "(JJ)J");
            names[i] = name;
            var method = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, "(JJ)J");
            new InsnBuilder(method.insns())
                    .label()
                    ._var(LLOAD, 0)
                    ._var(LLOAD, 2)
                    .add(new InsnNode(opcodes[i]))
                    ._lreturn();
        }

        return names;
    }

    public static JMethod createBsmMathInt(JClass clazz, String name, String[] intMathNames) {
        var bsmMathIntMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_MATH_INT_DESC);
        var tryStart = new LabelNode();
        var tryEnd = new LabelNode();
        var catchLabel = new LabelNode();
        var doneLabel = new LabelNode();

        bsmMathIntMethod.core().tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchLabel, "java/lang/Exception"));

        var builder = new InsnBuilder(bsmMathIntMethod.insns())
                .label(tryStart)
                ._var(ILOAD, 3)
                ._var(ILOAD, 5)
                .isub()
                ._var(ILOAD, 4)
                .ixor()
                ._var(ISTORE, 7);

        var switchLabels = new LabelNode[intMathNames.length];
        for (int i = 0; i < intMathNames.length; i++) {
            switchLabels[i] = new LabelNode();
        }
        var dfltLabel = new LabelNode();

        builder._var(ILOAD, 7)
                .add(new TableSwitchInsnNode(0, intMathNames.length - 1, dfltLabel, switchLabels));

        for (int i = 0; i < intMathNames.length; i++) {
            builder.label(switchLabels[i])
                    ._var(ALOAD, 0)
                    ._const(Type.getObjectType(clazz.name()))
                    ._const(intMathNames[i])
                    ._var(ALOAD, 2)
                    .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic",
                            "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                    ._var(ASTORE, 8)
                    ._goto(doneLabel);
        }

        builder.label(dfltLabel)
                ._var(ALOAD, 0)
                ._const(Type.getObjectType(clazz.name()))
                ._const(intMathNames[0])
                ._var(ALOAD, 2)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic",
                            "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, 8);

        builder.label(doneLabel)
                .label(tryEnd)
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                ._var(ALOAD, 8)
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn()

                .label(catchLabel)
                .type(NEW, "java/lang/BootstrapMethodError")
                .dup_x1()
                .swap()
                .method(INVOKESPECIAL, "java/lang/BootstrapMethodError", "<init>", "(Ljava/lang/Throwable;)V")
                .athrow();

        return bsmMathIntMethod;
    }

    public static JMethod createBsmMathLong(JClass clazz, String name, String[] longMathNames) {
        var bsmMathLongMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_MATH_LONG_DESC);
        var tryStart = new LabelNode();
        var tryEnd = new LabelNode();
        var catchLabel = new LabelNode();
        var doneLabel = new LabelNode();

        bsmMathLongMethod.core().tryCatchBlocks.add(new TryCatchBlockNode(tryStart, tryEnd, catchLabel, "java/lang/Exception"));

        var builder = new InsnBuilder(bsmMathLongMethod.insns())
                .label(tryStart)
                ._var(LLOAD, 3)
                ._var(ILOAD, 6)
                .i2l()
                .lsub()
                ._var(ILOAD, 5)
                .i2l()
                .lxor()
                .l2i()
                ._var(ISTORE, 8);

        var switchLabels = new LabelNode[longMathNames.length];
        for (int i = 0; i < longMathNames.length; i++) {
            switchLabels[i] = new LabelNode();
        }
        var dfltLabel = new LabelNode();

        builder._var(ILOAD, 8)
                .add(new TableSwitchInsnNode(0, longMathNames.length - 1, dfltLabel, switchLabels));

        for (int i = 0; i < longMathNames.length; i++) {
            builder.label(switchLabels[i])
                    ._var(ALOAD, 0)
                    ._const(Type.getObjectType(clazz.name()))
                    ._const(longMathNames[i])
                    ._var(ALOAD, 2)
                    .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic",
                            "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                    ._var(ASTORE, 9)
                    ._goto(doneLabel);
        }

        builder.label(dfltLabel)
                ._var(ALOAD, 0)
                ._const(Type.getObjectType(clazz.name()))
                ._const(longMathNames[0])
                ._var(ALOAD, 2)
                .method(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic",
                            "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
                ._var(ASTORE, 9);

        builder.label(doneLabel)
                .label(tryEnd)
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                ._var(ALOAD, 9)
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn()

                .label(catchLabel)
                .type(NEW, "java/lang/BootstrapMethodError")
                .dup_x1()
                .swap()
                .method(INVOKESPECIAL, "java/lang/BootstrapMethodError", "<init>", "(Ljava/lang/Throwable;)V")
                .athrow();

        return bsmMathLongMethod;
    }
}

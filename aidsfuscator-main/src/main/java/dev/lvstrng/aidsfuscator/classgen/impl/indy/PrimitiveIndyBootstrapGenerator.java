package dev.lvstrng.aidsfuscator.classgen.impl.indy;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;

import static org.objectweb.asm.Opcodes.*;

public class PrimitiveIndyBootstrapGenerator {
    public static final String BSM_INT_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;III)Ljava/lang/invoke/CallSite;";
    public static final String BSM_LONG_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;JJJ)Ljava/lang/invoke/CallSite;";
    public static final String BSM_FLOAT_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;FII)Ljava/lang/invoke/CallSite;";
    public static final String BSM_DOUBLE_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;DII)Ljava/lang/invoke/CallSite;";

    public static JMethod createBsmInt(JClass clazz, String name) {
        var method = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_INT_DESC);
        new InsnBuilder(method.insns())
                .label()
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                .field(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;")
                ._var(ILOAD, 3)
                ._var(ILOAD, 5)
                .iadd()
                ._var(ILOAD, 4)
                .ixor()
                .method(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn();
        return method;
    }

    public static JMethod createBsmLong(JClass clazz, String name) {
        var method = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_LONG_DESC);
        new InsnBuilder(method.insns())
                .label()
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                .field(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;")
                ._var(LLOAD, 3)
                ._var(LLOAD, 7)
                .ladd()
                ._var(LLOAD, 5)
                .lxor()
                .method(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;")
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn();
        return method;
    }

    public static JMethod createBsmFloat(JClass clazz, String name) {
        var method = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_FLOAT_DESC);
        new InsnBuilder(method.insns())
                .label()
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                .field(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;")
                ._var(FLOAD, 3)
                .method(INVOKESTATIC, "java/lang/Float", "floatToRawIntBits", "(F)I")
                ._var(ILOAD, 5)
                .iadd()
                ._var(ILOAD, 4)
                .ixor()
                .method(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F")
                .method(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;")
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn();
        return method;
    }

    public static JMethod createBsmDouble(JClass clazz, String name) {
        var method = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_DOUBLE_DESC);
        new InsnBuilder(method.insns())
                .label()
                .type(NEW, "java/lang/invoke/ConstantCallSite")
                .dup()
                .field(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;")
                ._var(DLOAD, 3)
                .method(INVOKESTATIC, "java/lang/Double", "doubleToRawLongBits", "(D)J")
                ._var(ILOAD, 6)
                .i2l()
                .ladd()
                ._var(ILOAD, 5)
                .i2l()
                .lxor()
                .method(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D")
                .method(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;")
                .method(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;")
                .method(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V")
                ._areturn();
        return method;
    }
}

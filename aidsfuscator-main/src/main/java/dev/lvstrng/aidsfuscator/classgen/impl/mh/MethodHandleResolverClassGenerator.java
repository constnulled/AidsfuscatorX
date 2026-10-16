package dev.lvstrng.aidsfuscator.classgen.impl.mh;

import dev.lvstrng.aidsfuscator.classgen.IClassGen;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public class MethodHandleResolverClassGenerator implements IClassGen {

    public static final String BSM_DESC =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
            "IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/invoke/CallSite;";

    public JMethod bsmMethod;

    private String className;
    private String decryptName;
    private String fieldTypeName;

    @Override
    public JClass create(Context context) {
        var clazz = context.createClass("java/lang/Object", ACC_PUBLIC | ACC_SUPER);
        className = clazz.core().name;

        var init = clazz.createMethod(ACC_PUBLIC, "<init>", "()V");
        var initInsns = init.insns();
        initInsns.add(new LabelNode());
        initInsns.add(new VarInsnNode(ALOAD, 0));
        initInsns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        initInsns.add(new InsnNode(RETURN));

        decryptName = context.dictionary().newMethodName(clazz, "(Ljava/lang/String;I)Ljava/lang/String;");
        createDecrypt(clazz);

        fieldTypeName = context.dictionary().newMethodName(clazz, "(Ljava/lang/String;)Ljava/lang/Class;");
        createFieldType(clazz);

        var bsmName = context.dictionary().newMethodName(clazz, BSM_DESC);
        createBsm(clazz, bsmName);

        context.addArtificial(clazz);
        return clazz;
    }

    public String className() {
        return className;
    }

    private void createDecrypt(JClass clazz) {
        var method = clazz.createMethod(ACC_STATIC, decryptName, "(Ljava/lang/String;I)Ljava/lang/String;");
        var loop = new LabelNode();
        var exit = new LabelNode();
        var insns = method.insns();

        insns.add(new LabelNode());
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
        insns.add(new VarInsnNode(ASTORE, 2));
        insns.add(new InsnNode(ICONST_0));
        insns.add(new VarInsnNode(ISTORE, 3));
        insns.add(loop);
        insns.add(new VarInsnNode(ILOAD, 3));
        insns.add(new VarInsnNode(ALOAD, 2));
        insns.add(new InsnNode(ARRAYLENGTH));
        insns.add(new JumpInsnNode(IF_ICMPGE, exit));
        insns.add(new VarInsnNode(ALOAD, 2));
        insns.add(new VarInsnNode(ILOAD, 3));
        insns.add(new VarInsnNode(ALOAD, 2));
        insns.add(new VarInsnNode(ILOAD, 3));
        insns.add(new InsnNode(CALOAD));
        insns.add(new VarInsnNode(ILOAD, 1));
        insns.add(new InsnNode(IXOR));
        insns.add(new VarInsnNode(ILOAD, 3));
        insns.add(new InsnNode(IXOR));
        insns.add(new InsnNode(I2C));
        insns.add(new InsnNode(CASTORE));
        insns.add(new IincInsnNode(3, 1));
        insns.add(new JumpInsnNode(GOTO, loop));
        insns.add(exit);
        insns.add(new TypeInsnNode(NEW, "java/lang/String"));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ALOAD, 2));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        insns.add(new InsnNode(ARETURN));
    }

    private void createFieldType(JClass clazz) {
        var method = clazz.createMethod(ACC_STATIC, fieldTypeName, "(Ljava/lang/String;)Ljava/lang/Class;");
        var insns = method.insns();

        insns.add(new LabelNode());
        insns.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
        insns.add(new InsnNode(DUP));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        insns.add(new LdcInsnNode("()"));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        insns.add(new InsnNode(ACONST_NULL));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "returnType", "()Ljava/lang/Class;", false));
        insns.add(new InsnNode(ARETURN));
    }

    private void createBsm(JClass clazz, String name) {
        bsmMethod = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, name, BSM_DESC);
        var insns = bsmMethod.insns();

        LabelNode[] cases = new LabelNode[9];
        for (int i = 0; i < 9; i++) cases[i] = new LabelNode();
        var defaultLabel = new LabelNode();
        var endSwitch = new LabelNode();

        insns.add(new LabelNode());

        insns.add(new VarInsnNode(ILOAD, 3));
        insns.add(new VarInsnNode(ILOAD, 4));
        insns.add(new InsnNode(IXOR));
        insns.add(new VarInsnNode(ISTORE, 8));

        insns.add(new VarInsnNode(ALOAD, 5));
        insns.add(new VarInsnNode(ILOAD, 4));
        insns.add(new MethodInsnNode(INVOKESTATIC, className, decryptName, "(Ljava/lang/String;I)Ljava/lang/String;", false));
        insns.add(new VarInsnNode(ASTORE, 9));

        insns.add(new VarInsnNode(ALOAD, 9));
        insns.add(new IntInsnNode(BIPUSH, '/'));
        insns.add(new IntInsnNode(BIPUSH, '.'));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;", false));
        insns.add(new VarInsnNode(ASTORE, 10));

        insns.add(new VarInsnNode(ALOAD, 6));
        insns.add(new VarInsnNode(ILOAD, 4));
        insns.add(new MethodInsnNode(INVOKESTATIC, className, decryptName, "(Ljava/lang/String;I)Ljava/lang/String;", false));
        insns.add(new VarInsnNode(ASTORE, 11));

        insns.add(new VarInsnNode(ALOAD, 7));
        insns.add(new VarInsnNode(ILOAD, 4));
        insns.add(new MethodInsnNode(INVOKESTATIC, className, decryptName, "(Ljava/lang/String;I)Ljava/lang/String;", false));
        insns.add(new VarInsnNode(ASTORE, 12));

        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "lookupClass", "()Ljava/lang/Class;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
        insns.add(new VarInsnNode(ASTORE, 13));

        insns.add(new VarInsnNode(ALOAD, 10));
        insns.add(new InsnNode(ICONST_0));
        insns.add(new VarInsnNode(ALOAD, 13));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;", false));
        insns.add(new VarInsnNode(ASTORE, 14));

        insns.add(new VarInsnNode(ILOAD, 8));
        insns.add(new TableSwitchInsnNode(1, 9, defaultLabel, cases));

        insns.add(cases[0]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 11));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new MethodInsnNode(INVOKESTATIC, className, fieldTypeName, "(Ljava/lang/String;)Ljava/lang/Class;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(cases[1]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 11));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new MethodInsnNode(INVOKESTATIC, className, fieldTypeName, "(Ljava/lang/String;)Ljava/lang/Class;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(cases[2]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 11));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new MethodInsnNode(INVOKESTATIC, className, fieldTypeName, "(Ljava/lang/String;)Ljava/lang/Class;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(cases[3]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 11));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new MethodInsnNode(INVOKESTATIC, className, fieldTypeName, "(Ljava/lang/String;)Ljava/lang/Class;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(cases[4]);
        insns.add(cases[5]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 11));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new VarInsnNode(ALOAD, 13));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(cases[6]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 11));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new VarInsnNode(ALOAD, 13));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSpecial", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(cases[7]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new VarInsnNode(ALOAD, 13));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findConstructor", "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(cases[8]);
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ALOAD, 14));
        insns.add(new VarInsnNode(ALOAD, 11));
        insns.add(new VarInsnNode(ALOAD, 12));
        insns.add(new VarInsnNode(ALOAD, 13));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new VarInsnNode(ASTORE, 15));
        insns.add(new JumpInsnNode(GOTO, endSwitch));

        insns.add(defaultLabel);
        insns.add(new TypeInsnNode(NEW, "java/lang/RuntimeException"));
        insns.add(new InsnNode(DUP));
        insns.add(new LdcInsnNode("Unknown handle kind"));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false));
        insns.add(new InsnNode(ATHROW));

        insns.add(endSwitch);
        insns.add(new TypeInsnNode(NEW, "java/lang/invoke/ConstantCallSite"));
        insns.add(new InsnNode(DUP));
        insns.add(new LdcInsnNode(Type.getType("Ljava/lang/invoke/MethodHandle;")));
        insns.add(new VarInsnNode(ALOAD, 15));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false));
        insns.add(new InsnNode(ARETURN));
    }
}
package dev.lvstrng.aidsfuscator.transform.impl.data.strings.decryptors;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringDecryptor;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Map;

public class IndyStringDecryptor implements IStringDecryptor {

    public static final String BSM_DESC =
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
            "Ljava/lang/String;II)Ljava/lang/invoke/CallSite;";

    private String decryptorName;
    private String bsmName;
    private String ownerName;
    private final int xorKey;
    private final int idxXor;

    public IndyStringDecryptor() {
        this.xorKey = random.nextInt();
        this.idxXor = random.nextInt();
    }

    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName) {
        ownerName = clazz.name();
        bsmName   = context.dictionary().newMethodName(clazz, BSM_DESC);
        generateBsm(context, clazz);
    }

    private void generateBsm(Context context, JClass clazz) {
        var bsm        = clazz.createMethod(ACC_PUBLIC | ACC_STATIC, bsmName, BSM_DESC);
        var charArrVar = bsm.allocVar();
        var iVar       = bsm.allocVar();
        var strVar     = bsm.allocVar();

        var loop = new LabelNode();
        var exit = new LabelNode();

        var insns = bsm.insns();
        insns.add(new LabelNode());
        insns.add(new VarInsnNode(ALOAD, 3));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
        insns.add(new VarInsnNode(ASTORE, charArrVar));

        insns.add(new LabelNode());
        insns.add(new InsnNode(ICONST_0));
        insns.add(new VarInsnNode(ISTORE, iVar));

        insns.add(loop);
        insns.add(new VarInsnNode(ILOAD, iVar));
        insns.add(new VarInsnNode(ALOAD, charArrVar));
        insns.add(new InsnNode(ARRAYLENGTH));
        insns.add(new JumpInsnNode(IF_ICMPGE, exit));

        insns.add(new VarInsnNode(ALOAD, charArrVar));
        insns.add(new VarInsnNode(ILOAD, iVar));
        insns.add(new VarInsnNode(ALOAD, charArrVar));
        insns.add(new VarInsnNode(ILOAD, iVar));
        insns.add(new InsnNode(CALOAD));
        insns.add(new VarInsnNode(ILOAD, 4));
        insns.add(new InsnNode(IXOR));
        insns.add(context.properties().add(ASMUtils.pushInt(xorKey), Property.IGNORE_INTEGER));
        insns.add(new InsnNode(IXOR));
        insns.add(new VarInsnNode(ILOAD, iVar));
        insns.add(new InsnNode(IXOR));
        insns.add(new InsnNode(I2C));
        insns.add(new InsnNode(CASTORE));

        insns.add(new IincInsnNode(iVar, 1));
        insns.add(new JumpInsnNode(GOTO, loop));

        insns.add(exit);
        insns.add(new TypeInsnNode(NEW, "java/lang/String"));
        insns.add(new InsnNode(DUP));
        insns.add(new VarInsnNode(ALOAD, charArrVar));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        insns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;", false));
        insns.add(new VarInsnNode(ASTORE, strVar));

        insns.add(new LabelNode());
        insns.add(new TypeInsnNode(NEW, "java/lang/invoke/ConstantCallSite"));
        insns.add(new InsnNode(DUP));
        insns.add(new LdcInsnNode(Type.getType("Ljava/lang/String;")));
        insns.add(new VarInsnNode(ALOAD, strVar));
        insns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant",
                "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>",
                "(Ljava/lang/invoke/MethodHandle;)V", false));
        insns.add(new InsnNode(ARETURN));

        bsm.properties().add(Property.STRING_DECRYPTOR);
        clazz.reinsertRandomly(random, bsm);
    }

    @Override
    public InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<String> strings, String str) {
        var idx       = strings.size();
        var key       = random.nextInt();
        var encrypted = encrypt(str, key);

        strings.add(encrypted);

        var bsmHandle = new Handle(H_INVOKESTATIC, ownerName, bsmName, BSM_DESC, false);
        var indy      = new InvokeDynamicInsnNode("s", "()Ljava/lang/String;", bsmHandle, encrypted, key, idx ^ idxXor);
        context.properties().add(indy, Property.IGNORE_REF_OBFUSCATION);

        var list = new InsnList();
        list.add(indy);
        return list;
    }

    private String encrypt(String s, int key) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] ^= (char) (key ^ xorKey ^ i);
        }
        return new String(chars);
    }

    @Override
    public void setName(String name) {
        this.decryptorName = name;
    }

    @Override
    public String getDescriptor() {
        return "(II)Ljava/lang/String;";
    }

    @Override
    public boolean usesSharedArray() {
        return false;
    }
}

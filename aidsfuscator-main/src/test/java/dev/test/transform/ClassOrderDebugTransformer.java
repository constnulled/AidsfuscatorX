package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class ClassOrderDebugTransformer extends Transformer {
    public ClassOrderDebugTransformer() {
        super("Class Order Debug", "classOrderDebug");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var list = new InsnList();
                list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                list.add(new LdcInsnNode(clazz.name()));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));

                if(clazz.hasSalt()) {
                    list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                    list.add(clazz.salt().load());
                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V"));

                    System.out.println(clazz.salt().value());
                }

                list.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "()V"));

                method.insertSafe(list);
            }
        }
    }
}

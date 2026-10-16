package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassInitOrderClassGen implements Opcodes {
    private final Context context;
    public JField field;
    public JMethod get, set;

    public ClassInitOrderClassGen(Context context) {
        this.context = context;
    }

    public JClass create() {
        var _node = new ClassNode();
        _node.name = context.dictionary().newClassName();
        _node.access = ACC_PUBLIC;
        _node.version = 61;
        _node.superName = "java/lang/Object";

        var clazz = new JClass(_node);
        field = generateField(context, clazz);
        get = generateGet(context, clazz, field);
        set = generateSet(context, clazz, field);

        var clinit = clazz.findOrCreateClinit();
        var builder = new InsnBuilder()
                .type(NEW, "java/util/HashMap")
                .dup()
                .method(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V")
                .field(PUTSTATIC, clazz.name(), field.name(), field.desc());

        clinit.insertSafe(builder.result());
        return clazz;
    }

    private JField generateField(Context context, JClass clazz) {
        var field = new FieldNode(ACC_STATIC, context.dictionary().newFieldName(clazz, "Ljava/util/Map;"), "Ljava/util/Map;", null, null);
        return clazz.add(field);
    }

    private JMethod generateGet(Context context, JClass clazz, JField field) {
        var desc = "(Ljava/lang/Object;)I";
        var name = context.dictionary().newMethodName(clazz, desc);
        var get = clazz.add(new MethodNode(ACC_PUBLIC | ACC_STATIC, name, desc, null, null));
        new InsnBuilder(get.insns())
                .field(GETSTATIC, clazz.name(), field.name(), field.desc())
                ._var(ALOAD, 0)
                .method(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true)
                .type(CHECKCAST, "java/lang/Integer")
                .method(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I")
                ._ireturn();

        return get;
    }

    private JMethod generateSet(Context context, JClass clazz, JField field) {
        var desc = "(Ljava/lang/Object;I)V";
        var name = context.dictionary().newMethodName(clazz, desc);
        var set = clazz.add(new MethodNode(ACC_PUBLIC | ACC_STATIC, name, desc, null, null));
        new InsnBuilder(set.insns())
                .field(GETSTATIC, clazz.name(), field.name(), field.desc())
                ._var(ALOAD, 0)
                ._var(ILOAD, 1)
                .method(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
                .method(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true)
                .pop()
                ._return();

        return set;
    }
}

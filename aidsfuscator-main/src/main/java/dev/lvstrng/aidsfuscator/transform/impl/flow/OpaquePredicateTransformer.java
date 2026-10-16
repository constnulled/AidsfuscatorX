package dev.lvstrng.aidsfuscator.transform.impl.flow;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

public class OpaquePredicateTransformer extends Transformer {
    private final Setting<Integer> density = setting("density", 67);
    private final Setting<Boolean> fieldPredicates = setting("fieldPredicates", true);

    private record Site(JMethod method, AbstractInsnNode target) {}

    public OpaquePredicateTransformer() {
        super("Opaque Predicates", "OpaquePredicatesTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.OPAQUE_PREDICATES.excluded(clazz))
                continue;

            var sites = collectSites(clazz);
            if (sites.isEmpty())
                continue;

            var field = fieldPredicates.value() && !clazz.isInterface()
                    ? createStateField(context, clazz)
                    : null;

            for (var site : sites) {
                if (random.nextInt(100) >= density.value())
                    continue;

                var list = buildPredicate(context, site.method(), clazz, field);
                site.method().insns().insertBefore(site.target(), list);
                markChange();
            }
        }
    }

    private ArrayList<Site> collectSites(JClass clazz) {
        var sites = new ArrayList<Site>();

        for (var method : clazz.methods()) {
            if (cantEditMethod(clazz, method))
                continue;
            if (Exclusions.OPAQUE_PREDICATES.excluded(method))
                continue;
            if (method.insns().size() < 4)
                continue;

            for (var insn : method.insns()) {
                if (isTarget(insn))
                    sites.add(new Site(method, insn));
            }
        }

        return sites;
    }

    private boolean isTarget(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= IRETURN && op <= RETURN) || op == ATHROW || insn instanceof MethodInsnNode;
    }

    private JField createStateField(Context ctx, JClass clazz) {
        var field = clazz.createField(
                ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                ctx.dictionary().newFieldName(clazz, "I"),
                "I"
        );

        var init = new InsnList();
        init.add(ASMUtils.pushInt(random.nextInt() | 1));
        init.add(new FieldInsnNode(PUTSTATIC, clazz.name(), field.name(), "I"));

        clazz.findOrCreateClinit().insns().insert(init);
        return field;
    }

    private InsnList buildPredicate(Context ctx, JMethod method, JClass clazz, JField field) {
        var list = new InsnList();
        var jump = addCondition(ctx, method, clazz, field, list);

        addDeadPayload(list);
        list.add(jump.label);

        ctx.properties().add(jump, Property.IGNORE_SWITCH_MUTATION);
        return list;
    }

    private JumpInsnNode addCondition(Context ctx, JMethod method, JClass clazz, JField field, InsnList list) {
        var real = new LabelNode();
        int choice = random.nextInt(field == null ? 5 : 6);

        JumpInsnNode jump;
        switch (choice) {
            case 0 -> {
                int n = random.nextInt(500) + 2;
                list.add(method.protectedIntPush(ctx, n));
                list.add(method.protectedIntPush(ctx, n));
                list.add(method.protectedIntPush(ctx, 1));
                list.add(new InsnNode(IADD));
                list.add(new InsnNode(IMUL));
                list.add(method.protectedIntPush(ctx, 2));
                list.add(new InsnNode(IREM));
                jump = new JumpInsnNode(IFEQ, real);
            }
            case 1 -> {
                int n = random.nextInt(Short.MAX_VALUE - 2) + 2;
                list.add(method.protectedIntPush(ctx, n));
                list.add(method.protectedIntPush(ctx, 1));
                list.add(new InsnNode(IOR));
                jump = new JumpInsnNode(IFNE, real);
            }
            case 2 -> {
                int a = random.nextInt(1000) + 1;
                int b = random.nextInt(1000) + 1;
                list.add(method.protectedIntPush(ctx, a * a));
                list.add(method.protectedIntPush(ctx, b * b));
                list.add(new InsnNode(IADD));
                jump = new JumpInsnNode(IFGE, real);
            }
            case 3 -> {
                int a = random.nextInt();
                int b = random.nextInt();
                list.add(method.protectedIntPush(ctx, a));
                list.add(method.protectedIntPush(ctx, b));
                list.add(new InsnNode(IXOR));
                list.add(method.protectedIntPush(ctx, b));
                list.add(new InsnNode(IXOR));
                list.add(method.protectedIntPush(ctx, a));
                jump = new JumpInsnNode(IF_ICMPEQ, real);
            }
            case 4 -> {
                int x = random.nextInt();
                list.add(method.protectedIntPush(ctx, x));
                list.add(new InsnNode(DUP));
                list.add(new InsnNode(IMUL));
                list.add(method.protectedIntPush(ctx, 2));
                list.add(new InsnNode(IAND));
                jump = new JumpInsnNode(IFEQ, real);
            }
            default -> {
                int k = random.nextInt(1 << 20) * 2 + 1;
                list.add(new FieldInsnNode(GETSTATIC, clazz.name(), field.name(), "I"));
                list.add(method.protectedIntPush(ctx, k));
                list.add(new InsnNode(IMUL));
                list.add(new InsnNode(DUP));
                list.add(new FieldInsnNode(PUTSTATIC, clazz.name(), field.name(), "I"));
                list.add(new InsnNode(DUP));
                list.add(new InsnNode(IMUL));
                list.add(method.protectedIntPush(ctx, 7));
                list.add(new InsnNode(IAND));
                list.add(method.protectedIntPush(ctx, 1));
                jump = new JumpInsnNode(IF_ICMPEQ, real);
            }
        }

        list.add(jump);
        return jump;
    }

    private void addDeadPayload(InsnList list) {
        if (random.nextBoolean()) {
            list.add(new InsnNode(ACONST_NULL));
            list.add(new InsnNode(ATHROW));
        } else {
            list.add(new TypeInsnNode(NEW, "java/lang/IllegalStateException"));
            list.add(new InsnNode(DUP));
            list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false));
            list.add(new InsnNode(ATHROW));
        }
    }
}

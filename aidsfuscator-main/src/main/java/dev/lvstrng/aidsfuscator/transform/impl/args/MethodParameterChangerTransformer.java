package dev.lvstrng.aidsfuscator.transform.impl.args;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.analysis.ref.nodes.MethodReference;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class MethodParameterChangerTransformer extends Transformer {

    public MethodParameterChangerTransformer() {
        super("Change Method Parameters", "MethodParameterChangerTransformer");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var methodKeys = new HashMap<JMethod, int[]>();
        var processedHierarchy = new HashSet<JMethod>();

        for (var clazz : context.classes()) {
            for (var method : clazz.methods()) {
                if (processedHierarchy.contains(method)) continue;
                if (skipMethod(graph, context, clazz, method)) continue;

                int count = random.nextInt(2) + 1;
                int[] keys = new int[count];
                for (int i = 0; i < count; i++) {
                    keys[i] = random.nextInt();
                }

                var hierarchy = new HashSet<>(method.tree());
                hierarchy.add(method);
                processedHierarchy.addAll(hierarchy);

                for (var hm : hierarchy) {
                    int[] slots = new int[count];
                    for (int i = 0; i < count; i++) {
                        slots[i] = hm.allocParameter(Type.INT_TYPE);
                    }
                    methodKeys.put(hm, keys);

                    if (!hm.isAbstract() && !hm.isNative()) {
                        insertOpaqueGuard(hm, keys, slots);
                    }
                }

                markChange();
            }
        }

        for (var entry : methodKeys.entrySet()) {
            var method = entry.getKey();
            var keys = entry.getValue();

            for (var ref : graph.refs(method)) {
                if (!(ref.insn() instanceof MethodInsnNode insn)) continue;

                var inject = new InsnList();
                for (int key : keys) {
                    inject.add(ASMUtils.pushInt(key));
                }
                ref.caller().insns().insertBefore(insn, inject);
                insn.desc = method.desc();
            }
        }
    }

    private void insertOpaqueGuard(JMethod method, int[] keys, int[] slots) {
        var list = new InsnList();

        for (int i = 0; i < keys.length; i++) {
            var validLabel = new LabelNode();
            list.add(new VarInsnNode(ILOAD, slots[i]));
            list.add(ASMUtils.pushInt(keys[i]));
            list.add(new JumpInsnNode(IF_ICMPEQ, validLabel));
            list.add(new InsnNode(ACONST_NULL));
            list.add(new InsnNode(ATHROW));
            list.add(validLabel);
        }

        method.insertSafe(list);
    }

    private boolean skipMethod(ReferenceGraph graph, Context context, JClass clazz, JMethod method) {
        if (method.owner().isLibMethod(method)) return true;

        String newDesc = method.desc().replace(")", "I)");
        boolean wouldCollide = clazz.methods().stream()
                .filter(m -> m != method)
                .filter(m -> m.name().equals(method.name()))
                .anyMatch(m -> m.desc().equals(newDesc));
        if (wouldCollide) return true;

        for (var member : impactedClasses(context, clazz, method)) {
            var m = member.findMethod(method.name(), method.desc()).orElse(method);
            if (Exclusions.PARAMETER_CHANGE.excluded(member)) return true;
            if (Exclusions.PARAMETER_CHANGE.excluded(member, m)) return true;
            if (cantEditMethod(member, m)) return true;
            if (graph.refs(m).stream().anyMatch(MethodReference::cantEdit)) return true;
        }

        return false;
    }

    private Set<JClass> impactedClasses(Context context, JClass clazz, JMethod method) {
        var classes = new HashSet<>(clazz.children());
        classes.add(clazz);
        for (var parent : clazz.tree()) {
            if (!parent.hasMethodInTree(context, method)) continue;
            classes.add(parent);
            classes.addAll(parent.children());
        }
        return classes;
    }
}

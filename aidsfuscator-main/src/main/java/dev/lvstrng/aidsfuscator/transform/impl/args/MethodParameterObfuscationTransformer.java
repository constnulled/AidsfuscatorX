package dev.lvstrng.aidsfuscator.transform.impl.args;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.analysis.ref.nodes.MethodReference;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.Set;

public class MethodParameterObfuscationTransformer extends Transformer {

    public MethodParameterObfuscationTransformer() {
        super("Obfuscate Method Parameters", "MethodParameterObfuscationTransformer");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var obfuscatedMethods = new HashSet<JMethod>();

        for (var clazz : context.classes()) {
            collectEligible(context, graph, clazz, obfuscatedMethods);
        }

        var processedInsns = new HashSet<AbstractInsnNode>();
        for (var method : obfuscatedMethods) {
            var args = method.args();

            if (!method.isAbstract() && !method.isNative()) {
                unpackArgs(context, method, args);
            }

            method.core().desc = "([Ljava/lang/Object;)" + method.returnType();

            if (method.core().localVariables != null) {
                method.core().localVariables.clear();
            }

            for (var ref : graph.refs(method)) {
                if (!(ref.insn() instanceof MethodInsnNode insn)) continue;
                if (!processedInsns.add(insn)) continue;

                var list = new InsnBuilder()
                        ._int(args.length).addProps(context, Property.IGNORE_INTEGER)
                        .anewarray("java/lang/Object");

                for (int i = args.length - 1; i >= 0; i--) {
                    var arg = args[i];
                    if (arg.getSize() == 2) {
                        list.dup_x2().dup_x2().pop();
                    } else {
                        list.dup_x1().swap();
                    }
                    ASMUtils.box(list.result(), arg);
                    list._int(i).addProps(context, Property.IGNORE_INTEGER).swap().aastore();
                }

                ref.caller().insns().insertBefore(insn, list.result());
                insn.desc = method.desc();
            }

            markChange();
        }
    }

    private void collectEligible(Context context, ReferenceGraph graph, JClass clazz, Set<JMethod> obfuscatedMethods) {
        var toSkip = new HashSet<JMethod>();

        for (var method : clazz.methods()) {
            if (skipMethod(graph, context, clazz, method)) {
                toSkip.addAll(method.tree());
                toSkip.add(method);
            }
        }

        for (var method : clazz.methods()) {
            if (toSkip.contains(method)) continue;

            boolean hasDuplicate = clazz.methods().stream()
                    .filter(e -> e != method)
                    .filter(e -> e.name().equals(method.name()))
                    .anyMatch(e -> e.desc().equals("([Ljava/lang/Object;)" + method.returnType()));
            if (hasDuplicate) continue;

            boolean alreadyMapped = obfuscatedMethods.stream()
                    .filter(e -> e != method)
                    .filter(e -> e.owner().equals(method.owner()) || e.owner().tree().contains(method.owner()))
                    .filter(e -> e.name().equals(method.name()))
                    .anyMatch(e -> e.returnType().equals(method.returnType()));
            if (alreadyMapped) continue;

            method.tree().forEach(e -> e.removeAccessFlags(ACC_VARARGS));
            method.removeAccessFlags(ACC_VARARGS);
            obfuscatedMethods.addAll(method.tree());
            obfuscatedMethods.add(method);
        }
    }

    private boolean skipMethod(ReferenceGraph graph, Context context, JClass clazz, JMethod method) {
        if (method.args().length == 0) return true;
        if (method.owner().isLibMethod(method)) return true;

        for (var member : impactedClasses(context, clazz, method)) {
            var m = member.findMethod(method.name(), method.desc()).orElse(method);
            if (Exclusions.PARAMETER_OBFUSCATE.excluded(member)) return true;
            if (Exclusions.PARAMETER_OBFUSCATE.excluded(member, m)) return true;
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

    private void unpackArgs(Context context, JMethod method, Type[] args) {
        int argIndex = method.isVirtual() ? 1 : 0;

        for (var insn : method.insns()) {
            switch (insn) {
                case VarInsnNode v -> { if (v.var >= argIndex) v.var++; }
                case IincInsnNode v -> { if (v.var >= argIndex) v.var++; }
                default -> {}
            }
        }

        var list = new InsnList();
        int varIndex = argIndex + 1;
        list.add(new VarInsnNode(ALOAD, argIndex));

        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            list.add(new InsnNode(DUP));
            list.add(context.properties().add(ASMUtils.pushInt(i), Property.IGNORE_INTEGER));
            list.add(new InsnNode(AALOAD));
            ASMUtils.unbox(list, arg);
            list.add(new VarInsnNode(arg.getOpcode(ISTORE), varIndex));
            varIndex += arg.getSize();
        }

        list.add(new InsnNode(POP));
        method.insertSafe(list);
        method.allocVar();

        if (method.hasSalt()) {
            method.salt().updateVar(method.salt().local() + 1);
        }
    }
}

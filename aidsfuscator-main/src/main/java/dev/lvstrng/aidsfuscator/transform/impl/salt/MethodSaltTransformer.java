package dev.lvstrng.aidsfuscator.transform.impl.salt;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.analysis.ref.nodes.MethodReference;
import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.salt.ISalt;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MethodSaltTransformer extends Transformer {
    public MethodSaltTransformer() {
        super("Method Salting", "MethodSaltingTransformer");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var saltedMethods = new HashSet<JMethod>();

        var danger = new HashSet<JMethod>();
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var scope = collisionScope(context, method);
                if(skipMethodAndTree(graph, method, scope)) {
                    danger.addAll(methodTreeClosure(method));
                }
            }
        }

        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                if(danger.contains(method) || collidesWithDanger(method, danger))
                    continue;

                registerMethodTree(context, clazz, method, saltedMethods);
            }
        }

        for(var method : saltedMethods) {
            method.salt().updateVar(method.allocParameter(Type.INT_TYPE));
            markChange();
        }

        var modified = new HashSet<AbstractInsnNode>();
        for(var method : saltedMethods) {
            var refs = graph.refs(method);
            var salt = method.salt();

            for(var ref : refs) {
                var call = (MethodInsnNode) ref.insn();
                if(!modified.add(call)) 
                    continue;

                var caller = ref.caller();
                var frames = caller.frames(context);

                salt(context, caller, salt, call, frames);
            }
        }
    }

    private void salt(Context context, JMethod caller, ISalt salt, MethodInsnNode call, Map<AbstractInsnNode, SimpleFrame> frames) {
        var list = new InsnList();

        if(!caller.canSalt(frames.get(call))) {
            list.add(context.properties().add(ASMUtils.pushInt(salt.value()), Property.UNPROTECTED_SALT));
        } else {
            var mask = caller.seed();
            var masked = caller.salt().value() & mask;

            list.add(caller.salt().load());
            list.add(context.properties().add(ASMUtils.pushInt(mask), Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS));
            list.add(new InsnNode(IAND));
            list.add(context.properties().add(ASMUtils.pushInt(masked ^ salt.value()), Property.IGNORE_INTEGER));
            list.add(new InsnNode(IXOR));
        }

        caller.insns().insertBefore(call, list);
        call.desc = call.desc.replace(")", "I)");
    }

    private boolean collidesWithDanger(JMethod method, Set<JMethod> danger) {
        for(var member : methodTreeClosure(method)) {
            var saltedDesc = member.desc().replace(")", "I)");
            var hit = danger.stream().anyMatch(d -> d.owner() == member.owner() && d.name().equals(member.name()) && d.desc().equals(saltedDesc));
            if(hit)
                return true;
        }

        return false;
    }

    private Set<JMethod> methodTreeClosure(JMethod method) {
        var visited = new HashSet<JMethod>();
        var queue = new ArrayDeque<JMethod>();

        visited.add(method);
        queue.add(method);

        while(!queue.isEmpty()) {
            var current = queue.poll();
            for(var related : current.tree()) {
                if(visited.add(related))
                    queue.add(related);
            }
        }

        return visited;
    }

    private void registerMethodTree(Context context, JClass clazz, JMethod method, Set<JMethod> saltedMethods) {
        var scope = collisionScope(context, method);
        var salt = findOrGenerateSalt(method, scope);

        for(var member : methodTreeClosure(method)) {
            if(!saltedMethods.add(member))
                continue;

            member.removeAccessFlags(ACC_VARARGS);
            member.makeSalt(salt, -1);
        }
    }

    private int findOrGenerateSalt(JMethod method, Set<JClass> impactedClasses) {
        for(var clazz : impactedClasses) {
            var foundOpt = clazz.findMethod(method.name(), method.desc());
            if(foundOpt.isEmpty())
                continue;

            var found = foundOpt.get();
            if(!found.hasSalt())
                continue;

            return found.salt().value();
        }

        return random.nextInt();
    }

    private boolean skipMethodAndTree(ReferenceGraph graph, JMethod method, Set<JClass> impactedClasses) {
        if(method.owner().isLibMethod(method))
            return true;

        for(var member : impactedClasses) {
            var opt = member.findMethod(method.name(), method.desc());
            if(opt.isPresent())
                method = opt.get();

            if(Exclusions.METHOD_SALTING.excluded(member))
                return true;

            if(Exclusions.METHOD_SALTING.excluded(member, method))
                return true;

            if(cantEditMethod(member, method, true))
                return true;

            var refs = graph.refs(method);
            if (refs.stream().anyMatch(MethodReference::cantEdit))
                return true;
        }

        return false;
    }

    private Set<JClass> collisionScope(Context context, JMethod method) {
        var visited = new HashSet<JClass>();
        var queue = new ArrayDeque<JClass>();

        visited.add(method.owner());
        queue.add(method.owner());

        while(!queue.isEmpty()) {
            var current = queue.poll();

            for(var child : current.children()) {
                if(visited.add(child))
                    queue.add(child);
            }

            for(var parent : current.parents()) {
                if(visited.contains(parent) || parent.isLibrary())
                    continue;

                if(parent.hasMethodInTree(context, method)) {
                    visited.add(parent);
                    queue.add(parent);
                }
            }
        }

        return visited;
    }
}
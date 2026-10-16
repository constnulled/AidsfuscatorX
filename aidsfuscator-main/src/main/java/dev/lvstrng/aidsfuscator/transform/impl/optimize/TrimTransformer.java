package dev.lvstrng.aidsfuscator.transform.impl.optimize;

import dev.lvstrng.aidsfuscator.analysis.ref.ReferenceGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.asm.SeenClassMethodVisitor;
import dev.lvstrng.aidsfuscator.context.asm.SeenClassVisitor;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class TrimTransformer extends Transformer {
    private final Setting<Boolean> classes = setting("classes", true);
    private final Setting<Boolean> fields = setting("fields", true);
    private final Setting<Boolean> methods = setting("methods", true);

    private final Set<String> seenClasses = new HashSet<>();
    private final Predicate<JClass> canTrimClass = e -> e.methods().isEmpty() && e.fields().isEmpty() && !seenClasses.contains(e.name());

    public TrimTransformer() {
        super("Trim Transformer", "TrimTransformer");
    }

    @Override
    public void transform(Context context) {
        var graph = context.referenceGraph().build();
        var toRemoveClasses = new HashSet<JClass>();

        for(var clazz : context.classes()) {
            if(Exclusions.TRIM.excluded(clazz))
                continue;

            var toRemoveMethods = new HashSet<JMethod>();
            var toRemoveFields = new HashSet<JField>();

            if(fields.value()) {
                for(var field : clazz.fields()) {
                    if(!canTrimField(graph, field))
                        continue;

                    if(Exclusions.TRIM.excluded(field))
                        continue;

                    toRemoveFields.add(field);
                    markChange();
                }
            }

            if(methods.value()) {
                for(var method : clazz.methods()) {
                    if(!canTrimMethod(graph, method))
                        continue;

                    if(Exclusions.TRIM.excluded(method))
                        continue;

                    toRemoveMethods.add(method);
                    markChange();
                }
            }

            toRemoveFields.forEach(clazz::remove);
            toRemoveMethods.forEach(clazz::remove);
        }

        for(var clazz : context.classes()) {
            clazz.accept(new SeenClassVisitor(seenClasses), null);
            for(var method : clazz.methods()) {
                method.core().accept(new SeenClassMethodVisitor(seenClasses));
            }
        }
        for(var clazz : context.classes()) {
            registerClass(toRemoveClasses, clazz);
        }
        toRemoveClasses.forEach(e -> {
            context.classMap().remove(e.name());
            markChange();
        });
    }

    private boolean canTrimMethod(ReferenceGraph graph, JMethod method) {
        if(cantEditMethod(method.owner(), method))
            return false;

        if(!method.tree().isEmpty())
            return false;

        var refs = graph.refs(method).stream()
                .filter(node -> node.caller() != node.method())
                .toList();
        if(refs.isEmpty())
            return true;

        var visited = new HashSet<JMethod>();
        visited.add(method);
        for(var ref : refs) {
            if(!isCallerChainDead(graph, ref.caller(), visited, 0))
                return false;
        }

        return true;
    }

    private boolean canTrimField(ReferenceGraph graph, JField field) {
        var refs = graph.refs(field);
        if(refs.isEmpty())
            return true;

        var visited = new HashSet<JMethod>();
        for(var ref : refs) {
            if(!isCallerChainDead(graph, ref.caller(), visited, 0))
                return false;
        }

        return true;
    }

    private void registerClass(Set<JClass> toRemoveClasses, JClass clazz) {
        if(Exclusions.TRIM.excluded(clazz))
            return;

        if(classes.value() && canTrimClass.test(clazz))
            toRemoveClasses.add(clazz);
    }


    /**
     * DFS to get to the last nodes in the tree and then check from there to make sure
     * the entire chain/path is dead
     */
    private boolean isCallerChainDead(ReferenceGraph graph, JMethod method, Set<JMethod> visited, int depth) {
        if(!visited.add(method)) return true;

        if(Exclusions.TRIM.excluded(method.owner()) || Exclusions.TRIM.excluded(method)) return false;

        if(cantEditMethod(method.owner(), method)) return false;

        if(!method.tree().isEmpty()) return false;

        var refs = graph.refs(method).stream()
                .filter(node -> node.caller() != node.method())
                .toList();
        if(refs.isEmpty()) return true;

        for(var ref : refs) {
            if(!isCallerChainDead(graph, ref.caller(), visited, depth + 1))
                return false;
        }

        return true;
    }
}

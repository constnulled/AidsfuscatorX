package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;

import java.util.*;

public class MethodRenameTransformer extends Transformer {
    private final Setting<String> prefix = setting("prefix", "");
    private final Setting<Boolean> shuffle = setting("shuffle", false);

    public MethodRenameTransformer() {
        super("Rename Methods", "RenameMethodTransformer");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            mapMethods(context, clazz);
        }

        remap(context);
        Mappings.METHOD.clearTemp();
    }

    private void mapMethods(Context context, JClass clazz) {
        if(shuffle.value()) {
            var seed = random.nextLong();

            Collections.shuffle(clazz.core().methods, new Random(seed));
            Collections.shuffle(clazz.methods(), new Random(seed));
        }

        for(var method : clazz.methods()) {
            var identity = identityScope(context, method);
            if(!doRename(method, identity))
                continue;

            var collision = new HashSet<>(identity);
            collision.addAll(clazz.tree()); // extra classes to avoid colliding with, not to propagate to

            var newName = newName(context, clazz, method, identity, collision);
            // every member of identity actually shares this method (declared or inherited)
            for(var member : identity) {
                var oldKey = MemberUtils.fullMethod(member, method);
                var newKey = MemberUtils.fullMethod(member.name(), newName, method.desc());
                Mappings.METHOD.register(oldKey, new Mapping(newKey, newName));

                member.findMethod(method.name(), method.desc()).ifPresent(e -> {
                    e.setMappedName(newName);
                    markChange();
                });
            }
        }
    }

    private String newName(Context context, JClass clazz, JMethod method, Set<JClass> identity, Set<JClass> collisionScope) {
        for(var member : identity) {
            var id = MemberUtils.fullMethod(member, method);
            if(Mappings.METHOD.containsOld(id))
                return Mappings.METHOD.retrieve(id).value();
        }

        return context.dictionary().newMethodName(prefix.value(), clazz, method.desc(), collisionScope);
    }

    private boolean doRename(JMethod root, Set<JClass> scope) {
        if(root.owner().isLibMethod(root))
            return false;

        for(var clazz : scope) {
            if(Exclusions.RENAME_METHOD.excluded(clazz))
                return false;

            if(clazz.isLibrary())
                return false;

            var method = root;
            var methodOpt = clazz.findMethod(root.name(), root.desc());
            if(methodOpt.isPresent())
                method = methodOpt.get();

            if(Exclusions.RENAME_METHOD.excluded(clazz, method))
                return false;

            if(cantEditMethod(clazz, method, false, true))
                return false;
        }

        return true;
    }

    /**
     * Walks the full override closure for this method, not just clazz's own hierarchy.
     * Needed because a class implementing two unrelated interfaces with matching name+desc
     * bridges them, so a name picked from one interface's side can still collide on the other.
     * Used for propagation - every member here genuinely shares this method's identity.
     * @param context obfuscator context
     * @param method method to find the identity scope for
     * @author brownie
     */
    private Set<JClass> identityScope(Context context, JMethod method) {
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

package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.MemberUtils;

import java.util.HashSet;
import java.util.Set;

public class NewMethodRenameTransformer extends Transformer {
    public NewMethodRenameTransformer() {
        super("Rename Methods", "renameMethods");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            mapMethods(context, clazz);
        }

        remap(context);
    }

    private void mapMethods(Context context, JClass clazz) {
        for(var method : clazz.methods()) {
            if(clazz.isLibMethod(method))
                continue;

            var impactedClasses = impactedClasses(context, clazz, method);
            if(skipHierarchy(clazz, method, impactedClasses))
                continue;

            var name = findOrGenerateName(context, clazz, impactedClasses, method);
            for(var member : impactedClasses) {
                var oldId = MemberUtils.fullMethod(member, method);
                if(Mappings.METHOD.containsOld(oldId))
                    continue;

                var newId = MemberUtils.fullMethod(member.name(), name, method.desc());
                Mappings.METHOD.register(oldId, new Mapping(newId, name));

                markChange();
            }
        }
    }

    private String findOrGenerateName(Context context, JClass clazz, Set<JClass> impactedClasses, JMethod method) {
        for(var member : impactedClasses) {
            var id = MemberUtils.fullMethod(member, method);

            if(Mappings.METHOD.containsOld(id))
                return Mappings.METHOD.retrieve(id).value();
        }

        return context.dictionary().newMethodName(clazz, method.desc());
    }

    /**
     * Exclusion and invalid method check
     * @param clazz method class
     * @param method method
     * @param impactedClasses all impacted classes
     * @return false if should continue, true if should skip
     */
    private boolean skipHierarchy(JClass clazz, JMethod method, Set<JClass> impactedClasses) {
        // ---- DIRECT CHECKS ----
        if(cantEditMethod(clazz, method, false, true))
            return true;

        if(Exclusions.RENAME_METHOD.excluded(clazz))
            return true;

        if(Exclusions.RENAME_METHOD.excluded(method))
            return true;

        // ---- CLASS TREE CHECKS ----
        for(var member : impactedClasses) {
            if(Exclusions.RENAME_METHOD.excluded(member))
                return true;

            if(Exclusions.RENAME_METHOD.excluded(member, method))
                return true;

            if(cantEditMethod(member, method, false, true))
                return true;
        }

        return false;
    }

    private Set<JClass> impactedClasses(Context context, JClass clazz, JMethod method) {
        var classes = new HashSet<>(clazz.children()); // children will obviously have this method in their tree
        classes.add(clazz);

        for(var parent : clazz.parents()) {
            if(!parent.hasMethodInTree(context, method))
                continue;

            classes.add(parent);
        }

        return classes;
    }
}

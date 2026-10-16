package dev.lvstrng.aidsfuscator.transform.impl.optimize;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class TrimTransformer extends Transformer {

    private final Setting<Boolean> trimClasses   = setting("classes",      true);
    private final Setting<Boolean> trimFields    = setting("fields",       true);
    private final Setting<Boolean> trimMethods   = setting("methods",      true);
    private final Setting<Boolean> keepPublicApi = setting("keepPublicApi", true);

    public TrimTransformer() {
        super("Trim Transformer", "TrimTransformer");
    }

    @Override
    public void transform(Context context) {
        Set<JClass>  reachableClasses = new HashSet<>();
        Set<JClass>  instantiated     = new HashSet<>();
        Set<JMethod> reachableMethods = new HashSet<>();
        Set<JField>  reachableFields  = new HashSet<>();
        Deque<JMethod> queue          = new ArrayDeque<>();

        seed(context, reachableClasses, instantiated, reachableMethods, reachableFields, queue);
        propagate(context, reachableClasses, instantiated, reachableMethods, reachableFields, queue);
        propagateOverrides(context, reachableClasses, instantiated, reachableMethods, reachableFields, queue);
        applyTrim(context, reachableClasses, reachableMethods, reachableFields);
    }

    private void seed(Context context,
                      Set<JClass>  reachableClasses,
                      Set<JClass>  instantiated,
                      Set<JMethod> reachableMethods,
                      Set<JField>  reachableFields,
                      Deque<JMethod> queue) {
        for (JClass cls : context.classes()) {
            boolean classKept = Exclusions.TRIM.excluded(cls);

            for (JMethod method : cls.methods()) {
                if (classKept
                        || isHardSeed(cls, method)
                        || Exclusions.TRIM.excluded(method)) {
                    enqueue(cls, method, reachableClasses, instantiated, reachableMethods, queue);
                }
            }

            for (JField field : cls.fields()) {
                if (classKept || Exclusions.TRIM.excluded(field)) {
                    reachableClasses.add(cls);
                    reachableFields.add(field);
                }
            }
        }

        if (keepPublicApi.value()) {
            for (JClass cls : context.classes()) {
                if (!cls.isPublic()) continue;
                reachableClasses.add(cls);
                instantiated.add(cls);
                for (JMethod method : cls.methods()) {
                    if (method.isPublic() || method.isProtected()) {
                        enqueue(cls, method, reachableClasses, instantiated, reachableMethods, queue);
                    }
                }
                for (JField field : cls.fields()) {
                    if (field.isPublic() || field.isProtected()) {
                        reachableFields.add(field);
                    }
                }
            }
        }

        for (JClass cls : new ArrayList<>(reachableClasses)) {
            seedStaticInitializer(cls, reachableClasses, instantiated, reachableMethods, queue);
        }
    }

    private void propagate(Context context,
                           Set<JClass>  reachableClasses,
                           Set<JClass>  instantiated,
                           Set<JMethod> reachableMethods,
                           Set<JField>  reachableFields,
                           Deque<JMethod> queue) {
        while (!queue.isEmpty()) {
            JMethod method = queue.poll();
            scanBytecode(method, context,
                         reachableClasses, instantiated,
                         reachableMethods, reachableFields, queue);
        }
    }

    private void propagateOverrides(Context context,
                                    Set<JClass>  reachableClasses,
                                    Set<JClass>  instantiated,
                                    Set<JMethod> reachableMethods,
                                    Set<JField>  reachableFields,
                                    Deque<JMethod> queue) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (JClass cls : context.classes()) {
                if (!instantiated.contains(cls)) continue;
                for (JMethod method : cls.methods()) {
                    if (reachableMethods.contains(method)) continue;
                    if (overridesReachable(method, cls, reachableMethods)) {
                        enqueue(cls, method, reachableClasses, instantiated, reachableMethods, queue);
                        changed = true;
                    }
                }
            }
            propagate(context, reachableClasses, instantiated, reachableMethods, reachableFields, queue);
        }
    }

    private void applyTrim(Context context,
                           Set<JClass>  reachableClasses,
                           Set<JMethod> reachableMethods,
                           Set<JField>  reachableFields) {
        for (JClass cls : context.classes()) {
            if (Exclusions.TRIM.excluded(cls)) continue;

            if (trimMethods.value()) {
                List<JMethod> dead = new ArrayList<>();
                for (JMethod method : cls.methods()) {
                    if (reachableMethods.contains(method)) continue;
                    if (Exclusions.TRIM.excluded(method)) continue;
                    if (cantEditMethod(cls, method, true, true)) continue;
                    dead.add(method);
                }
                if (!dead.isEmpty()) {
                    dead.forEach(cls::remove);
                    markChange();
                }
            }

            if (trimFields.value()) {
                List<JField> dead = new ArrayList<>();
                for (JField field : cls.fields()) {
                    if (reachableFields.contains(field)) continue;
                    if (Exclusions.TRIM.excluded(field)) continue;
                    dead.add(field);
                }
                if (!dead.isEmpty()) {
                    dead.forEach(cls::remove);
                    markChange();
                }
            }
        }

        if (trimClasses.value()) {
            List<String> deadNames = new ArrayList<>();
            for (JClass cls : context.classes()) {
                if (Exclusions.TRIM.excluded(cls)) continue;
                if (!reachableClasses.contains(cls)) {
                    deadNames.add(cls.name());
                }
            }
            if (!deadNames.isEmpty()) {
                deadNames.forEach(context.classMap()::remove);
                markChange();
            }
        }
    }

    private void scanBytecode(JMethod method,
                               Context context,
                               Set<JClass>  reachableClasses,
                               Set<JClass>  instantiated,
                               Set<JMethod> reachableMethods,
                               Set<JField>  reachableFields,
                               Deque<JMethod> queue) {
        MethodNode mn = method.core();
        if (mn.instructions == null) return;

        for (AbstractInsnNode insn : mn.instructions) {
            switch (insn.getType()) {

                case AbstractInsnNode.METHOD_INSN -> {
                    MethodInsnNode mi = (MethodInsnNode) insn;
                    JClass owner = context.classMap().get(mi.owner);
                    if (owner == null) break;
                    boolean isNew = insn.getOpcode() == Opcodes.INVOKESPECIAL
                                 && mi.name.equals("<init>");
                    if (isNew) {
                        markInstantiated(owner, reachableClasses, instantiated,
                                         reachableMethods, queue);
                    } else {
                        reachableClasses.add(owner);
                    }
                    owner.findMethod(mi.name, mi.desc).ifPresent(m ->
                        enqueue(owner, m, reachableClasses, instantiated, reachableMethods, queue)
                    );
                }

                case AbstractInsnNode.FIELD_INSN -> {
                    FieldInsnNode fi = (FieldInsnNode) insn;
                    JClass owner = context.classMap().get(fi.owner);
                    if (owner == null) break;
                    reachableClasses.add(owner);
                    owner.fields().stream()
                         .filter(f -> f.name().equals(fi.name) && f.desc().equals(fi.desc))
                         .findFirst()
                         .ifPresent(reachableFields::add);
                }

                case AbstractInsnNode.TYPE_INSN -> {
                    TypeInsnNode ti = (TypeInsnNode) insn;
                    JClass ref = context.classMap().get(ti.desc);
                    if (ref == null) break;
                    if (ti.getOpcode() == Opcodes.NEW) {
                        markInstantiated(ref, reachableClasses, instantiated,
                                         reachableMethods, queue);
                    } else {
                        reachableClasses.add(ref);
                    }
                }

                case AbstractInsnNode.LDC_INSN -> {
                    LdcInsnNode ldc = (LdcInsnNode) insn;
                    if (ldc.cst instanceof Type t && t.getSort() == Type.OBJECT) {
                        JClass ref = context.classMap().get(t.getInternalName());
                        if (ref != null) reachableClasses.add(ref);
                    }
                }

                case AbstractInsnNode.INVOKE_DYNAMIC_INSN -> {
                    InvokeDynamicInsnNode idi = (InvokeDynamicInsnNode) insn;
                    if (idi.bsmArgs == null) break;
                    for (Object arg : idi.bsmArgs) {
                        if (!(arg instanceof Handle h)) continue;
                        JClass hClass = context.classMap().get(h.getOwner());
                        if (hClass == null) continue;
                        reachableClasses.add(hClass);
                        hClass.findMethod(h.getName(), h.getDesc()).ifPresent(m ->
                            enqueue(hClass, m, reachableClasses, instantiated, reachableMethods, queue)
                        );
                    }
                }

                case AbstractInsnNode.MULTIANEWARRAY_INSN -> {
                    MultiANewArrayInsnNode mana = (MultiANewArrayInsnNode) insn;
                    String elementType = Type.getType(mana.desc).getElementType().getInternalName();
                    JClass ref = context.classMap().get(elementType);
                    if (ref != null) reachableClasses.add(ref);
                }
            }
        }

        if (mn.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : mn.tryCatchBlocks) {
                if (tcb.type == null) continue;
                JClass excType = context.classMap().get(tcb.type);
                if (excType != null) reachableClasses.add(excType);
            }
        }

        for (Type argType : Type.getArgumentTypes(mn.desc)) {
            if (argType.getSort() == Type.OBJECT) {
                JClass ref = context.classMap().get(argType.getInternalName());
                if (ref != null) reachableClasses.add(ref);
            }
            if (argType.getSort() == Type.ARRAY) {
                Type elem = argType.getElementType();
                if (elem.getSort() == Type.OBJECT) {
                    JClass ref = context.classMap().get(elem.getInternalName());
                    if (ref != null) reachableClasses.add(ref);
                }
            }
        }

        Type returnType = Type.getReturnType(mn.desc);
        if (returnType.getSort() == Type.OBJECT) {
            JClass ref = context.classMap().get(returnType.getInternalName());
            if (ref != null) reachableClasses.add(ref);
        }
    }

    private void enqueue(JClass cls,
                         JMethod method,
                         Set<JClass>  reachableClasses,
                         Set<JClass>  instantiated,
                         Set<JMethod> reachableMethods,
                         Deque<JMethod> queue) {
        reachableClasses.add(cls);
        if (!method.isStatic()) instantiated.add(cls);
        if (reachableMethods.add(method)) {
            queue.add(method);
        }
    }

    private void markInstantiated(JClass cls,
                                   Set<JClass>  reachableClasses,
                                   Set<JClass>  instantiated,
                                   Set<JMethod> reachableMethods,
                                   Deque<JMethod> queue) {
        reachableClasses.add(cls);
        if (instantiated.add(cls)) {
            seedStaticInitializer(cls, reachableClasses, instantiated, reachableMethods, queue);
            for (JClass parent : cls.parents()) {
                markInstantiated(parent, reachableClasses, instantiated, reachableMethods, queue);
            }
        }
    }

    private void seedStaticInitializer(JClass cls,
                                        Set<JClass>  reachableClasses,
                                        Set<JClass>  instantiated,
                                        Set<JMethod> reachableMethods,
                                        Deque<JMethod> queue) {
        cls.findMethod("<clinit>", "()V").ifPresent(clinit ->
            enqueue(cls, clinit, reachableClasses, instantiated, reachableMethods, queue)
        );
    }

    private boolean isHardSeed(JClass cls, JMethod method) {
        String name = method.name();
        String desc = method.desc();

        if (name.equals("main")
                && desc.equals("([Ljava/lang/String;)V")
                && method.isStatic() && method.isPublic()) {
            return true;
        }

        if ((name.equals("premain") || name.equals("agentmain")) && method.isStatic()) {
            return true;
        }

        if (name.equals("readObject") || name.equals("writeObject")
         || name.equals("readResolve") || name.equals("writeReplace")
         || name.equals("readObjectNoData")) {
            return true;
        }

        if (cls.isEnum() && (name.equals("values") || name.equals("valueOf"))) {
            return true;
        }

        if (name.equals("$deserializeLambda$")) {
            return true;
        }

        return false;
    }

    private boolean overridesReachable(JMethod method,
                                        JClass cls,
                                        Set<JMethod> reachableMethods) {
        if (method.name().startsWith("<")) return false;
        if (method.isStatic()) return false;
        for (JClass parent : cls.parents()) {
            for (JMethod parentMethod : parent.methods()) {
                if (parentMethod.name().equals(method.name())
                        && parentMethod.desc().equals(method.desc())
                        && reachableMethods.contains(parentMethod)) {
                    return true;
                }
            }
        }
        return false;
    }
}

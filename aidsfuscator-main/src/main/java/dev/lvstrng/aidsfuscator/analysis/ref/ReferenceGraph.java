package dev.lvstrng.aidsfuscator.analysis.ref;

import dev.lvstrng.aidsfuscator.analysis.ref.collector.IReferenceCollector;
import dev.lvstrng.aidsfuscator.analysis.ref.collector.impl.ClassReferenceCollector;
import dev.lvstrng.aidsfuscator.analysis.ref.collector.impl.FieldReferenceCollector;
import dev.lvstrng.aidsfuscator.analysis.ref.collector.impl.MethodReferenceCollector;
import dev.lvstrng.aidsfuscator.analysis.ref.nodes.ClassReference;
import dev.lvstrng.aidsfuscator.analysis.ref.nodes.FieldReference;
import dev.lvstrng.aidsfuscator.analysis.ref.nodes.MethodReference;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.exception.MissingMemberException;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JField;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds a reference graph of every field and method in the obfuscatable (and excluded) classes.
 * @author lvstrng
 */
public class ReferenceGraph {
    private final Context context;
    private final Map<JMethod, Set<MethodReference>> methodReferences;
    private final Map<JField, Set<FieldReference>> fieldReferences;
    private final Map<JClass, Set<ClassReference>> classReferences;

    private final Map<JMethod, Set<MethodReference>> methodReferencesIn;
    private final Map<JMethod, Set<FieldReference>> fieldReferencesIn;
    private final Map<JMethod, Set<ClassReference>> classReferencesIn;

    private final List<IReferenceCollector> collectors = List.of(
            new ClassReferenceCollector(),
            new FieldReferenceCollector(),
            new MethodReferenceCollector()
    );

    public ReferenceGraph(Context context) {
        this.context = context;

        this.methodReferences = new ConcurrentHashMap<>();
        this.fieldReferences = new ConcurrentHashMap<>();
        this.classReferences = new ConcurrentHashMap<>();

        this.methodReferencesIn = new ConcurrentHashMap<>();
        this.fieldReferencesIn = new ConcurrentHashMap<>();
        this.classReferencesIn = new ConcurrentHashMap<>();
    }

    public ReferenceGraph build() {
        this.clear();

        for(var clazz : context.jarClasses()) {
            try {
                for(var method : clazz.methods()) {
                    method.insns().forEach(insn -> collectors.stream()
                            .filter(e -> e.isOfType(insn))
                            .forEach(e -> e.collect(context, this, clazz, method, insn)));
                }
            } catch (MissingMemberException _) {}
        }

        return this;
    }

    public Set<MethodReference> refs(JMethod method) {
        return methodReferences.computeIfAbsent(method, _ -> new HashSet<>());
    }

    public Set<FieldReference> refs(JField field) {
        return fieldReferences.computeIfAbsent(field, _ -> new HashSet<>());
    }

    public Set<ClassReference> refs(JClass clazz) {
        return classReferences.computeIfAbsent(clazz, _ -> new HashSet<>());
    }

    public Set<MethodReference> methodRefsIn(JMethod method) {
        return methodReferencesIn.computeIfAbsent(method, _ -> new HashSet<>());
    }

    public Set<FieldReference> fieldRefsIn(JMethod method) {
        return fieldReferencesIn.computeIfAbsent(method, _ -> new HashSet<>());
    }

    public Set<ClassReference> classRefsIn(JMethod method) {
        return classReferencesIn.computeIfAbsent(method, _ -> new HashSet<>());
    }

    public void handleHandle(JClass callerClass, JMethod caller, AbstractInsnNode insn, Handle handle) {
        var node = construct(callerClass, caller, insn, handle.getOwner(), handle.getName(), handle.getDesc());
        if(node == null)
            return;

        add(node);
    }

    public void add(FieldReference node) {
        fieldReferences.computeIfAbsent(node.field(), _ -> new HashSet<>()).add(node);
        fieldReferencesIn.computeIfAbsent(node.caller(), _ -> new HashSet<>()).add(node);
    }

    public void add(MethodReference node) {
        methodReferences.computeIfAbsent(node.method(), _ -> new HashSet<>()).add(node);
        methodReferencesIn.computeIfAbsent(node.caller(), _ -> new HashSet<>()).add(node);
    }

    public void add(ClassReference node) {
        classReferences.computeIfAbsent(node.clazz(), _ -> new HashSet<>()).add(node);
        classReferencesIn.computeIfAbsent(node.caller(), _ -> new HashSet<>()).add(node);
    }

    public void clear() {
        fieldReferencesIn.clear();
        fieldReferences.clear();

        methodReferencesIn.clear();
        methodReferences.clear();

        classReferencesIn.clear();
        classReferences.clear();
    }

    public ClassReference constructClass(JClass callerClass, JMethod caller, AbstractInsnNode insn, String className) {
        var clazz = context.forName(className);
        if(clazz == null)
            return null;

        return constructClass(callerClass, caller, insn, clazz);
    }

    public ClassReference constructClass(JClass callerClass, JMethod caller, AbstractInsnNode insn, JClass clazz) {
        return new ClassReference(callerClass, caller, clazz, insn);
    }

    public FieldReference constructField(JClass callerClass, JMethod caller, AbstractInsnNode insn, String ownerName, String name, String desc) {
        var owner = context.forName(ownerName);
        if(owner == null)
            return null;

        add(constructClass(callerClass, caller, insn, owner));
        var field = owner.findFieldFull(context, name, desc);
        if(field == null)
            return null;

        return new FieldReference(callerClass, caller, field, insn);
    }

    public MethodReference construct(JClass callerClass, JMethod caller, AbstractInsnNode insn, String ownerName, String name, String desc) {
        var owner = context.forName(ownerName);
        if (owner == null)
            return null;

        add(constructClass(callerClass, caller, insn, owner));
        var method = owner.findMethodFull(context, name, desc);
        if (method == null)
            return null;

        return new MethodReference(callerClass, caller, method, insn);
    }
}

package dev.lvstrng.aidsfuscator.reference;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.reference.impl.FieldReferenceCandidate;
import dev.lvstrng.aidsfuscator.reference.impl.MethodReferenceCandidate;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

public class ReferenceManager {
    private final Context context;
    private final List<IReferenceCandidate> references;

    public ReferenceManager(Context context) {
        this.context = context;
        this.references = new ArrayList<>();
    }

    public boolean canObfuscate(MethodInsnNode call) {
        return references.stream().filter(MethodReferenceCandidate.class::isInstance)
                .anyMatch(e -> e.test(call.owner, call.name, call.desc));
    }

    public boolean canObfuscate(FieldInsnNode field) {
        return references.stream().filter(FieldReferenceCandidate.class::isInstance)
                .anyMatch(e -> e.test(field.owner, field.name, field.desc));
    }

    public void addMethodCandidate(String filter) {
        references.add(new MethodReferenceCandidate(context, filter));
    }

    public void addFieldCandidate(String filter) {
        references.add(new FieldReferenceCandidate(context, filter));
    }

    public List<IReferenceCandidate> references() {
        return references;
    }

    public List<IReferenceCandidate> fieldReferences() {
        return references.stream().filter(FieldReferenceCandidate.class::isInstance).toList();
    }

    public List<IReferenceCandidate> methodReferences() {
        return references.stream().filter(MethodReferenceCandidate.class::isInstance).toList();
    }
}

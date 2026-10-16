package dev.lvstrng.aidsfuscator.context.asm;

import org.objectweb.asm.*;

import java.util.Arrays;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class SeenClassVisitor extends ClassVisitor {
    private final Set<String> seenClasses;

    public SeenClassVisitor(Set<String> seenClasses) {
        super(ASM9);
        this.seenClasses = seenClasses;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if(superName != null)
            add(Type.getObjectType(superName));
        if(interfaces != null)
            Arrays.stream(interfaces).map(Type::getObjectType).forEach(this::add);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Arrays.stream(Type.getArgumentTypes(descriptor)).forEach(this::add);
        add(Type.getReturnType(descriptor));
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        add(Type.getType(descriptor));
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        add(Type.getType(descriptor));
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        add(Type.getType(descriptor));
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        if(name != null)
            add(Type.getObjectType(name));
        if(outerName != null)
            add(Type.getObjectType(outerName));
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitNestHost(String nestHost) {
        if(nestHost == null)
            return;

        add(Type.getObjectType(nestHost));
        super.visitNestHost(nestHost);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        if(descriptor == null)
            return;
        add(Type.getType(descriptor));
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public void visitNestMember(String nestMember) {
        if(nestMember == null)
            return;

        add(Type.getObjectType(nestMember));
        super.visitNestMember(nestMember);
    }

    @Override
    public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
        add(Type.getType(descriptor));
        return super.visitRecordComponent(name, descriptor, signature);
    }

    private void add(Type type) {
        if(type.getSort() == Type.OBJECT)
            seenClasses.add(type.getInternalName());
        else if (type.getSort() == Type.ARRAY) {
            add(type.getElementType());
        }
    }
}

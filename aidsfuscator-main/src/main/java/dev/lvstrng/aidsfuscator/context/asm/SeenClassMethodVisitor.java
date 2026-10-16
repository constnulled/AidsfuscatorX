package dev.lvstrng.aidsfuscator.context.asm;

import org.objectweb.asm.*;

import java.util.Arrays;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class SeenClassMethodVisitor extends MethodVisitor {
    private final Set<String> seenClasses;

    public SeenClassMethodVisitor(Set<String> seenClasses) {
        super(ASM9);
        this.seenClasses = seenClasses;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        addInternal(owner);
        Arrays.stream(Type.getArgumentTypes(descriptor)).forEach(this::add);
        add(Type.getReturnType(descriptor));
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        addInternal(owner);
        addDescriptor(descriptor);
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if(value instanceof Type t)
            add(t);
        if(value instanceof Handle h)
            addInternal(h.getOwner());
        super.visitLdcInsn(value);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        addInternal(type);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        addInternal(type);
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
        addDescriptor(descriptor);
        return super.visitParameterAnnotation(parameter, descriptor, visible);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        Arrays.stream(Type.getArgumentTypes(descriptor)).forEach(this::add);
        add(Type.getReturnType(descriptor));

        try {
            Arrays.stream(Type.getArgumentTypes(bootstrapMethodHandle.getDesc())).forEach(this::add);
            add(Type.getReturnType(bootstrapMethodHandle.getDesc()));
        } catch (StringIndexOutOfBoundsException _) {}
        addInternal(bootstrapMethodHandle.getOwner());

        for(var obj : bootstrapMethodArguments) {
            if(!(obj instanceof Handle h))
                continue;

            try {
                Arrays.stream(Type.getArgumentTypes(h.getDesc())).forEach(this::add);
                add(Type.getReturnType(h.getDesc()));
            } catch (StringIndexOutOfBoundsException _) {}
            addInternal(h.getOwner());
        }
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    private void add(Type type) {
        if(type.getSort() == Type.OBJECT)
            seenClasses.add(type.getInternalName());
        else if (type.getSort() == Type.ARRAY) {
            add(type.getElementType());
        }
    }

    private void addInternal(String internalName) {
        if(internalName == null)
            return;

        var type = Type.getObjectType(internalName);
        if(type.getSort() == Type.OBJECT)
            seenClasses.add(type.getInternalName());
        else if (type.getSort() == Type.ARRAY) {
            add(type.getElementType());
        }
    }

    private void addDescriptor(String descriptor) {
        if(descriptor == null)
            return;

        var type = Type.getType(descriptor);
        if(type.getSort() == Type.OBJECT)
            seenClasses.add(type.getInternalName());
        else if (type.getSort() == Type.ARRAY) {
            add(type.getElementType());
        }
    }
}

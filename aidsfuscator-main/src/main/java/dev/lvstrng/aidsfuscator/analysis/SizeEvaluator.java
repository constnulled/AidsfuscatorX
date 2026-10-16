package dev.lvstrng.aidsfuscator.analysis;

import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class SizeEvaluator extends MethodVisitor {
    private int size;

    public SizeEvaluator() {
        super(ASM9);

        this.size = 0;
        this.size += 2;
        this.size += 2;
        this.size += 2;
        this.size += 2;
    }

    public int getSize() {
        return size;
    }

    @Override
    public void visitInsn(int opcode) {
        size++;
        super.visitInsn(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        if (varIndex < 4 && opcode != RET) {
            size++;
        } else if (varIndex >= 256) {
            size += 4;
        } else {
            size += 2;
        }
        super.visitVarInsn(opcode, varIndex);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if(opcode == SIPUSH) {
            size += 3;
        } else {
            size += 2;
        }
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        size += 3;
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcodeAndSource, String owner, String name, String descriptor, boolean isInterface) {
        int opcode = opcodeAndSource & ~Opcodes.SOURCE_MASK;

        if(opcode == INVOKEINTERFACE) {
            size += 5;
        } else {
            size += 3;
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        size += 5;
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if(opcode == GOTO || opcode == JSR) {
            size += 5;
        } else {
            size += 8;
        }
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        size += 3;
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        if (varIndex > 255 || increment > 127 || increment < -128) {
            size += 6;
        } else {
            size += 3;
        }
        super.visitIincInsn(varIndex, increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        size += 16 + labels.length * 4;
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        size += 12 + keys.length * 8;
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
        size += 4;
        super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }
}

package dev.lvstrng.aidsfuscator.analysis.interpreter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.ArrayList;

public class SimpleFrame extends Frame<SimpleValue> {
    public SimpleFrame(int numLocals, int maxStack) {
        super(numLocals, maxStack);
    }

    public SimpleFrame(Frame<? extends SimpleValue> frame) {
        super(frame);
    }

    public static SimpleFrame of(Frame<? extends SimpleValue> frame) {
        return new SimpleFrame(frame);
    }

    public static SimpleFrame of(int numLocals, int maxStack) {
        return new SimpleFrame(numLocals, maxStack);
    }

    public boolean isInitThis() {
        if(getLocals() < 1)
            return true;

        var val = getLocal(0);
        if(val.isThis())
            return val.isInitializedThis();
        return true;
    }

    @Override
    public void execute(AbstractInsnNode insn, Interpreter<SimpleValue> interpreter) throws AnalyzerException {
        var simInterpreter = (SimpleInterpreter) interpreter;
        var method = simInterpreter.method();

        if(insn.getOpcode() == Opcodes.INVOKESPECIAL && method.name().equals("<init>")) {
            var call = ((MethodInsnNode) insn);
            var desc = call.desc;
            executeInvokeSpecial(simInterpreter, insn, desc);
            return;
        }

        super.execute(insn, interpreter);
    }

    private void executeInvokeSpecial(SimpleInterpreter interpreter, AbstractInsnNode insn, String methodDescriptor) {
        var valueList = new ArrayList<SimpleValue>();
        for (int i = Type.getArgumentCount(methodDescriptor); i > 0; --i) {
            valueList.addFirst(pop());
        }

        if(insn.getOpcode() != Opcodes.INVOKESTATIC)
            valueList.addFirst(pop());
        if(Type.getReturnType(methodDescriptor) == Type.VOID_TYPE) {
            interpreter.handleInitializer(insn, methodDescriptor, valueList, this);
        } else {
            push(interpreter.handleInitializer(insn, methodDescriptor, valueList, this));
        }
    }
}

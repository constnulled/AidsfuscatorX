package dev.lvstrng.aidsfuscator.transform.impl.vm;

import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.SwitchUtils;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public final class VMInterpreterBuilder {

    private VMInterpreterBuilder() {}

    public static void build(JMethod vmMethod, Map<VMOpcode, Integer> isaTable,
                             Set<VMOpcode> usedOpcodes) {
        var insns = vmMethod.insns();

        insns.add(new InsnNode(ICONST_0));
        insns.add(new VarInsnNode(ISTORE, 4));

        insns.add(new InsnNode(ICONST_0));
        insns.add(new VarInsnNode(ISTORE, 5));

        insns.add(new IntInsnNode(SIPUSH, 256));
        insns.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
        insns.add(new VarInsnNode(ASTORE, 6));

        var loopStart = new LabelNode();
        var loopEnd = new LabelNode();

        insns.add(loopStart);

        insns.add(new VarInsnNode(ILOAD, 4));
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new InsnNode(ARRAYLENGTH));
        insns.add(new JumpInsnNode(IF_ICMPGE, loopEnd));
        insns.add(new VarInsnNode(ALOAD, 0));
        insns.add(new VarInsnNode(ILOAD, 4));
        insns.add(new IincInsnNode(4, 1));
        insns.add(new InsnNode(BALOAD));
        insns.add(new IntInsnNode(SIPUSH, 255));
        insns.add(new InsnNode(IAND));
        insns.add(new VarInsnNode(ILOAD, 3));
        insns.add(new InsnNode(IXOR));
        insns.add(new IntInsnNode(SIPUSH, 255));
        insns.add(new InsnNode(IAND));
        insns.add(new VarInsnNode(ISTORE, 7));

        var caseMap = new HashMap<Integer, LabelNode>();
        var caseBlocks = new HashMap<VMOpcode, InsnList>();

        for (var op : usedOpcodes) {
            int opNum = isaTable.get(op);
            var caseLabel = new LabelNode();
            caseMap.put(opNum, caseLabel);
            var block = new InsnList();
            block.add(caseLabel);
            VMInstructionEmitter.emitCase(block, op, loopStart);
            caseBlocks.put(op, block);
        }

        var defaultLabel = new LabelNode();
        var switchNode = SwitchUtils.createLookup(defaultLabel, caseMap);
        vmMethod.properties().add(Property.IGNORE_SWITCH_MUTATION);

        insns.add(new VarInsnNode(ILOAD, 7));
        insns.add(switchNode);

        for (var op : usedOpcodes) {
            insns.add(caseBlocks.get(op));
        }

        insns.add(defaultLabel);
        insns.add(new TypeInsnNode(NEW, "java/lang/IllegalStateException"));
        insns.add(new InsnNode(DUP));
        insns.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/IllegalStateException",
                "<init>", "()V", false));
        insns.add(new InsnNode(ATHROW));

        insns.add(loopEnd);
        insns.add(new InsnNode(ACONST_NULL));
        insns.add(new InsnNode(ARETURN));

        vmMethod.core().maxStack = 12;
        vmMethod.setMaxLocals(11);
    }
}

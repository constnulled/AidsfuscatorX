package dev.test.transform;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.ArrayList;

public class SplitTryCatchTestTransformer extends Transformer {
    public SplitTryCatchTestTransformer() {
        super("Split Try-Catch Blocks", "splitTCB");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            for (var method : clazz.methods()) {
                if (method.traps().isEmpty())
                    continue;

                var oldTraps = new ArrayList<>(method.traps());
                var graph = method.createFlowGraph(context);

                for (var block : graph.blocks()) {
                    // current try-catch detection for CFG is broken :/
                    var traps = oldTraps.stream()
                            .filter(e -> method.idx(e.start) <= method.idx(block.label()))
                            .filter(e -> method.idx(e.end) > method.idx(block.label()))
                            .toList();
                    if (traps.isEmpty())
                        continue;

                    var endLabel = new LabelNode();
                    method.insns().insert(block.lastInsn(), endLabel);

                    var list = new InsnList();
                    if (!block.deadEnd())
                        list.add(new JumpInsnNode(GOTO, block.defaultBlock().label()));

                    for (var trap : traps) {
                        var handler = new LabelNode();
                        list.add(handler);
                        list.add(new JumpInsnNode(GOTO, trap.handler));

                        method.traps().add(new TryCatchBlockNode(block.label(), endLabel, handler, trap.type));
                    }

                    method.insns().insert(endLabel, list);
                }

                method.traps().removeAll(oldTraps);
            }
        }
    }
}
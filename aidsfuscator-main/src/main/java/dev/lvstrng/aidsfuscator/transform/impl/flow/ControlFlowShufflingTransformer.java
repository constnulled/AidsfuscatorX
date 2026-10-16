package dev.lvstrng.aidsfuscator.transform.impl.flow;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Shuffles control flow blocks into a random order after control flow flattening to actually make control flow flattening strong.
 * @author lvstrng
 */
public class ControlFlowShufflingTransformer extends Transformer {
    public ControlFlowShufflingTransformer() {
        super("Control Flow Shuffling", "ControlFlowShuffleTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.FLOW_SHUFFLE.excluded(clazz))
                continue;
            for (var method : clazz.methods()) {
                if (Exclusions.FLOW_SHUFFLE.excluded(method))
                    continue;

                var graph = method.createFlowGraph(context);
                if (graph.blocks().size() <= 3)
                    continue;

                // ---- PREP ----
                method.localVariables().clear();
                var firstBlock = graph.firstBlock();
                var blocks = new ArrayList<>(graph.blocks());
                var oldTraps = new ArrayList<>(method.traps());

                // Record instruction index coverage of each trap in original instruction list
                // Trap covers [startIdx, endIdx)
                var trapRanges = new ArrayList<TrapRange>();
                for (var trap : oldTraps) {
                    int startIdx = method.idx(trap.start);
                    int endIdx = method.idx(trap.end);
                    if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                        trapRanges.add(new TrapRange(trap, startIdx, endIdx));
                    }
                }

                // Map each block to the traps covering it in the original method
                var blockTraps = new HashMap<Block, List<TryCatchBlockNode>>();
                for (var block : blocks) {
                    int blockIdx = method.idx(block.label());
                    var covering = new ArrayList<TryCatchBlockNode>();
                    for (var tr : trapRanges) {
                        if (blockIdx >= tr.startIdx && blockIdx < tr.endIdx) {
                            covering.add(tr.trap);
                        }
                    }
                    blockTraps.put(block, covering);
                }

                // Shuffle blocks, keeping entry block at the very beginning
                Collections.shuffle(blocks, random);
                blocks.remove(firstBlock);
                blocks.addFirst(firstBlock);

                // ---- REBUILD ----
                var rebuilt = new InsnList();
                var blockEndLabels = new HashMap<Block, LabelNode>();

                for (var block : blocks) {
                    for (var insn : block.insns()) {
                        method.insns().remove(insn);
                        if (insn instanceof FrameNode)
                            continue;

                        rebuilt.add(insn);
                    }

                    var endLabel = new LabelNode();
                    rebuilt.add(endLabel);
                    blockEndLabels.put(block, endLabel);

                    if (!block.deadEnd() && !block.ends() && !isNextBlock(blocks, block)) {
                        rebuilt.add(new JumpInsnNode(GOTO, block.defaultBlock().label()));
                    }
                }

                // Reconstruct try-catch table with split ranges for shuffled blocks
                var newTraps = new ArrayList<TryCatchBlockNode>();
                for (var trap : oldTraps) {
                    for (var block : blocks) {
                        if (!hasExecutableBytecode(block))
                            continue;

                        var trapsForBlock = blockTraps.get(block);
                        if (trapsForBlock != null && trapsForBlock.contains(trap)) {
                            var endLabel = blockEndLabels.get(block);
                            newTraps.add(new TryCatchBlockNode(block.label(), endLabel, trap.handler, trap.type));
                        }
                    }
                }

                method.insns().clear();
                method.insns().add(rebuilt);
                method.traps().clear();
                method.traps().addAll(newTraps);
                method.reinitUnsafeInstructions();
                markChange();
            }
        }
    }

    private boolean hasExecutableBytecode(Block block) {
        for (var insn : block.insns()) {
            if (insn.getOpcode() >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isNextBlock(List<Block> blocks, Block block) {
        var dfltIdx = blocks.indexOf(block.defaultBlock());
        var currIdx = blocks.indexOf(block);
        return (dfltIdx - currIdx) == 1;
    }

    private record TrapRange(TryCatchBlockNode trap, int startIdx, int endIdx) {}
}

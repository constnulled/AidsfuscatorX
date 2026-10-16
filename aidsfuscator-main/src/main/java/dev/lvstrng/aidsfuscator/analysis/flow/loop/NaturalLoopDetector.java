package dev.lvstrng.aidsfuscator.analysis.flow.loop;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.analysis.flow.graph.ControlFlowGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NaturalLoopDetector {

    public static List<NaturalLoop> detect(ControlFlowGraph graph) {
        var blocks = graph.blocks();
        if (blocks.isEmpty())
            return List.of();

        var entry = graph.firstBlock();
        var dominators = computeDominators(blocks, entry);

        var headerToLoop = new LinkedHashMap<Block, NaturalLoop>();

        for (var block : blocks) {
            for (var succ : block.successors()) {
                if (dominates(dominators, succ, block)) {
                    var loop = headerToLoop.computeIfAbsent(succ, NaturalLoop::new);
                    loop.addBackEdgeSource(block);
                    buildLoopBody(loop, succ, block);
                }
            }
        }

        var loops = new ArrayList<>(headerToLoop.values());
        linkParents(loops);
        for (var loop : loops)
            loop.computeExits();

        return loops;
    }

    private static boolean dominates(Map<Block, Set<Block>> dominators, Block candidate, Block node) {
        var doms = dominators.get(node);
        return doms != null && doms.contains(candidate);
    }

    private static Map<Block, Set<Block>> computeDominators(List<Block> blocks, Block entry) {
        var dom = new HashMap<Block, Set<Block>>();
        var all = new HashSet<>(blocks);

        for (var block : blocks) {
            if (block == entry) {
                var s = new HashSet<Block>();
                s.add(entry);
                dom.put(block, s);
            } else {
                dom.put(block, new HashSet<>(all));
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;

            for (var block : blocks) {
                if (block == entry)
                    continue;

                Set<Block> intersection = null;
                for (var pred : block.predecessors()) {
                    var predDom = dom.get(pred);
                    if (predDom == null)
                        continue;

                    if (intersection == null) {
                        intersection = new HashSet<>(predDom);
                    } else {
                        intersection.retainAll(predDom);
                    }
                }

                if (intersection == null)
                    intersection = new HashSet<>();

                intersection.add(block);

                if (!intersection.equals(dom.get(block))) {
                    dom.put(block, intersection);
                    changed = true;
                }
            }
        }

        return dom;
    }

    private static void buildLoopBody(NaturalLoop loop, Block header, Block backEdgeSource) {
        loop.addBodyBlock(header);

        if (backEdgeSource == header)
            return;

        var worklist = new ArrayDeque<Block>();
        var visited = new HashSet<Block>();

        worklist.push(backEdgeSource);
        visited.add(backEdgeSource);

        while (!worklist.isEmpty()) {
            var block = worklist.pop();
            loop.addBodyBlock(block);

            for (var pred : block.predecessors()) {
                if (pred == header)
                    continue;

                if (visited.add(pred))
                    worklist.push(pred);
            }
        }
    }

    private static void linkParents(List<NaturalLoop> loops) {
        for (var outer : loops) {
            NaturalLoop best = null;

            for (var candidate : loops) {
                if (candidate == outer)
                    continue;
                if (!candidate.body().containsAll(outer.body()))
                    continue;
                if (candidate.body().size() <= outer.body().size())
                    continue;
                if (best == null || candidate.body().size() < best.body().size())
                    best = candidate;
            }

            outer.setParent(best);
        }
    }

    private NaturalLoopDetector() {
    }
}

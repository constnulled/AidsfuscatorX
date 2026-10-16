package dev.lvstrng.aidsfuscator.analysis.flow.loop;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class NaturalLoop {
    private final Block header;
    private final Set<Block> body;
    private final Set<Block> backEdgeSources;
    private final Set<Block> exitBlocks;
    private NaturalLoop parent;

    public NaturalLoop(Block header) {
        this.header = header;
        this.body = new LinkedHashSet<>();
        this.backEdgeSources = new LinkedHashSet<>();
        this.exitBlocks = new LinkedHashSet<>();
    }

    public Block header() {
        return header;
    }

    public Set<Block> body() {
        return Collections.unmodifiableSet(body);
    }

    public void addBodyBlock(Block block) {
        body.add(block);
    }

    public void addBackEdgeSource(Block block) {
        backEdgeSources.add(block);
    }

    public Set<Block> backEdgeSources() {
        return Collections.unmodifiableSet(backEdgeSources);
    }

    public Set<Block> exitBlocks() {
        return Collections.unmodifiableSet(exitBlocks);
    }

    public void computeExits() {
        exitBlocks.clear();
        for (var block : body) {
            for (var succ : block.successors()) {
                if (!body.contains(succ)) {
                    exitBlocks.add(block);
                    break;
                }
            }
        }
    }

    public boolean contains(Block block) {
        return body.contains(block);
    }

    public NaturalLoop parent() {
        return parent;
    }

    public void setParent(NaturalLoop parent) {
        this.parent = parent;
    }

    public boolean isOutermost() {
        return parent == null;
    }

    public int depth() {
        int d = 0;
        var cur = parent;
        while (cur != null) {
            d++;
            cur = cur.parent;
        }
        return d;
    }

    @Override
    public String toString() {
        return "loop[header=" + header + ", size=" + body.size() + "]";
    }
}

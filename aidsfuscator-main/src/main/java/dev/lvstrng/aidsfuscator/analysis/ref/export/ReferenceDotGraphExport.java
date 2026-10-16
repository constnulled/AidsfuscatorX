package dev.lvstrng.aidsfuscator.analysis.ref.export;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lets you visualize the reference graph via dot-graph.
 * @author lvstrng
 */
public class ReferenceDotGraphExport {
    private final Context context;

    public ReferenceDotGraphExport(Context context) {
        this.context = context;
    }

    public String export(boolean full) {
        context.referenceGraph().build();
        var sb = new StringBuilder("digraph G {\n");
        sb.append("\tnode [shape=box3d, fontsize = 10];\n");
        sb.append("\tedge [fontsize = 8];\n");

        var graph = context.referenceGraph();
        var names = new HashMap<JMethod, String>();
        var added = new HashSet<String>();

        var nodeBuilder = new StringBuilder();
        var edgeBuilder = new StringBuilder();

        var n = new AtomicInteger(0);
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var refs = graph.methodRefsIn(method);
                var name = names.computeIfAbsent(method, _ -> "node" + n.getAndIncrement());

                var visited = new HashSet<String>();

                for(var node : refs) {
                    var targetName = names.computeIfAbsent(node.method(), _ -> "node" + n.getAndIncrement());
                    if(!visited.add(method.fullName()) && !full)
                        continue;

                    if(added.add(targetName))
                        nodeBuilder.append('\t').append(targetName).append(" [label = \"").append(node.method().fullName()).append("\"]\n");

                    edgeBuilder.append('\t').append(name).append(" -> ").append(targetName).append('\n');
                }

                if(added.add(name))
                    nodeBuilder.append('\t').append(name).append(" [label = \"").append(method.fullName()).append("\"]\n");
            }
        }

        sb.append(nodeBuilder).append("\n").append(edgeBuilder).append("\n}");
        return sb.toString();
    }
}

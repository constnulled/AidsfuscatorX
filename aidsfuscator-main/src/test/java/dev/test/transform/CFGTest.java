package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.flow.export.DotGraphExport;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class CFGTest extends Transformer {
    public CFGTest() {
        super("CFGTest", "cfgTest");
    }

    @Override
    public void transform(Context context) {
        for(var clazz : context.classes()) {
            for(var method : clazz.methods()) {
                var graph = method.createFlowGraph(context);
                var export = new DotGraphExport(graph).export();

                System.out.println(method);
                System.out.println(export);
                System.out.println();
            }
        }
    }
}

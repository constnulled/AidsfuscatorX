package dev.test.transform;

import dev.lvstrng.aidsfuscator.analysis.ref.export.ReferenceDotGraphExport;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class RefGraphExportTest extends Transformer {
    public RefGraphExportTest() {
        super("Export Ref Graph", "refGraphExport");
    }

    @Override
    public void transform(Context context) {
        System.out.println(new ReferenceDotGraphExport(context).export(true));
    }
}

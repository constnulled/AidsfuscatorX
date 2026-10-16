package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.file.impl.ConfigWriter;
import dev.lvstrng.aidsfuscator.file.mapping.MappingExport;
import dev.lvstrng.aidsfuscator.file.mapping.ProGuardMappingExport;
import dev.lvstrng.aidsfuscator.log.Logger;

import java.io.IOException;

public class FinishingProcessor implements IProcessor {
    @Override
    public void run(Context context) {
        try {
            var format = context.mappingFormat();

            if ("proguard".equalsIgnoreCase(format)) {
                new ProGuardMappingExport(context).write();
            } else if ("both".equalsIgnoreCase(format)) {
                new ProGuardMappingExport(context).write();
                new MappingExport(context).write();
            } else {
                new MappingExport(context).write();
            }

            Logger.success("Finished exporting files...");
        } catch (IOException _) {
            Logger.error("There was an error exporting files");
        }
    }
}

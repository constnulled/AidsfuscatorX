package dev.lvstrng.aidsfuscator.context.pipeline.preprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.dependency.DependencyAnalyzer;
import dev.lvstrng.aidsfuscator.context.exception.MissingDependencyException;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.log.Logger;

public class DependencyAnalysisProcessor implements IProcessor {
    @Override
    public void run(Context context) {
        Logger.info("Analyzing class hierarchy dependencies...");

        var report = new DependencyAnalyzer(context).analyze();
        report.log();

        if (report.hasMissingDependencies() && context.doesStrictDependencies())
            throw new MissingDependencyException(report.missingClasses().size());
    }
}

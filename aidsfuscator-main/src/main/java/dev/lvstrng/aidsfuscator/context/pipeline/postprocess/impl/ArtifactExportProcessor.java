package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.asm.HierarchyClassWriter;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.utils.Utils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodTooLargeException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarOutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

public class ArtifactExportProcessor implements IProcessor {
    @Override
    public void run(Context context) {
        Logger.info("Exporting JAR...");
        var outputFile = new File(context.out());

        try (var jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)))) {
            jos.setLevel(Deflater.BEST_COMPRESSION);
            var classes = new ArrayList<>(context.jarClasses());
            classes.addAll(context.artificials().values());

            for (var clazz : classes) {
                try {
                    var writer = new HierarchyClassWriter(context);
                    boolean writeOk = true;

                    try {
                        clazz.core().accept(writer);
                    } catch (Exception e) {
                        writeOk = false;
                        if ((context.writerFlags() & ClassWriter.COMPUTE_FRAMES) != 0) {
                            var fallbackWriter = new HierarchyClassWriter(context, ClassWriter.COMPUTE_MAXS);
                            try {
                                clazz.core().accept(fallbackWriter);
                                writer = fallbackWriter;
                                writeOk = true;
                            } catch (Exception fe) {
                                Logger.error("Error writing class %s", clazz.name());
                                fe.printStackTrace();
                            }
                        } else {
                            Logger.error("Error writing class %s", clazz.name());
                            e.printStackTrace();
                        }
                    }

                    if (!writeOk) continue;

                    var classBytes = writer.toByteArray();
                    context.hashIntegrityClass().add(clazz, classBytes);

                    jos.putNextEntry(new ZipEntry(clazz.name() + ".class"));
                    jos.write(classBytes);
                    jos.closeEntry();
                } catch (MethodTooLargeException e) {
                    e.printStackTrace();
                    System.err.printf("Method size: %s%n", e.getCodeSize());
                }
            }

            context.hashIntegrityClass().addResource();
            context.resourceHandler().handle(jos);
        } catch (IOException e) {
            Logger.error("Error writing output JAR: %s", e);
        }

        var inFile = new File(context.in());
        Logger.exportResult(
                context.in(),  Utils.bytesToKB(inFile.length()),
                context.out(), Utils.bytesToKB(outputFile.length())
        );
    }
}

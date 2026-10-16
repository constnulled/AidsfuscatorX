package dev.lvstrng.aidsfuscator.context.pipeline.postprocess.impl;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.pipeline.IProcessor;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;

import java.util.ArrayList;

public class AidsfuscatorAnnotationProcessor implements IProcessor {
    @Override
    public void run(Context context) {
        var toRemove = new ArrayList<JClass>();

        for(var clazz : context.classes()) {
            if(clazz.name().startsWith("dev/lvstrng/aidsfuscator/api/")) {
                toRemove.add(clazz);
                continue;
            }

            clazz.annotations().stream().filter(e -> e.desc.startsWith("Ldev/lvstrng/aidsfuscator/api/")).forEach(e -> {
                clazz.removeAnnotation(e.desc);
            });

            for(var field : clazz.fields()) {
                field.annotations().stream().filter(e -> e.desc.startsWith("Ldev/lvstrng/aidsfuscator/api/")).forEach(e -> {
                    field.removeAnnotation(e.desc);
                });
            }

            for(var method : clazz.methods()) {
                method.annotations().stream().filter(e -> e.desc.startsWith("Ldev/lvstrng/aidsfuscator/api/")).forEach(e -> {
                    method.removeAnnotation(e.desc);
                });
            }
        }

        toRemove.forEach(e -> context.classMap().remove(e.name()));
    }
}

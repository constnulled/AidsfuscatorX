package dev.lvstrng.aidsfuscator.context.order;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.transform.impl.salt.ClassSaltTransformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.Pair;

import java.util.HashSet;
import java.util.Set;

public class ClassInitOrderHandler {
    private final Context context;
    private final Set<Pair<JClass, JClass>> pairs; // before -> after

    public ClassInitOrderHandler(Context context) {
        this.context = context;
        this.pairs = new HashSet<>();
    }

    public void add(String before, String after) {
        before = before.replace('.', '/');
        after = after.replace('.', '/');

        var finalBefore = before;
        if(context.jarClasses().stream().noneMatch(e -> e.name().equals(finalBefore))) {
            Logger.warn("Skipping classInitOrder statement for pair (%s -> %s): %s class is not found or isn't in JAR classes list", before, after, before);
            return;
        }

        var finalAfter = after;
        if(context.jarClasses().stream().noneMatch(e -> e.name().equals(finalAfter))) {
            Logger.warn("Skipping classInitOrder statement for pair (%s -> %s): %s class is not found or isn't in JAR classes list", before, after, after);
            return;
        }

        var firstClass = context.forName(before);
        var secondClass = context.forName(after);

        secondClass.setFirstInitializerClass(firstClass);
        firstClass.initializes().add(secondClass);
        pairs.add(new Pair<>(firstClass, secondClass));
    }

    public Set<Pair<JClass, JClass>> pairs() {
        return pairs;
    }
}

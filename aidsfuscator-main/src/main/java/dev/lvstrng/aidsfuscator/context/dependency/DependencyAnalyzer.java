package dev.lvstrng.aidsfuscator.context.dependency;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.context.hierarchy.IHierarchy;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DependencyAnalyzer {
    private final Context context;

    private final Set<String> visited = new HashSet<>();
    private final Set<String> resolvedMain = new LinkedHashSet<>();
    private final Set<String> resolvedLibrary = new LinkedHashSet<>();
    private final Map<String, Set<String>> missing = new LinkedHashMap<>();

    public DependencyAnalyzer(Context context) {
        this.context = context;
    }

    public DependencyReport analyze() {
        visited.clear();
        resolvedMain.clear();
        resolvedLibrary.clear();
        missing.clear();

        var mainClasses = context.jarClasses();
        for (var clazz : mainClasses) {
            resolve(clazz.name(), clazz.name());
        }

        return new DependencyReport(mainClasses.size(), resolvedMain, resolvedLibrary, missing);
    }

    private void resolve(String className, String referencedFrom) {
        if (className == null || className.isEmpty() || className.equals(IHierarchy.object))
            return;

        var clazz = context.findByName(className);
        if (clazz == null) {
            missing.computeIfAbsent(className, k -> new LinkedHashSet<>()).add(referencedFrom);
            return;
        }

        if (!visited.add(className))
            return;

        if (clazz.isLibrary())
            resolvedLibrary.add(className);
        else
            resolvedMain.add(className);

        resolve(clazz.superName(), className);

        var interfaces = clazz.interfaces();
        if (interfaces != null)
            for (var itf : interfaces)
                resolve(itf, className);
    }
}

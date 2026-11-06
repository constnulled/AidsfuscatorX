package dev.lvstrng.aidsfuscator.context.dependency;

import dev.lvstrng.aidsfuscator.log.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DependencyReport {
    private final int classesScanned;
    private final Set<String> resolvedMainClasses;
    private final Set<String> resolvedLibraryClasses;
    private final Map<String, Set<String>> missingClasses;

    public DependencyReport(int classesScanned,
                             Set<String> resolvedMainClasses,
                             Set<String> resolvedLibraryClasses,
                             Map<String, Set<String>> missingClasses) {
        this.classesScanned = classesScanned;
        this.resolvedMainClasses = Collections.unmodifiableSet(resolvedMainClasses);
        this.resolvedLibraryClasses = Collections.unmodifiableSet(resolvedLibraryClasses);
        this.missingClasses = Collections.unmodifiableMap(missingClasses);
    }

    public int classesScanned() {
        return classesScanned;
    }

    public Set<String> resolvedMainClasses() {
        return resolvedMainClasses;
    }

    public Set<String> resolvedLibraryClasses() {
        return resolvedLibraryClasses;
    }

    public Map<String, Set<String>> missingClasses() {
        return missingClasses;
    }

    public boolean hasMissingDependencies() {
        return !missingClasses.isEmpty();
    }

    public void log() {
        Logger.info("Dependency analysis: %d classes scanned, %d resolved from main jar, %d resolved from libraries",
                classesScanned, resolvedMainClasses.size(), resolvedLibraryClasses.size());

        if (!hasMissingDependencies()) {
            Logger.success("All class hierarchies fully resolved.");
            return;
        }

        Logger.warn("Found %d unresolved class%s in the hierarchy. Obfuscation may produce invalid bytecode (bad renames, casts or invoke resolution) for the affected classes.",
                missingClasses.size(), missingClasses.size() == 1 ? "" : "es");

        for (var entry : missingClasses.entrySet()) {
            var referees = entry.getValue();
            var preview = referees.stream().limit(3).reduce((a, b) -> a + ", " + b).orElse("");
            var suffix = referees.size() > 3 ? " (+" + (referees.size() - 3) + " more)" : "";
            Logger.warn("  - %s is missing, referenced by %s%s", entry.getKey(), preview, suffix);
        }

        Logger.warn("Add the jars providing the classes above to your libs folder to resolve this.");
    }
}

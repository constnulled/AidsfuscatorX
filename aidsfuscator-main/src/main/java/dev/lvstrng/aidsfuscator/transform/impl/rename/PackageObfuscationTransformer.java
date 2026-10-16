package dev.lvstrng.aidsfuscator.transform.impl.rename;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.naming.Mapping;
import dev.lvstrng.aidsfuscator.naming.Mappings;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;

public class PackageObfuscationTransformer extends Transformer {

    public enum Mode { FLATTEN, EXPLODE }

    private final Setting<String> mode = setting("mode", "FLATTEN");
    private final Setting<String> flatPackage = setting("flatPackage", "");
    private final Setting<Integer> explodeDepth = setting("explodeDepth", 6);
    private final Setting<Boolean> mixinSafe = setting("mixinSafe", true);

    public PackageObfuscationTransformer() {
        super("Package Obfuscation", "PackageObfuscateTransformer");
    }

    @Override
    public void transform(Context context) {
        var resolvedMode = Mode.valueOf(mode.value().toUpperCase());

        for (var clazz : context.classes()) {
            if (Exclusions.PACKAGE_OBFUSCATION.excluded(clazz))
                continue;

            if (mixinSafe.value() && clazz.isMixin())
                continue;

            var currentName = clazz.name();
            var simpleName = simpleName(currentName);

            var newName = switch (resolvedMode) {
                case FLATTEN -> flatten(simpleName);
                case EXPLODE -> explode(context, simpleName);
            };

            if (newName.equals(currentName))
                continue;

            Mappings.CLASS.register(currentName, new Mapping(newName, newName));

            if (clazz.core().sourceFile != null)
                clazz.setSourceFile(simpleName(newName) + ".java");

            markChange();
        }

        remap(context);
        Mappings.CLASS.clearTemp();
    }

    private String simpleName(String internalName) {
        int slash = internalName.lastIndexOf('/');
        return slash == -1 ? internalName : internalName.substring(slash + 1);
    }

    private String flatten(String simpleName) {
        var pkg = flatPackage.value().replace('.', '/');
        if (pkg.isEmpty())
            return simpleName;
        return pkg + "/" + simpleName;
    }

    private String explode(Context context, String simpleName) {
        int depth = Math.max(1, explodeDepth.value());
        var sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append(context.dictionary().newClassName());
            sb.append('/');
        }
        sb.append(simpleName);
        return sb.toString();
    }
}
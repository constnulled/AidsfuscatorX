package dev.lvstrng.aidsfuscator.exclude.preset;

import dev.lvstrng.aidsfuscator.exclude.IExclusionPreset;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusion;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;

public class MinecraftFabricExclusionPreset implements IExclusionPreset {
    private static final String MIXIN = "org/spongepowered/asm/mixin/Mixin";
    private static final String SHADOW = "org/spongepowered/asm/mixin/Shadow";
    private static final String OVERWRITE = "org/spongepowered/asm/mixin/Overwrite";
    private static final String ACCESSOR = "org/spongepowered/asm/mixin/gen/Accessor";
    private static final String INVOKER = "org/spongepowered/asm/mixin/gen/Invoker";

    @Override
    public void load() {
        Exclusions.CLASS_SALTING.addAnnotation(MIXIN);
        Exclusions.METHOD_SALTING.addAnnotation(MIXIN);
        Exclusions.FLOW_FLATTEN.addAnnotation(MIXIN);
        Exclusions.FLOW_SHUFFLE.addAnnotation(MIXIN);
        Exclusions.INTEGER_ENCRYPTION.addAnnotation(MIXIN);
        Exclusions.STRING_ENCRYPTION.addAnnotation(MIXIN);
        Exclusions.FIX_CONSTANTS.addAnnotation(MIXIN);
        Exclusions.REFERENCE_OBFUSCATE.addAnnotation(MIXIN);
        Exclusions.LINE_NUMBERS.addAnnotation(MIXIN);
        Exclusions.LOCAL_NAMES.addAnnotation(MIXIN);
        Exclusions.TRIM.addAnnotation(MIXIN);
        Exclusions.FLOW_INTS.addAnnotation(MIXIN);

        Exclusions.RENAME_METHOD.addAnnotation(SHADOW);
        Exclusions.RENAME_METHOD.addAnnotation(OVERWRITE);
        Exclusions.RENAME_METHOD.addAnnotation(ACCESSOR);
        Exclusions.RENAME_METHOD.addAnnotation(INVOKER);

        Exclusions.RENAME_FIELD.addAnnotation(SHADOW);
        Exclusions.HASH_INTEGRITY.addAnnotation(MIXIN);
    }
}

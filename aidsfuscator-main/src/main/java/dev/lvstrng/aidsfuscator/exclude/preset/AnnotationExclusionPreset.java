package dev.lvstrng.aidsfuscator.exclude.preset;

import dev.lvstrng.aidsfuscator.api.ExcludeGlobal;
import dev.lvstrng.aidsfuscator.api.data.*;
import dev.lvstrng.aidsfuscator.api.args.*;
import dev.lvstrng.aidsfuscator.api.dynamic.*;
import dev.lvstrng.aidsfuscator.api.flow.*;
import dev.lvstrng.aidsfuscator.api.integrity.*;
import dev.lvstrng.aidsfuscator.api.optimize.*;
import dev.lvstrng.aidsfuscator.api.rename.*;
import dev.lvstrng.aidsfuscator.api.salt.*;
import dev.lvstrng.aidsfuscator.api.strip.*;
import dev.lvstrng.aidsfuscator.api.shuffle.*;
import dev.lvstrng.aidsfuscator.exclude.IExclusionPreset;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;

/**
 * Default exclusion preset
 */
public class AnnotationExclusionPreset implements IExclusionPreset {
    @Override
    public void load() {
        Exclusions.GLOBAL.addClass("dev/lvstrng/aidsfuscator/api/*");
        Exclusions.GLOBAL.addAnnotation(internal(ExcludeGlobal.class));

        Exclusions.RENAME_CLASS.addAnnotation(internal(ExcludeClassRename.class));
        Exclusions.RENAME_FIELD.addAnnotation(internal(ExcludeFieldRename.class));
        Exclusions.RENAME_METHOD.addAnnotation(internal(ExcludeMethodRename.class));

        Exclusions.LOCAL_NAMES.addAnnotation(internal(ExcludeLocalVariableNames.class));
        Exclusions.LINE_NUMBERS.addAnnotation(internal(ExcludeLineNumbers.class));
        Exclusions.SOURCE_FILE.addAnnotation(internal(ExcludeSourceFile.class));
        Exclusions.TRIM.addAnnotation(internal(ExcludeTrim.class));

        Exclusions.METHOD_SALTING.addAnnotation(internal(ExcludeMethodSalting.class));
        Exclusions.CLASS_SALTING.addAnnotation(internal(ExcludeClassSalting.class));
        Exclusions.PARAMETER_OBFUSCATE.addAnnotation(internal(ExcludeMethodParameter.class));
        // PARAMETERS

        Exclusions.REFERENCE_OBFUSCATE.addAnnotation(internal(ExcludeReferenceObfuscation.class));
        Exclusions.FIX_CONSTANTS.addAnnotation(internal(ExcludeConstantFix.class));
        Exclusions.INTEGER_ENCRYPTION.addAnnotation(internal(ExcludeIntegerEncryption.class));
        Exclusions.LONG_ENCRYPTION.addAnnotation(internal(ExcludeLongEncryption.class));
        Exclusions.STRING_ENCRYPTION.addAnnotation(internal(ExcludeStringEncryption.class));
        
        Exclusions.OPAQUE_PREDICATES.addAnnotation(internal(ExcludeOpaquePredicates.class));
        Exclusions.FLOW_FLATTEN.addAnnotation(internal(ExcludeFlattening.class));
        Exclusions.FLOW_SHUFFLE.addAnnotation(internal(ExcludeShuffling.class));
        Exclusions.SHUFFLE_MEMBERS.addAnnotation(internal(ExcludeShuffleMembers.class));

        // INTEGRITY
        Exclusions.HASH_INTEGRITY.addAnnotation(internal(ExcludeHashIntegrity.class));
    }
}

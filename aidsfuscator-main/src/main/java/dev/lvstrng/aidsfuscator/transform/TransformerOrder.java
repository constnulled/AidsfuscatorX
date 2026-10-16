package dev.lvstrng.aidsfuscator.transform;

import dev.lvstrng.aidsfuscator.transform.impl.data.*;
import dev.lvstrng.aidsfuscator.transform.impl.args.*;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.*;
import dev.lvstrng.aidsfuscator.transform.impl.data.longs.*;
import dev.lvstrng.aidsfuscator.transform.impl.flow.flowInts.*;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.*;
import dev.lvstrng.aidsfuscator.transform.impl.dynamic.*;
import dev.lvstrng.aidsfuscator.transform.impl.indirection.*;
import dev.lvstrng.aidsfuscator.transform.impl.flow.*;
import dev.lvstrng.aidsfuscator.transform.impl.vm.*;
import dev.lvstrng.aidsfuscator.transform.impl.integrity.*;
import dev.lvstrng.aidsfuscator.transform.impl.optimize.*;
import dev.lvstrng.aidsfuscator.transform.impl.rename.*;
import dev.lvstrng.aidsfuscator.transform.impl.misc.*;
import dev.lvstrng.aidsfuscator.transform.impl.salt.*;
import dev.lvstrng.aidsfuscator.transform.impl.shuffle.*;
import dev.lvstrng.aidsfuscator.transform.impl.strip.*;

import java.util.Comparator;
import java.util.List;

public final class TransformerOrder {

    private static final List<Transformer> transformers = List.of(
            new TrimTransformer(),

            new FieldRenameTransformer(),
            new MethodRenameTransformer(),
            new ClassRenameTransformer(),
            new PackageObfuscationTransformer(),

            new LocalVariableNameTransformer(),
            new LineNumberTransformer(),
            new SourceFileTransformer(),
            new MethodSaltTransformer(),
            new ClassSaltTransformer(),
            new MethodParameterChangerTransformer(),
            new MethodParameterObfuscationTransformer(),
            
            new IntegerEncryptTransformer(),
            new LongEncryptTransformer(),
            new StringEncryptTransformer(),
            new VirtualizerTransformer(),
            new InvokeDynamicConstantTransformer(),
            new FlowIntsTransformer(),
            
            new OpaquePredicateTransformer(),
            new ControlFlowFlatteningTransformer(),
            new ControlFlowShufflingTransformer(),
            new DeadCodeCleanTransformer(),
            new MethodHandleObfuscationTransformer(),
            new ReferenceObfuscationTransformer(),
            new ShuffleMembersTransformer(),
            new HashIntegrityTransformer(),
            new WatermarkTransformer()
    );

    @SuppressWarnings("unchecked")
    public <T> T get(Class<Transformer> clazz) {
        return (T) transformers.stream()
                .filter(clazz::isInstance)
                .findFirst().orElse(null);
    }

    public List<Transformer> sorted(List<Transformer> transformers) {
        transformers.sort(Comparator.comparingInt(TransformerOrder.transformers::indexOf));
        return transformers;
    }

    public static List<Transformer> transformers() {
        return transformers;
    }
}

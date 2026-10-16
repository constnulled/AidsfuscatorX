package dev.test;

import dev.lvstrng.aidsfuscator.classgen.impl.hash.BasicHashIntegrityClass;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.impl.data.ConstantsFixTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.integers.IntegerEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.StringEncryptTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.dynamic.*;
import dev.lvstrng.aidsfuscator.transform.impl.flow.*;
import dev.lvstrng.aidsfuscator.transform.impl.vm.VirtualizerTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.flow.flowInts.FlowIntsTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.integrity.HashIntegrityTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.optimize.*;
import dev.lvstrng.aidsfuscator.transform.impl.rename.*;
import dev.lvstrng.aidsfuscator.transform.impl.salt.*;
import dev.lvstrng.aidsfuscator.transform.impl.strip.*;
import dev.test.transform.*;

public class TestMain {
    public static void main(String[] args) {
        var context = Context.newInstance()
                .computeFrames()
                .in("in.jar")
                .libs("libs")
                .out("out.jar")
                .setAggressiveOverload(true);

        context.referenceManager().addFieldCandidate("*");
        context.referenceManager().addMethodCandidate("*");

        Exclusions.GLOBAL.addClass("dev/lvstrng/aidsfuscator/api/*");
        context.run(
                new TrimTransformer(),

                new FieldRenameTransformer(),
                new MethodRenameTransformer(),
                new ClassRenameTransformer(),

                new LocalVariableNameTransformer(),
                new LineNumberTransformer(),
                new MethodSaltTransformer(),
                new ClassSaltTransformer(),

                new ConstantsFixTransformer(),
                new IntegerEncryptTransformer(),
                new StringEncryptTransformer(),
                new InvokeDynamicConstantTransformer(),
                new FlowIntsTransformer(),

                new ControlFlowFlatteningTransformer(),
                new ControlFlowShufflingTransformer(),
                new VirtualizerTransformer(),
                new DeadCodeCleanTransformer(),
                new ReferenceObfuscationTransformer(),
                new StringEncryptTransformer(),
                new HashIntegrityTransformer()
        );
    }
}

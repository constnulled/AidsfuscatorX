package dev.lvstrng.aidsfuscator.transform.impl.vm;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class VirtualizerTransformer extends Transformer {

    private final Setting<Integer> maxMethodsPerClass = setting("maxMethodsPerClass", 10);
    private final Setting<Boolean> randomizeIsa = setting("randomizeIsa", true);
    private final Setting<Boolean> encryptBytecode = setting("encryptBytecode", true);

    public VirtualizerTransformer() {
        super("Bytecode Virtualizer", "VirtualizerTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.VIRTUALIZER.excluded(clazz)) continue;
            if (clazz.isInterface() || clazz.isAnnotation()) continue;

            var eligible = new ArrayList<JMethod>();
            for (var method : clazz.methods()) {
                if (cantEditMethod(clazz, method)) continue;
                if (Exclusions.VIRTUALIZER.excluded(method)) continue;
                if (VMCompiler.canCompile(method)) eligible.add(method);
            }

            if (eligible.isEmpty()) continue;

            var isaTable = generateIsaTable();
            var allUsedOpcodes = new HashSet<VMOpcode>();
            var compiledMethods = new HashMap<JMethod, VMCompiler.VMCompiledMethod>();

            int count = 0;
            for (var method : eligible) {
                if (count >= maxMethodsPerClass.value()) break;
                int xorKey = encryptBytecode.value() ? (random.nextInt(254) + 1) : 0;
                var compiled = VMCompiler.compile(method, isaTable, xorKey);
                allUsedOpcodes.addAll(compiled.usedOpcodes());
                compiledMethods.put(method, compiled);
                count++;
            }

            if (compiledMethods.isEmpty()) continue;

            allUsedOpcodes.add(VMOpcode.NOP);
            allUsedOpcodes.add(VMOpcode.RETURN_VOID);
            allUsedOpcodes.add(VMOpcode.RETURN_VAL);

            var vmMethodName = context.dictionary().newMethodName(clazz,
                    "([B[Ljava/lang/Object;[Ljava/lang/Object;I)Ljava/lang/Object;");
            var vmMethod = clazz.createMethod(
                    ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                    vmMethodName,
                    "([B[Ljava/lang/Object;[Ljava/lang/Object;I)Ljava/lang/Object;");
            VMInterpreterBuilder.build(vmMethod, isaTable, allUsedOpcodes);

            for (var entry : compiledMethods.entrySet()) {
                VMCallSiteRewriter.rewrite(clazz, entry.getKey(), vmMethod, entry.getValue());
                markChange();
            }
        }
    }

    private Map<VMOpcode, Integer> generateIsaTable() {
        var opcodes = Arrays.asList(VMOpcode.values());
        var numbers = new ArrayList<Integer>();
        for (int i = 0; i < opcodes.size(); i++) numbers.add(i);
        if (randomizeIsa.value()) Collections.shuffle(numbers, random);
        var table = new HashMap<VMOpcode, Integer>();
        for (int i = 0; i < opcodes.size(); i++) table.put(opcodes.get(i), numbers.get(i));
        return table;
    }
}

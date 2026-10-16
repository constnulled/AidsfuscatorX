package dev.lvstrng.aidsfuscator.transform.impl.flow.flowInts;

import dev.lvstrng.aidsfuscator.analysis.flow.graph.Block;
import dev.lvstrng.aidsfuscator.analysis.flow.graph.ControlFlowGraph;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import dev.lvstrng.aidsfuscator.utils.SwitchUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.*;
import java.util.function.Function;

/**
 * A transformer that adds an {@code int} variable to the code. This variable gets updated in the program's control flow.
 * Since these values are harder to track than regular variables (because they're being updated each block), aidsfuscator uses this variable as another layer of number obfuscation.
 */
public class FlowIntsTransformer extends Transformer {
    private final Setting<Boolean> obfuscateLongs = setting("obfuscateLongs", true);
    private final Setting<Boolean> separateBlocks  = setting("separateBlocks", true);
    private final Setting<Boolean> confuseFlow     = setting("confuseFlow", true);

    public FlowIntsTransformer() {
        super("Flow Ints", "FlowIntsTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.FLOW_INTS.excluded(clazz))
                continue;

            for (var method : clazz.methods()) {
                if (Exclusions.FLOW_INTS.excluded(method))
                    continue;

                if (ASMUtils.codeSize(method) > 20000)
                    continue;

                modify(context, method);
                markChange();
            }
        }
    }

    private void modify(Context context, JMethod method) {
        var graph = method.createFlowGraph(context);
        if (graph.isEmpty())
            return;

        if (!graph.firstBlock().predecessors().isEmpty()) {
            method.insns().insert(new LabelNode());
            graph = method.createFlowGraph(context);
        }

        var variable    = method.allocVar(Type.INT_TYPE);
        var blockValues = new HashMap<Block, Integer>();
        initializeValues(graph, blockValues);

        for (var block : graph.blocks()) {
            var list             = new InsnList();
            var predecessorValue = blockValues.get(block.predecessors().stream().findFirst().orElse(null));
            var value            = blockValues.get(block);
            if (value == null)
                continue;

            if (!Objects.equals(value, predecessorValue)) {
                if (predecessorValue == null) {
                    if (method.hasSalt()) {
                        var masked = method.salt().value() | method.seed();
                        var xor    = masked ^ value;

                        list.add(method.salt().load());
                        list.add(context.properties().add(ASMUtils.pushInt(method.seed()), Property.IGNORE_INTEGER));
                        list.add(new InsnNode(IOR));
                        list.add(ASMUtils.pushInt(xor));
                        list.add(new InsnNode(IXOR));
                    } else {
                        list.add(context.properties().add(ASMUtils.pushInt(value), Property.SENSITIVE_CONSTANT));
                    }
                    list.add(new VarInsnNode(ISTORE, variable));
                } else {
                    if (!confuseFlow.value() || random.nextInt(100) >= 10) {
                        list.add(context.properties().add(new VarInsnNode(ILOAD, variable), Property.IGNORE_VAR_USAGE));
                        list.add(context.properties().add(ASMUtils.pushInt(value ^ predecessorValue), Property.IGNORE_INTEGER));
                        list.add(new InsnNode(IXOR));
                        list.add(new VarInsnNode(ISTORE, variable));
                    } else createFlowConfuser(context, list, variable, predecessorValue, value);
                }
            }

            if (separateBlocks.value()) list.add(new LabelNode());
            method.insns().insert(block.label(), list);

            obfuscateBlockValues(context, method, block, variable, value);
        }

        method.reinitUnsafeInstructions();
    }

    private void createFlowConfuser(Context context, InsnList list, int variable, int predecessorValue, int value) {
        var switchLabel = new LabelNode();
        var targetLabel = new LabelNode();
        var fakeLabel1  = new LabelNode();
        var fakeLabel2  = new LabelNode();
        var exitLabel   = new LabelNode();

        var targetBuilder = new InsnBuilder()
                .label(targetLabel)
                ._var(ILOAD, variable).addProps(context, Property.IGNORE_VAR_USAGE)
                ._int(value ^ predecessorValue).addProps(context, Property.IGNORE_INTEGER)
                .ixor()
                ._var(ISTORE, variable)
                ._goto(exitLabel);

        var fake1Builder = new InsnBuilder()
                .label(fakeLabel1)
                ._var(ILOAD, variable).addProps(context, Property.IGNORE_VAR_USAGE)
                ._int(random.nextInt()).addProps(context, Property.IGNORE_INTEGER)
                .ixor()
                ._var(ISTORE, variable)
                ._goto(exitLabel);

        var fake2Builder = new InsnBuilder()
                .label(fakeLabel2)
                ._var(ILOAD, variable).addProps(context, Property.IGNORE_VAR_USAGE)
                ._int(random.nextInt()).addProps(context, Property.IGNORE_INTEGER)
                .ixor()
                ._var(ISTORE, variable)
                ._goto(switchLabel);

        var cases  = Map.of(
                predecessorValue, targetLabel,
                random.nextInt(), fakeLabel1,
                random.nextInt(), fakeLabel2
        );
        var labels = cases.values().stream().toList();

        list.add(switchLabel);
        list.add(new VarInsnNode(ILOAD, variable));
        list.add(SwitchUtils.createLookup(labels.get(random.nextInt(labels.size())), cases));

        var builders = new ArrayList<>(List.of(targetBuilder, fake1Builder, fake2Builder));
        Collections.shuffle(builders);
        for (var builder : builders) {
            list.add(builder.result());
        }
        list.add(exitLabel);
    }

    private void initializeValues(ControlFlowGraph graph, Map<Block, Integer> blockValues) {
        var parent     = new HashMap<Block, Block>();
        var rootValues = new HashMap<Block, Integer>();

        for (var block : graph.blocks()) {
            parent.put(block, block);
        }

        var find = new Function<Block, Block>() {
            @Override
            public Block apply(Block block) {
                var p = parent.get(block);
                if (p != block) {
                    p = this.apply(p);
                    parent.put(block, p);
                }
                return p;
            }
        };

        for (var successor : graph.blocks()) {
            var preds = successor.predecessors();
            if (preds.size() < 2)
                continue;

            var first     = preds.getFirst();
            var firstRoot = find.apply(first);

            for (var pred : preds) {
                var root = find.apply(pred);
                if (root != firstRoot)
                    parent.put(root, firstRoot);
            }
        }

        for (var block : graph.blocks()) {
            var root = find.apply(block);
            rootValues.putIfAbsent(root, random.nextInt());
            blockValues.put(block, rootValues.get(root));
        }
    }

    private void obfuscateBlockValues(Context context, JMethod method, Block block, int variable, int value) {
        for (var insn : block.insns()) {
            if (method.isUnsafe(insn))
                continue;

            if (ASMUtils.isIntPush(insn)) {
                if (ASMUtils.isIconst(insn))
                    continue;

                if (context.properties().get(insn).has(Property.IGNORE_FLOW_INTS))
                    continue;

                var num    = ASMUtils.getInt(insn);
                var masked = value | method.seed();

                var list = new InsnList();
                list.add(new VarInsnNode(ILOAD, variable));
                list.add(ASMUtils.pushInt(method.seed()));
                list.add(new InsnNode(IOR));
                list.add(ASMUtils.pushInt(masked ^ num));
                list.add(new InsnNode(IXOR));

                method.insns().insertBefore(insn, list);
                method.insns().remove(insn);
            } else if (ASMUtils.isLongPush(insn) && obfuscateLongs.value()) {
                if (ASMUtils.isLconst(insn))
                    continue;

                if (context.properties().get(insn).has(Property.IGNORE_FLOW_INTS))
                    continue;

                var num = ASMUtils.getLong(insn);
                long obf = ((num << 32 >>> 32) ^ (value & 0xFFFFFFFFL)) | (((num >>> 32) ^ (value & 0xFFFFFFFFL)) << 32);

                var builder = new InsnBuilder()
                        .add(context.properties().add(ASMUtils.pushLong(obf), Property.IGNORE_LONG))
                        .dup2()
                        ._int(32)
                        .lshl()
                        ._int(32)
                        .lushr()
                        ._var(ILOAD, variable)
                        .i2l()
                        .add(context.properties().add(ASMUtils.pushLong(0xFFFFFFFFL), Property.IGNORE_LONG))
                        .land()
                        .lxor()
                        .dup2_x2()

                        .pop2()
                        ._int(32)
                        .lushr()
                        ._var(ILOAD, variable)
                        .i2l()
                        .add(context.properties().add(ASMUtils.pushLong(0xFFFFFFFFL), Property.IGNORE_LONG))
                        .land()
                        .lxor()
                        ._int(32)
                        .lshl()
                        .lor();

                method.insns().insertBefore(insn, builder.result());
                method.insns().remove(insn);
            }
        }
    }
}

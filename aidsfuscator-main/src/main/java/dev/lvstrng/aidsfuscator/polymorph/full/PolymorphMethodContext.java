package dev.lvstrng.aidsfuscator.polymorph.full;

import dev.lvstrng.aidsfuscator.polymorph.full.args.ArgumentBuilder;
import dev.lvstrng.aidsfuscator.polymorph.full.integer.VarXorIntMask;
import dev.lvstrng.aidsfuscator.polymorph.full.strings.DoWhileLoopStringMask;
import dev.lvstrng.aidsfuscator.polymorph.full.strings.ForLoopStringMask;
import org.objectweb.asm.tree.InsnList;

import java.util.*;
import java.util.function.Supplier;

/**
 * Polymorph context used for string encryption. Currently only supports category 1 values.
 * @author lvstrng
 */
public class PolymorphMethodContext {
    public static final Random random = new Random();
    private int traceVar, indexVar, charArrVar;
    private int traceValue;
    private final int traceXorKey, indexXorKey;

    private final int maxMasks, maxSubMasks;
    private int maxLocals;

    private final ArgumentBuilder args;
    private final List<PolyMask<String>> masks = new ArrayList<>();
    private final Map<Integer, Integer> values = new HashMap<>();
    @SuppressWarnings("all")
    private final List<PolyMask<Integer>> allImportMasks = new ArrayList<>();
    private final List<PolyMask<Integer>> unusedImportantMasks = new ArrayList<>();

    public PolymorphMethodContext(int maxMasks, int maxSubMasks) {
        this.args = new ArgumentBuilder();

        this.maxMasks = maxMasks;
        this.maxSubMasks = maxSubMasks;

        this.traceXorKey = random.nextInt(Character.MAX_VALUE);
        this.indexXorKey = random.nextInt();
    }

    public PolymorphMethodContext initialize() {
        this.args.generate(this);

        this.traceVar = args.allocVar(1);
        this.indexVar = args.allocVar(1);
        this.charArrVar = args.allocVar(1);
        this.maxLocals = args.getFreeLocal();

        List<Supplier<PolyMask<String>>> allMasks = List.of(
                () -> new ForLoopStringMask(this, maxSubMasks),
                () -> new DoWhileLoopStringMask(this, maxSubMasks)
        );

        for(var arg : args.list()) {
            var mask = new VarXorIntMask(this, args.slot(arg));
            unusedImportantMasks.add(mask);
            allImportMasks.add(mask);
        }
        var traceMask = new VarXorIntMask(this, traceVar);
        unusedImportantMasks.add(traceMask);
        allImportMasks.add(traceMask);

        int n = random.nextInt(1, maxMasks + 1);
        for(int i = 0; i < n; i++) {
            masks.add(allMasks.get(random.nextInt(allMasks.size())).get());
        }

        while (!unusedImportantMasks.isEmpty()) {
            var parent = masks.get(random.nextInt(masks.size()));
            if(parent instanceof ForLoopStringMask f)
                f.childMasks.add(randomUnusedMask());
            else if (parent instanceof DoWhileLoopStringMask d)
                d.childMasks.add(randomUnusedMask());
        }
        return this;
    }

    public List<PolyMask<Integer>> unusedArgs() {
        return unusedImportantMasks;
    }

    public PolyMask<Integer> randomUnusedMask() {
        if(unusedImportantMasks.isEmpty())
            return null;

        var mask = unusedImportantMasks.get(random.nextInt(unusedImportantMasks.size()));
        unusedImportantMasks.remove(mask);
        return mask;
    }

    public void setValues(int traceValue, Map<Integer, Integer> values) {
        this.traceValue = traceValue;
        this.values.putAll(values);
        this.values.put(traceVar, traceValue);
    }

    public InsnList instructions() {
        var list = new InsnList();
        for(var mask : masks) {
            list.add(mask.instructions());
        }

        return list;
    }

    public int varValue(VariableIntValue v) {
        return values.get(v.variableSlot());
    }

    public String encrypt(String input) {
        for(var mask : masks.reversed()) {
            input = mask.applyInverse(input);
        }

        return input;
    }

    public String decrypt(String input) {
        for(var mask : masks) {
            input = mask.apply(input);
        }

        return input;
    }

    public ArgumentBuilder args() {
        return args;
    }

    public void setCharArrVar(int charArrVar) {
        this.charArrVar = charArrVar;
    }

    public void setIndexVar(int indexVar) {
        this.indexVar = indexVar;
    }

    public void setTraceVar(int traceVar) {
        this.traceVar = traceVar;
    }

    public int traceVar() {
        return traceVar;
    }

    public int indexVar() {
        return indexVar;
    }

    public int charArrVar() {
        return charArrVar;
    }

    public int traceXorKey() {
        return traceXorKey;
    }

    public int indexXorKey() {
        return indexXorKey;
    }

    public int maxLocals() {
        return maxLocals;
    }
}

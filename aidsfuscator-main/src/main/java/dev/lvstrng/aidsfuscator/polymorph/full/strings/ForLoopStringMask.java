package dev.lvstrng.aidsfuscator.polymorph.full.strings;

import dev.lvstrng.aidsfuscator.polymorph.full.*;
import dev.lvstrng.aidsfuscator.polymorph.full.integer.*;
import dev.lvstrng.aidsfuscator.utils.CryptUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ForLoopStringMask extends PolyMask<String> {
    public final List<PolyMask<Integer>> childMasks;

    public ForLoopStringMask(PolymorphMethodContext context, int maxMasks) {
        super(context);
        this.childMasks = new ArrayList<>();

        List<Supplier<PolyMask<Integer>>> masks = new ArrayList<>(List.of(
                () -> new XorIntMask(context, Character.MIN_VALUE, Character.MAX_VALUE),
                () -> new LoopBasedXorIntMask(context, CryptUtils.generateKeys(random, random.nextInt(4, 32), 255)),
                () -> new LoopXorIntMask(context)
        ));

        var n = random.nextInt(1, maxMasks + 1);
        for(int i = 0; i < n; i++) {
            if(masks.isEmpty())
                break;

            var sup = masks.get(random.nextInt(masks.size()));
            var mask = sup.get();
            if(mask == null) {
                masks.remove(sup);
                sup = masks.get(random.nextInt(masks.size()));
                mask = sup.get();
            }
            if(!(mask instanceof VarXorIntMask))
                masks.remove(sup);

            childMasks.add(mask);
        }
    }

    @Override
    public InsnList instructions() {
        var loopLabel = new LabelNode();
        var exitLabel = new LabelNode();

        var builder = new InsnBuilder()
                .label()
                ._int(0)
                ._var(ISTORE, context.indexVar())

                .label(loopLabel)
                ._var(ILOAD, context.indexVar())
                ._var(ALOAD, context.charArrVar())
                .arraylength()
                .jump(IF_ICMPGE, exitLabel)

                .label()
                ._var(ALOAD, context.charArrVar())
                ._var(ILOAD, context.indexVar())
                ._var(ALOAD, context.charArrVar())
                ._var(ILOAD, context.indexVar())
                .caload();
        // INSERT CHILD MASKS HERE
        for(var mask : childMasks) {
            builder.add(mask.instructions());
        }
        builder
                .i2c()
                .castore()

                .label()
                .iinc(context.indexVar(), 1)

                .label()
                ._goto(loopLabel)

                .label(exitLabel)
        ;

        return builder.result();
    }

    @Override
    public String apply(String obj) {
        var arr = obj.toCharArray();
        for(var mask : childMasks) {
            if(mask instanceof VariableIntValue v) {
                v.provideValue(context.varValue(v));
            }

            if(mask instanceof LoopBasedMask<?> loopMask) {
                // noinspection unchecked
                arr = ((LoopBasedMask<char[]>)loopMask).modify(arr);
            } else {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = (char) ((int) mask.apply((int) arr[i]));
                }
            }
        }

        return new String(arr);
    }

    @Override
    public String applyInverse(String obj) {
        var arr = obj.toCharArray();
        for(var mask : childMasks.reversed()) {
            if(mask instanceof VariableIntValue v) {
                v.provideValue(context.varValue(v));
            }

            if(mask instanceof LoopBasedMask<?> loopMask) {
                // noinspection unchecked
                arr = ((LoopBasedMask<char[]>)loopMask).modifyInverse(arr);
            } else {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = (char) ((int) mask.applyInverse((int) arr[i]));
                }
            }
        }

        return new String(arr);
    }
}
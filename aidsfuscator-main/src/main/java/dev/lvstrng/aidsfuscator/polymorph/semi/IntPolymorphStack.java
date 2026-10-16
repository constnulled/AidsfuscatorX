package dev.lvstrng.aidsfuscator.polymorph.semi;

import dev.lvstrng.aidsfuscator.polymorph.semi.impl.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;

/**
 * Allows for easier managing of polymorphism for integers.
 * @author lvstrng
 */
public class IntPolymorphStack extends Stack<IntMask<?>> {
    private static final List<Supplier<IntMask<?>>> allMasks = List.of(
            AddMask::new,
            SubMask::new,

            MulMask::new,
            DivMask::new,

            UShiftRightMask::new,
            RightShiftMask::new,
            LeftShiftMask::new,

            XorMask::new,
            OrMask::new,
            AndMask::new,

            NegMask::new,

            // Non-linear masks (defeat algebraic/SMT solvers)
            SBoxMask::new,
            FeistelMask::new,
            MBAMask::new,
            ModMulMask::new
    );

    public static IntPolymorphStack of(IntMask<?>... masks) {
        var stack = new IntPolymorphStack();
        for(var mask : masks) {
            stack.push(mask);
        }

        return stack;
    }

    public int apply(int n) {
        for(var mask : this) {
            n = mask.apply(n);
        }

        return n;
    }

    public int applyInverse(int n) {
        for(var mask : this.reversed()) {
            n = mask.applyInverse(n);
        }

        return n;
    }

    public InsnList dump() {
        var ls = new InsnList();
        for(var mask : this) {
            ls.add(mask.insns());
        }

        return ls;
    }

    public InsnList dumpWithInsn(Supplier<AbstractInsnNode> between) {
        var ls = new InsnList();
        for(var mask : this) {
            ls.add(mask.insns());
            ls.add(between.get());
        }

        return ls;
    }

    public InsnList dumpWithList(Supplier<InsnList> between) {
        var ls = new InsnList();
        for(var mask : this) {
            ls.add(mask.insns());
            ls.add(between.get());
        }

        return ls;
    }
}

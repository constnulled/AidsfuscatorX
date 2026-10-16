package dev.lvstrng.aidsfuscator.utils;

import dev.lvstrng.aidsfuscator.analysis.SizeEvaluator;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.Arrays;

public class ASMUtils implements Opcodes {
    public static int getConsumedValueCount(AbstractInsnNode insn, int topSize) {
        return switch (insn.getOpcode()) {
            case NOP,
                 ACONST_NULL,
                 ICONST_M1, 
                 ICONST_0, 
                 ICONST_1, 
                 ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1,
                 FCONST_0, FCONST_1, FCONST_2,
                 DCONST_0, DCONST_1,
                 BIPUSH, SIPUSH,
                 LDC,
                 ILOAD, LLOAD, FLOAD, DLOAD, ALOAD,
                 GETSTATIC,
                 NEW,
                 RET,
                 GOTO,
                 JSR,
                 -1 -> 0;

            case POP,
                 ISTORE, LSTORE, FSTORE, DSTORE, ASTORE,
                 IRETURN, LRETURN, FRETURN, DRETURN, ARETURN,
                 ATHROW,
                 ARRAYLENGTH,
                 MONITORENTER, MONITOREXIT,
                 IFNULL, IFNONNULL,
                 TABLESWITCH, LOOKUPSWITCH,
                 PUTSTATIC,
                 NEWARRAY, ANEWARRAY,
                 CHECKCAST, INSTANCEOF -> 1;

            // Consumes two values
            case POP2,
                 IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD,
                 IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE,
                 PUTFIELD,
                 IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
                 IF_ACMPEQ, IF_ACMPNE -> 2;

            case INEG, LNEG, FNEG, DNEG,
                 I2L, I2F, I2D,
                 L2I, L2F, L2D,
                 F2I, F2L, F2D,
                 D2I, D2L, D2F,
                 I2B, I2C, I2S -> 1;
                 
            case IADD, LADD, FADD, DADD,
                 ISUB, LSUB, FSUB, DSUB,
                 IMUL, LMUL, FMUL, DMUL,
                 IDIV, LDIV, FDIV, DDIV,
                 IREM, LREM, FREM, DREM,
                 ISHL, LSHL,
                 ISHR, LSHR,
                 IUSHR, LUSHR,
                 IAND, LAND,
                 IOR, LOR,
                 IXOR, LXOR,
                 LCMP,
                 FCMPL, FCMPG,
                 DCMPL, DCMPG -> 2;

            case DUP -> 1;
            case DUP_X1 -> 2;
            case DUP_X2 -> 2;
            case DUP2 -> topSize == 2 ? 1 : 2;
            case DUP2_X1 -> topSize == 2 ? 2 : 3;
            case DUP2_X2 -> topSize == 2 ? 2 : 4;
            case SWAP -> 2;

            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE -> 1;

            case MULTIANEWARRAY -> ((MultiANewArrayInsnNode) insn).dims;

            case GETFIELD -> 1;

            case INVOKEVIRTUAL,
                 INVOKESPECIAL,
                 INVOKEINTERFACE -> {
                var method = (MethodInsnNode) insn;
                yield Type.getArgumentTypes(method.desc).length + 1;
            }

            case INVOKESTATIC -> {
                var method = (MethodInsnNode) insn;
                yield Type.getArgumentTypes(method.desc).length;
            }

            case INVOKEDYNAMIC -> {
                var indy = (InvokeDynamicInsnNode) insn;
                yield Type.getArgumentTypes(indy.desc).length;
            }

            default -> throw new IllegalArgumentException(
                    "Unhandled opcode: " + insn.getOpcode());
        };
    }

    public static boolean isVarLoad(AbstractInsnNode insn) {
        var op = insn.getOpcode();
        return op >= ILOAD && op <= ALOAD;
    }

    public static boolean isVarStore(AbstractInsnNode insn) {
        var op = insn.getOpcode();
        return op >= ISTORE && op <= ASTORE;
    }

    public static int codeSize(JMethod method) {
        var eval = new SizeEvaluator();
        method.core().accept(eval);
        return eval.getSize();
    }

    public static boolean isReturn(int opcode) {
        return opcode >= IRETURN && opcode <= RETURN;
    }

    public static boolean isReturn(AbstractInsnNode insn) {
        return isReturn(insn.getOpcode());
    }

    public static AbstractInsnNode pushInt(int n) {
        if(n >= -1 && n <= 5)
            return new InsnNode(ICONST_0 + n);

        if(n >= -128 && n <= 127)
            return new IntInsnNode(BIPUSH, n);
        if(n >= -32768 && n <= 32767)
            return new IntInsnNode(SIPUSH, n);

        return new LdcInsnNode(n);
    }
    
    public static long getLong(AbstractInsnNode insn) {
        if(insn instanceof LdcInsnNode ldc && ldc.cst instanceof Long l)
            return l;

        if(isLconst(insn))
            return insn.getOpcode() - LCONST_0;

        throw new IllegalArgumentException("Not long insn: " + insn.getOpcode());
    }

    public static boolean isLongPush(AbstractInsnNode insn) {
        if(isLconst(insn))
            return true;

        return insn instanceof LdcInsnNode ldc && ldc.cst instanceof Long;
    }

    public static boolean isLconst(AbstractInsnNode insn) {
        return insn.getOpcode() == LCONST_0 || insn.getOpcode() == LCONST_1;
    }

    public static AbstractInsnNode pushLong(long l) {
        if(l == 0 || l == 1)
            return new InsnNode((int) (LCONST_0 + l));

        return new LdcInsnNode(l);
    }

    public static AbstractInsnNode pushDouble(double d) {
        if(d == 0 || d == 1)
            return new InsnNode((int) (DCONST_0 + d));

        return new LdcInsnNode(d);
    }

    public static AbstractInsnNode pushFloat(float f) {
        if(f == 0 || f == 1 || f == 2)
            return new InsnNode((int) (FCONST_0 + f));

        return new LdcInsnNode(f);
    }

    public static int getInt(AbstractInsnNode insn) {
        var op = insn.getOpcode();
        if(isIconst(insn))
            return op - ICONST_0;

        if(op == BIPUSH || op == SIPUSH)
            return ((IntInsnNode) insn).operand;

        if(insn instanceof LdcInsnNode ldc)
            return (int)ldc.cst;

        throw new IllegalArgumentException("Not number insn: " + insn.getOpcode());
    }

    public static boolean isIntPush(AbstractInsnNode insn) {
        if(insn instanceof IntInsnNode node)
            return node.getOpcode() != NEWARRAY;

        if(isIconst(insn))
            return true;

        return insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer;
    }

    public static boolean isIconst(AbstractInsnNode insn) {
        return insn.getOpcode() >= ICONST_M1 && insn.getOpcode() <= ICONST_5;
    }

    public static void translateConcatenation(JMethod method) {
        var STACK_ARG_CONSTANT = '\u0001';
        var BSM_ARG_CONSTANT = '\u0002';

        for(var insn : method.insns()) {
            if(!(insn instanceof InvokeDynamicInsnNode indy))
                continue;

            if(!indy.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory"))
                continue;

            if(!indy.bsm.getName().equals("makeConcatWithConstants"))
                continue;

            var pattern = (String) indy.bsmArgs[0];

            var stackArgs = Type.getArgumentTypes(indy.desc);
            var bsmArgs = Arrays.copyOfRange(indy.bsmArgs, 1, indy.bsmArgs.length);

            int stackArgsCount = 0;
            for(var c : pattern.toCharArray()) {
                if(c == STACK_ARG_CONSTANT)
                    stackArgsCount++;
            }

            int bsmArgsCount = 0;
            for (char c : pattern.toCharArray()) {
                if (c == BSM_ARG_CONSTANT)
                    bsmArgsCount++;
            }

            if(stackArgsCount != stackArgs.length)
                continue;

            if(bsmArgsCount != bsmArgs.length)
                continue;

            var v = method.allocVar(stackArgs[0]);
            var indices = new int[stackArgsCount];

            for(int i = 0; i < stackArgs.length; i++) {
                indices[i] = v;
                v += stackArgs[i].getSize();
            }

            for (int i = indices.length - 1; i >= 0; i--) {
                method.insns().insertBefore(indy, new VarInsnNode(stackArgs[i].getOpcode(ISTORE), indices[i]));
            }

            var list = new InsnList();
            var arr = pattern.toCharArray();

            int stackArgsIndex = 0;
            int bsmArgsIndex = 0;

            var builder = new StringBuilder();
            list.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
            list.add(new InsnNode(DUP));
            list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V"));

            for (char c : arr) {
                if (c == STACK_ARG_CONSTANT) {
                    if (!builder.isEmpty()) {
                        list.add(new LdcInsnNode(builder.toString()));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                        builder = new StringBuilder();
                    }

                    var stackArg = stackArgs[stackArgsIndex++];
                    var stackIndex = indices[stackArgsIndex - 1];

                    if (stackArg.getSort() == Type.OBJECT) {
                        list.add(new VarInsnNode(ALOAD, stackIndex));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                    } else if (stackArg.getSort() == Type.ARRAY) {
                        list.add(new VarInsnNode(ALOAD, stackIndex));
                        list.add(new MethodInsnNode(INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;"));
                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
                    } else {
                        list.add(new VarInsnNode(stackArg.getOpcode(ILOAD), stackIndex));
                        var adaptedDescriptor = stackArg.getDescriptor();
                        if (adaptedDescriptor.equals("B") || adaptedDescriptor.equals("S"))
                            adaptedDescriptor = "I";

                        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(" + adaptedDescriptor + ")Ljava/lang/StringBuilder;"));
                    }
                } else if (c == BSM_ARG_CONSTANT) {
                    list.add(new LdcInsnNode(bsmArgs[bsmArgsIndex++]));
                    list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;"));
                } else {
                    builder.append(c);
                }
            }

            if (!builder.isEmpty()) {
                list.add(new LdcInsnNode(builder.toString()));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
            }

            list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;"));

            method.insns().insertBefore(indy, list);
            method.insns().remove(indy);
        }
    }

    public static AbstractInsnNode box(InsnList list, Type type) {
        MethodInsnNode m;
        switch (type.getSort()) {
            case Type.CHAR -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
            case Type.INT -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
            case Type.BYTE -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
            case Type.SHORT -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
            case Type.FLOAT -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
            case Type.DOUBLE -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
            case Type.LONG -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
            case Type.BOOLEAN -> list.add(m = new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
            default -> m = null;
        }

        return m;
    }

    public static MethodInsnNode unbox(InsnList list, Type type) {
        MethodInsnNode m = null;

        switch (type.getSort()) {
            case Type.CHAR -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Character"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C"));
            }
            case Type.INT -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Integer"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I"));
            }
            case Type.SHORT -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Short"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S"));
            }
            case Type.BYTE -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Byte"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B"));
            }
            case Type.BOOLEAN -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Boolean"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z"));
            }
            case Type.FLOAT -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Float"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F"));
            }
            case Type.DOUBLE -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Double"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D"));
            }
            case Type.LONG -> {
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Long"));
                list.add(m = new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J"));
            }
            case Type.OBJECT, Type.ARRAY -> list.add(new TypeInsnNode(CHECKCAST, type.getInternalName()));
        }

        return m;
    }
}

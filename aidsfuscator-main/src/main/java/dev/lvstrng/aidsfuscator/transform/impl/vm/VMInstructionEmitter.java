package dev.lvstrng.aidsfuscator.transform.impl.vm;

import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public final class VMInstructionEmitter {

    private VMInstructionEmitter() {}

    public static void emitCase(InsnList list, VMOpcode op, LabelNode loopStart) {
        switch (op) {
            case NOP -> list.add(new JumpInsnNode(GOTO, loopStart));

            case CONST_NULL -> {
                pushToStack(list, il -> il.add(new InsnNode(ACONST_NULL)));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case CONST_INT -> {
                list.add(new VarInsnNode(ALOAD, 0));
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
                list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(BIPUSH, 24)); list.add(new InsnNode(ISHL));
                list.add(new VarInsnNode(ALOAD, 0));
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
                list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
                list.add(new IntInsnNode(BIPUSH, 16)); list.add(new InsnNode(ISHL)); list.add(new InsnNode(IOR));
                list.add(new VarInsnNode(ALOAD, 0));
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
                list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
                list.add(new IntInsnNode(BIPUSH, 8)); list.add(new InsnNode(ISHL)); list.add(new InsnNode(IOR));
                list.add(new VarInsnNode(ALOAD, 0));
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
                list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
                list.add(new InsnNode(IOR));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case CONST_LONG -> {
                emitReadLong(list);
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case CONST_FLOAT -> {
                emitRead4ByteInt(list);
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case CONST_DOUBLE -> {
                emitReadLong(list);
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case CONST_POOL -> {
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new IincInsnNode(5, 1));
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new InsnNode(AASTORE));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case LOAD_VAR -> {
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new IincInsnNode(5, 1));
                list.add(new VarInsnNode(ALOAD, 2));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new InsnNode(AASTORE));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case STORE_VAR -> {
                emitRead2ByteIndex(list);
                list.add(new VarInsnNode(ISTORE, 8));
                list.add(new IincInsnNode(5, -1));
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new InsnNode(AALOAD));
                list.add(new VarInsnNode(ASTORE, 9));
                list.add(new VarInsnNode(ALOAD, 2));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new InsnNode(AASTORE));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IINC -> {
                emitRead2ByteIndex(list);
                list.add(new VarInsnNode(ISTORE, 8));
                emitRead4ByteInt(list);
                list.add(new VarInsnNode(ISTORE, 9));
                list.add(new VarInsnNode(ALOAD, 2));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new VarInsnNode(ALOAD, 2));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new InsnNode(IADD));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new InsnNode(AASTORE));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case POP -> {
                list.add(new IincInsnNode(5, -1));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case DUP -> {
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new InsnNode(ICONST_1));
                list.add(new InsnNode(ISUB));
                list.add(new InsnNode(AALOAD));
                list.add(new InsnNode(AASTORE));
                list.add(new IincInsnNode(5, 1));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case SWAP -> {
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new InsnNode(ICONST_1));
                list.add(new InsnNode(ISUB));
                list.add(new InsnNode(AALOAD));
                list.add(new VarInsnNode(ASTORE, 8));
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new InsnNode(ICONST_2));
                list.add(new InsnNode(ISUB));
                list.add(new InsnNode(AALOAD));
                list.add(new VarInsnNode(ASTORE, 9));
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new InsnNode(ICONST_1));
                list.add(new InsnNode(ISUB));
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new InsnNode(AASTORE));
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new InsnNode(ICONST_2));
                list.add(new InsnNode(ISUB));
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new InsnNode(AASTORE));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IADD, ISUB, IMUL, IDIV, IREM, IXOR, IAND, IOR, ISHL, ISHR, IUSHR -> {
                popIntTo(list, 8); popIntTo(list, 9);
                list.add(new VarInsnNode(ILOAD, 9)); list.add(new VarInsnNode(ILOAD, 8));
                list.add(new InsnNode(mapIntMathOpcode(op)));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case INEG -> {
                popIntTo(list, 8);
                list.add(new VarInsnNode(ILOAD, 8)); list.add(new InsnNode(INEG));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case LADD, LSUB, LMUL, LDIV, LREM, LXOR, LAND, LOR -> {
                popLongTo(list, 8); popLongTo(list, 10);
                list.add(new VarInsnNode(LLOAD, 10)); list.add(new VarInsnNode(LLOAD, 8));
                list.add(new InsnNode(mapLongMathOpcode(op)));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case FADD, FSUB, FMUL, FDIV, FREM -> {
                popFloatTo(list, 8); popFloatTo(list, 9);
                list.add(new VarInsnNode(FLOAD, 9)); list.add(new VarInsnNode(FLOAD, 8));
                list.add(new InsnNode(mapFloatMathOpcode(op)));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case FNEG -> {
                popFloatTo(list, 8);
                list.add(new VarInsnNode(FLOAD, 8)); list.add(new InsnNode(FNEG));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case DADD, DSUB, DMUL, DDIV, DREM -> {
                popDoubleTo(list, 8); popDoubleTo(list, 10);
                list.add(new VarInsnNode(DLOAD, 10)); list.add(new VarInsnNode(DLOAD, 8));
                list.add(new InsnNode(mapDoubleMathOpcode(op)));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case DNEG -> {
                popDoubleTo(list, 8);
                list.add(new VarInsnNode(DLOAD, 8)); list.add(new InsnNode(DNEG));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case LCMP -> {
                popLongTo(list, 8); popLongTo(list, 10);
                list.add(new VarInsnNode(LLOAD, 10)); list.add(new VarInsnNode(LLOAD, 8));
                list.add(new InsnNode(LCMP));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case FCMPL, FCMPG -> {
                popFloatTo(list, 8); popFloatTo(list, 9);
                list.add(new VarInsnNode(FLOAD, 9)); list.add(new VarInsnNode(FLOAD, 8));
                list.add(new InsnNode(op == VMOpcode.FCMPL ? FCMPL : FCMPG));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case DCMPL, DCMPG -> {
                popDoubleTo(list, 8); popDoubleTo(list, 10);
                list.add(new VarInsnNode(DLOAD, 10)); list.add(new VarInsnNode(DLOAD, 8));
                list.add(new InsnNode(op == VMOpcode.DCMPL ? DCMPL : DCMPG));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case I2L -> {
                popIntTo(list, 8);
                list.add(new VarInsnNode(ILOAD, 8)); list.add(new InsnNode(I2L));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case I2F -> {
                popIntTo(list, 8);
                list.add(new VarInsnNode(ILOAD, 8)); list.add(new InsnNode(I2F));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case I2D -> {
                popIntTo(list, 8);
                list.add(new VarInsnNode(ILOAD, 8)); list.add(new InsnNode(I2D));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case I2B -> {
                popIntTo(list, 8);
                list.add(new VarInsnNode(ILOAD, 8)); list.add(new InsnNode(I2B));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case I2C -> {
                popIntTo(list, 8);
                list.add(new VarInsnNode(ILOAD, 8)); list.add(new InsnNode(I2C));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case I2S -> {
                popIntTo(list, 8);
                list.add(new VarInsnNode(ILOAD, 8)); list.add(new InsnNode(I2S));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case L2I -> {
                popLongTo(list, 8);
                list.add(new VarInsnNode(LLOAD, 8)); list.add(new InsnNode(L2I));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case L2F -> {
                popLongTo(list, 8);
                list.add(new VarInsnNode(LLOAD, 8)); list.add(new InsnNode(L2F));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case L2D -> {
                popLongTo(list, 8);
                list.add(new VarInsnNode(LLOAD, 8)); list.add(new InsnNode(L2D));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case F2I -> {
                popFloatTo(list, 8);
                list.add(new VarInsnNode(FLOAD, 8)); list.add(new InsnNode(F2I));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case F2L -> {
                popFloatTo(list, 8);
                list.add(new VarInsnNode(FLOAD, 8)); list.add(new InsnNode(F2L));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case F2D -> {
                popFloatTo(list, 8);
                list.add(new VarInsnNode(FLOAD, 8)); list.add(new InsnNode(F2D));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case D2I -> {
                popDoubleTo(list, 8);
                list.add(new VarInsnNode(DLOAD, 8)); list.add(new InsnNode(D2I));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case D2L -> {
                popDoubleTo(list, 8);
                list.add(new VarInsnNode(DLOAD, 8)); list.add(new InsnNode(D2L));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }
            case D2F -> {
                popDoubleTo(list, 8);
                list.add(new VarInsnNode(DLOAD, 8)); list.add(new InsnNode(D2F));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                list.add(new VarInsnNode(ASTORE, 8)); pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case GOTO -> {
                emitReadBranchOffset(list);
                list.add(new VarInsnNode(ILOAD, 4));
                list.add(new InsnNode(IADD));
                list.add(new VarInsnNode(ISTORE, 4));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE -> {
                emitReadBranchOffset(list);
                list.add(new VarInsnNode(ISTORE, 7));
                popIntTo(list, 8);
                var branchTaken = new LabelNode();
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new JumpInsnNode(mapSingleJumpOpcode(op), branchTaken));
                list.add(new JumpInsnNode(GOTO, loopStart));
                list.add(branchTaken);
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new VarInsnNode(ILOAD, 7));
                list.add(new InsnNode(IADD)); list.add(new VarInsnNode(ISTORE, 4));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE -> {
                emitReadBranchOffset(list);
                list.add(new VarInsnNode(ISTORE, 7));
                popIntTo(list, 8); popIntTo(list, 9);
                var branchTaken = new LabelNode();
                list.add(new VarInsnNode(ILOAD, 9)); list.add(new VarInsnNode(ILOAD, 8));
                list.add(new JumpInsnNode(mapDualJumpOpcode(op), branchTaken));
                list.add(new JumpInsnNode(GOTO, loopStart));
                list.add(branchTaken);
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new VarInsnNode(ILOAD, 7));
                list.add(new InsnNode(IADD)); list.add(new VarInsnNode(ISTORE, 4));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IF_ACMPEQ, IF_ACMPNE -> {
                emitReadBranchOffset(list);
                list.add(new VarInsnNode(ISTORE, 7));
                popRefTo(list, 8); popRefTo(list, 9);
                var branchTaken = new LabelNode();
                list.add(new VarInsnNode(ALOAD, 9)); list.add(new VarInsnNode(ALOAD, 8));
                list.add(new JumpInsnNode(op == VMOpcode.IF_ACMPEQ ? IF_ACMPEQ : IF_ACMPNE, branchTaken));
                list.add(new JumpInsnNode(GOTO, loopStart));
                list.add(branchTaken);
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new VarInsnNode(ILOAD, 7));
                list.add(new InsnNode(IADD)); list.add(new VarInsnNode(ISTORE, 4));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IFNULL, IFNONNULL -> {
                emitReadBranchOffset(list);
                list.add(new VarInsnNode(ISTORE, 7));
                popRefTo(list, 8);
                var branchTaken = new LabelNode();
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new JumpInsnNode(op == VMOpcode.IFNULL ? IFNULL : IFNONNULL, branchTaken));
                list.add(new JumpInsnNode(GOTO, loopStart));
                list.add(branchTaken);
                list.add(new VarInsnNode(ILOAD, 4)); list.add(new VarInsnNode(ILOAD, 7));
                list.add(new InsnNode(IADD)); list.add(new VarInsnNode(ISTORE, 4));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case GETSTATIC -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new VarInsnNode(ASTORE, 8));
                emitFieldGet(list, true);
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case PUTSTATIC -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new VarInsnNode(ASTORE, 8));
                popRefTo(list, 9);
                emitFieldSet(list, true);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case GETFIELD -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new VarInsnNode(ASTORE, 8));
                popRefTo(list, 9);
                emitFieldGet(list, false);
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case PUTFIELD -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new VarInsnNode(ASTORE, 8));
                popRefTo(list, 10);
                popRefTo(list, 9);
                emitFieldSetInstance(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case INVOKESTATIC, INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new VarInsnNode(ASTORE, 8));
                boolean isStatic = (op == VMOpcode.INVOKESTATIC);
                emitMethodInvoke(list, isStatic, loopStart);
            }

            case NEW_INSTANCE -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName",
                        "(Ljava/lang/String;)Ljava/lang/Class;", false));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructors",
                        "()[Ljava/lang/reflect/Constructor;", false));
                list.add(new InsnNode(ICONST_0));
                list.add(new InsnNode(AALOAD));
                list.add(new VarInsnNode(ASTORE, 8));
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new InsnNode(ICONST_1));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Constructor",
                        "setAccessible", "(Z)V", false));
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new IincInsnNode(5, 1));
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new InsnNode(AASTORE));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case NEWARRAY -> {
                list.add(new VarInsnNode(ALOAD, 0));
                list.add(new VarInsnNode(ILOAD, 4));
                list.add(new IincInsnNode(4, 1));
                list.add(new InsnNode(BALOAD));
                list.add(new IntInsnNode(SIPUSH, 255));
                list.add(new InsnNode(IAND));
                list.add(new VarInsnNode(ISTORE, 8));
                popIntTo(list, 9);
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new MethodInsnNode(INVOKESTATIC,
                        "java/lang/reflect/Array", "newInstance",
                        "(Ljava/lang/Class;I)Ljava/lang/Object;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case ANEWARRAY -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName",
                        "(Ljava/lang/String;)Ljava/lang/Class;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                popIntTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new MethodInsnNode(INVOKESTATIC,
                        "java/lang/reflect/Array", "newInstance",
                        "(Ljava/lang/Class;I)Ljava/lang/Object;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case ARRAYLENGTH -> {
                popRefTo(list, 8);
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "getLength", "(Ljava/lang/Object;)I", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case AALOAD -> {
                popIntTo(list, 8); popRefTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new InsnNode(AALOAD));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case AASTORE -> {
                popRefTo(list, 8); popIntTo(list, 9); popRefTo(list, 10);
                list.add(new VarInsnNode(ALOAD, 10));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new InsnNode(AASTORE));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IALOAD -> {
                popIntTo(list, 8); popRefTo(list, 9);
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "getInt", "(Ljava/lang/Object;I)I", false));
                list.add(new VarInsnNode(ISTORE, 8));
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case LALOAD -> {
                popIntTo(list, 8); popRefTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "getLong", "(Ljava/lang/Object;I)J", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case FALOAD -> {
                popIntTo(list, 8); popRefTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "getFloat", "(Ljava/lang/Object;I)F", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case DALOAD -> {
                popIntTo(list, 8); popRefTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "getDouble", "(Ljava/lang/Object;I)D", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case BALOAD, CALOAD, SALOAD -> {
                popIntTo(list, 8); popRefTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "getInt", "(Ljava/lang/Object;I)I", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case IASTORE -> {
                popIntTo(list, 8); popIntTo(list, 9); popRefTo(list, 10);
                list.add(new VarInsnNode(ALOAD, 10));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "setInt", "(Ljava/lang/Object;II)V", false));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case LASTORE -> {
                popLongTo(list, 8); popIntTo(list, 9); popRefTo(list, 10);
                list.add(new VarInsnNode(ALOAD, 10));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new VarInsnNode(LLOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "setLong", "(Ljava/lang/Object;IJ)V", false));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case FASTORE -> {
                popFloatTo(list, 8); popIntTo(list, 9); popRefTo(list, 10);
                list.add(new VarInsnNode(ALOAD, 10));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new VarInsnNode(FLOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "setFloat", "(Ljava/lang/Object;IF)V", false));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case DASTORE -> {
                popDoubleTo(list, 8); popIntTo(list, 9); popRefTo(list, 10);
                list.add(new VarInsnNode(ALOAD, 10));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new VarInsnNode(DLOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "setDouble", "(Ljava/lang/Object;ID)V", false));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case BASTORE, CASTORE, SASTORE -> {
                popIntTo(list, 8); popIntTo(list, 9); popRefTo(list, 10);
                list.add(new VarInsnNode(ALOAD, 10));
                list.add(new VarInsnNode(ILOAD, 9));
                list.add(new VarInsnNode(ILOAD, 8));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/reflect/Array",
                        "setInt", "(Ljava/lang/Object;II)V", false));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case CHECKCAST -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName",
                        "(Ljava/lang/String;)Ljava/lang/Class;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                popRefTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "cast",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case INSTANCEOF_CHECK -> {
                list.add(new VarInsnNode(ALOAD, 1));
                emitRead2ByteIndex(list);
                list.add(new InsnNode(AALOAD));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/String"));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName",
                        "(Ljava/lang/String;)Ljava/lang/Class;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                popRefTo(list, 9);
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new VarInsnNode(ALOAD, 9));
                list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "isInstance",
                        "(Ljava/lang/Object;)Z", false));
                list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                list.add(new VarInsnNode(ASTORE, 8));
                pushFromTemp8(list);
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case THROW -> {
                popRefTo(list, 8);
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new TypeInsnNode(CHECKCAST, "java/lang/Throwable"));
                list.add(new InsnNode(ATHROW));
            }

            case MONITOR_ENTER -> {
                popRefTo(list, 8);
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new InsnNode(MONITORENTER));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case MONITOR_EXIT -> {
                popRefTo(list, 8);
                list.add(new VarInsnNode(ALOAD, 8));
                list.add(new InsnNode(MONITOREXIT));
                list.add(new JumpInsnNode(GOTO, loopStart));
            }

            case RETURN_VAL -> {
                list.add(new IincInsnNode(5, -1));
                list.add(new VarInsnNode(ALOAD, 6));
                list.add(new VarInsnNode(ILOAD, 5));
                list.add(new InsnNode(AALOAD));
                list.add(new InsnNode(ARETURN));
            }

            case RETURN_VOID -> {
                list.add(new InsnNode(ACONST_NULL));
                list.add(new InsnNode(ARETURN));
            }
        }
    }

    private interface InsnEmitter { void emit(InsnList list); }

    private static void pushToStack(InsnList list, InsnEmitter valueEmitter) {
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new IincInsnNode(5, 1));
        valueEmitter.emit(list);
        list.add(new InsnNode(AASTORE));
    }

    private static void pushFromTemp8(InsnList list) {
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new IincInsnNode(5, 1));
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new InsnNode(AASTORE));
    }

    private static void popRefTo(InsnList list, int slot) {
        list.add(new IincInsnNode(5, -1));
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new InsnNode(AALOAD));
        list.add(new VarInsnNode(ASTORE, slot));
    }

    private static void popIntTo(InsnList list, int slot) {
        list.add(new IincInsnNode(5, -1));
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new InsnNode(AALOAD));
        list.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false));
        list.add(new VarInsnNode(ISTORE, slot));
    }

    private static void popLongTo(InsnList list, int slot) {
        list.add(new IincInsnNode(5, -1));
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new InsnNode(AALOAD));
        list.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false));
        list.add(new VarInsnNode(LSTORE, slot));
    }

    private static void popFloatTo(InsnList list, int slot) {
        list.add(new IincInsnNode(5, -1));
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new InsnNode(AALOAD));
        list.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false));
        list.add(new VarInsnNode(FSTORE, slot));
    }

    private static void popDoubleTo(InsnList list, int slot) {
        list.add(new IincInsnNode(5, -1));
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new InsnNode(AALOAD));
        list.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false));
        list.add(new VarInsnNode(DSTORE, slot));
    }

    private static void emitReadLong(InsnList list) {
        emitRead4ByteInt(list);
        list.add(new InsnNode(I2L));
        list.add(new IntInsnNode(BIPUSH, 32));
        list.add(new InsnNode(LSHL));
        emitRead4ByteInt(list);
        list.add(new InsnNode(I2L));
        list.add(new LdcInsnNode(0xFFFFFFFFL));
        list.add(new InsnNode(LAND));
        list.add(new InsnNode(LOR));
    }

    private static void emitRead4ByteInt(InsnList list) {
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(BIPUSH, 24)); list.add(new InsnNode(ISHL));
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
        list.add(new IntInsnNode(BIPUSH, 16)); list.add(new InsnNode(ISHL)); list.add(new InsnNode(IOR));
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
        list.add(new IntInsnNode(BIPUSH, 8)); list.add(new InsnNode(ISHL)); list.add(new InsnNode(IOR));
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
        list.add(new InsnNode(IOR));
    }

    private static void emitRead2ByteIndex(InsnList list) {
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
        list.add(new IntInsnNode(BIPUSH, 8)); list.add(new InsnNode(ISHL));
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
        list.add(new InsnNode(IOR));
    }

    private static void emitReadBranchOffset(InsnList list) {
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(BIPUSH, 8)); list.add(new InsnNode(ISHL));
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new VarInsnNode(ILOAD, 4)); list.add(new IincInsnNode(4, 1));
        list.add(new InsnNode(BALOAD)); list.add(new IntInsnNode(SIPUSH, 255)); list.add(new InsnNode(IAND));
        list.add(new InsnNode(IOR)); list.add(new InsnNode(I2S));
    }

    private static void emitFieldGet(InsnList list, boolean isStatic) {
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new LdcInsnNode(":"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;", false));
        list.add(new VarInsnNode(ASTORE, 8));

        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_0)); list.add(new InsnNode(AALOAD));
        list.add(new IntInsnNode(BIPUSH, '/'));  list.add(new IntInsnNode(BIPUSH, '.'));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;", false));
        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        list.add(new VarInsnNode(ASTORE, 10));

        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1)); list.add(new InsnNode(AALOAD));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 10));
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField",
                "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false));
        list.add(new VarInsnNode(ALOAD, 8));
        if (isStatic) {
            list.add(new InsnNode(ACONST_NULL));
        } else {
            list.add(new VarInsnNode(ALOAD, 9));
        }
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Field", "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;", false));
        list.add(new VarInsnNode(ASTORE, 8));
    }

    private static void emitFieldSet(InsnList list, boolean isStatic) {
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new LdcInsnNode(":"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;", false));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_0)); list.add(new InsnNode(AALOAD));
        list.add(new IntInsnNode(BIPUSH, '/')); list.add(new IntInsnNode(BIPUSH, '.'));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;", false));
        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        list.add(new VarInsnNode(ASTORE, 10));
        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1)); list.add(new InsnNode(AALOAD));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 10));
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField",
                "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false));
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new InsnNode(ACONST_NULL));
        list.add(new VarInsnNode(ALOAD, 9));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Field", "set",
                "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
    }

    private static void emitFieldSetInstance(InsnList list) {
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new LdcInsnNode(":"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;)[Ljava/lang/String;", false));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_0)); list.add(new InsnNode(AALOAD));
        list.add(new IntInsnNode(BIPUSH, '/')); list.add(new IntInsnNode(BIPUSH, '.'));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;", false));
        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        list.add(new VarInsnNode(ASTORE, 9));
        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1)); list.add(new InsnNode(AALOAD));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 9));
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField",
                "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false));
        list.add(new VarInsnNode(ASTORE, 8));
        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false));
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new VarInsnNode(ALOAD, 9));
        list.add(new VarInsnNode(ALOAD, 10));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Field", "set",
                "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
    }

    private static void emitMethodInvoke(InsnList list, boolean isStatic, LabelNode loopStart) {
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new LdcInsnNode(":"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "split",
                "(Ljava/lang/String;)[Ljava/lang/String;", false));
        list.add(new VarInsnNode(ASTORE, 8));

        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_0)); list.add(new InsnNode(AALOAD));
        list.add(new IntInsnNode(BIPUSH, '/')); list.add(new IntInsnNode(BIPUSH, '.'));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "replace", "(CC)Ljava/lang/String;", false));
        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Class", "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;", false));
        list.add(new VarInsnNode(ASTORE, 9));

        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1)); list.add(new InsnNode(AALOAD));
        list.add(new VarInsnNode(ASTORE, 10));

        list.add(new VarInsnNode(ALOAD, 8)); list.add(new IntInsnNode(BIPUSH, 2)); list.add(new InsnNode(AALOAD));
        list.add(new VarInsnNode(ASTORE, 8));

        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new VarInsnNode(ALOAD, 9));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader",
                "()Ljava/lang/ClassLoader;", false));
        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString",
                "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/invoke/MethodType", "parameterArray",
                "()[Ljava/lang/Class;", false));
        list.add(new VarInsnNode(ASTORE, 8));

        list.add(new VarInsnNode(ALOAD, 9));
        list.add(new VarInsnNode(ALOAD, 10));
        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false));
        list.add(new VarInsnNode(ASTORE, 8));

        list.add(new VarInsnNode(ALOAD, 8)); list.add(new InsnNode(ICONST_1));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V", false));

        if (!isStatic) {
            popRefTo(list, 9);
        }

        list.add(new VarInsnNode(ALOAD, 8));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Method", "getParameterCount",
                "()I", false));
        list.add(new VarInsnNode(ISTORE, 7));

        list.add(new VarInsnNode(ILOAD, 7));
        list.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
        list.add(new VarInsnNode(ASTORE, 10));

        list.add(new VarInsnNode(ILOAD, 7));
        list.add(new InsnNode(ICONST_1));
        list.add(new InsnNode(ISUB));
        list.add(new VarInsnNode(ISTORE, 7));

        var fillLoopStart = new LabelNode();
        var fillLoopEnd = new LabelNode();
        list.add(fillLoopStart);
        list.add(new VarInsnNode(ILOAD, 7));
        list.add(new JumpInsnNode(IFLT, fillLoopEnd));
        list.add(new VarInsnNode(ALOAD, 10));
        list.add(new VarInsnNode(ILOAD, 7));
        list.add(new IincInsnNode(5, -1));
        list.add(new VarInsnNode(ALOAD, 6));
        list.add(new VarInsnNode(ILOAD, 5));
        list.add(new InsnNode(AALOAD));
        list.add(new InsnNode(AASTORE));
        list.add(new IincInsnNode(7, -1));
        list.add(new JumpInsnNode(GOTO, fillLoopStart));
        list.add(fillLoopEnd);
        list.add(new VarInsnNode(ALOAD, 8));
        if (isStatic) {
            list.add(new InsnNode(ACONST_NULL));
        } else {
            list.add(new VarInsnNode(ALOAD, 9));
        }
        list.add(new VarInsnNode(ALOAD, 10));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false));
        list.add(new VarInsnNode(ASTORE, 8));
        pushFromTemp8(list);
        list.add(new JumpInsnNode(GOTO, loopStart));
    }

    private static int mapIntMathOpcode(VMOpcode op) {
        return switch (op) {
            case IADD -> IADD; case ISUB -> ISUB; case IMUL -> IMUL;
            case IDIV -> IDIV; case IREM -> IREM; case IXOR -> IXOR;
            case IAND -> IAND; case IOR  -> IOR;  case ISHL -> ISHL;
            case ISHR -> ISHR; case IUSHR-> IUSHR;
            default -> IADD;
        };
    }

    private static int mapLongMathOpcode(VMOpcode op) {
        return switch (op) {
            case LADD -> LADD; case LSUB -> LSUB; case LMUL -> LMUL;
            case LDIV -> LDIV; case LREM -> LREM; case LXOR -> LXOR;
            case LAND -> LAND; case LOR  -> LOR;
            default -> LADD;
        };
    }

    private static int mapFloatMathOpcode(VMOpcode op) {
        return switch (op) {
            case FADD -> FADD; case FSUB -> FSUB; case FMUL -> FMUL;
            case FDIV -> FDIV; case FREM -> FREM;
            default -> FADD;
        };
    }

    private static int mapDoubleMathOpcode(VMOpcode op) {
        return switch (op) {
            case DADD -> DADD; case DSUB -> DSUB; case DMUL -> DMUL;
            case DDIV -> DDIV; case DREM -> DREM;
            default -> DADD;
        };
    }

    private static int mapSingleJumpOpcode(VMOpcode op) {
        return switch (op) {
            case IFEQ -> IFEQ; case IFNE -> IFNE; case IFLT -> IFLT;
            case IFGE -> IFGE; case IFGT -> IFGT; case IFLE -> IFLE;
            default -> IFEQ;
        };
    }

    private static int mapDualJumpOpcode(VMOpcode op) {
        return switch (op) {
            case IF_ICMPEQ -> IF_ICMPEQ; case IF_ICMPNE -> IF_ICMPNE;
            case IF_ICMPLT -> IF_ICMPLT; case IF_ICMPGE -> IF_ICMPGE;
            case IF_ICMPGT -> IF_ICMPGT; case IF_ICMPLE -> IF_ICMPLE;
            default -> IF_ICMPEQ;
        };
    }
}

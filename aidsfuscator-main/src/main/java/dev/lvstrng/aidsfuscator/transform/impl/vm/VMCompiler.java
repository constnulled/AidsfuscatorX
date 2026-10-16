package dev.lvstrng.aidsfuscator.transform.impl.vm;

import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.util.*;

public class VMCompiler implements Opcodes {

    public record VMCompiledMethod(
            byte[] bytecode,
            List<Object> constantPool,
            Set<VMOpcode> usedOpcodes,
            int xorKey,
            boolean hasTryCatch
    ) {}

    private record JumpPatch(int patchOffset, LabelNode target, int nextPc) {}

    public static boolean canCompile(JMethod method) {
        if (method.isSpecial())
            return false;
        if (method.insns().size() < 4)
            return false;

        for (var insn : method.insns()) {
            if (insn instanceof LineNumberNode || insn instanceof FrameNode || insn instanceof LabelNode)
                continue;

            int op = insn.getOpcode();

            if (insn instanceof InsnNode) {
                if (op == NOP || op == ACONST_NULL) continue;
                if (op >= ICONST_M1 && op <= ICONST_5) continue;
                if (op == LCONST_0 || op == LCONST_1) continue;
                if (op == FCONST_0 || op == FCONST_1 || op == FCONST_2) continue;
                if (op == DCONST_0 || op == DCONST_1) continue;
                if (op == POP || op == POP2 || op == DUP || op == SWAP) continue;
                if (op >= IADD && op <= INEG) continue;
                if (op >= LADD && op <= LOR) continue;
                if (op >= FADD && op <= FNEG) continue;
                if (op >= DADD && op <= DNEG) continue;
                if (op >= I2L && op <= I2S) continue;
                if (op >= L2I && op <= L2D) continue;
                if (op >= F2I && op <= F2D) continue;
                if (op >= D2I && op <= D2F) continue;
                if (op == LCMP || op == FCMPL || op == FCMPG || op == DCMPL || op == DCMPG) continue;
                if (op >= IRETURN && op <= RETURN) continue;
                if (op == ARRAYLENGTH) continue;
                if (op >= IALOAD && op <= SALOAD) continue;
                if (op >= IASTORE && op <= SASTORE) continue;
                if (op == ATHROW) continue;
                if (op == MONITORENTER || op == MONITOREXIT) continue;
                return false;
            }

            if (insn instanceof VarInsnNode) {
                if ((op >= ILOAD && op <= ALOAD) || (op >= ISTORE && op <= ASTORE)) continue;
                return false;
            }

            if (insn instanceof IincInsnNode) continue;

            if (insn instanceof IntInsnNode) {
                if (op == BIPUSH || op == SIPUSH || op == NEWARRAY) continue;
                return false;
            }

            if (insn instanceof LdcInsnNode) continue;

            if (insn instanceof JumpInsnNode) {
                if (op == GOTO) continue;
                if (op >= IFEQ && op <= IF_ACMPNE) continue;
                if (op == IFNULL || op == IFNONNULL) continue;
                return false;
            }

            if (insn instanceof FieldInsnNode) continue;

            if (insn instanceof MethodInsnNode m) {
                if (m.name.equals("<init>") || m.name.equals("<clinit>")) return false;
                if (op == INVOKESPECIAL || op == INVOKEVIRTUAL || op == INVOKESTATIC || op == INVOKEINTERFACE)
                    continue;
                return false;
            }

            if (insn instanceof TypeInsnNode) {
                if (op == ANEWARRAY || op == CHECKCAST || op == INSTANCEOF) continue;
                return false;
            }

            if (insn instanceof MultiANewArrayInsnNode) return false;
            if (insn instanceof InvokeDynamicInsnNode) return false;
            if (insn instanceof TableSwitchInsnNode) return false;
            if (insn instanceof LookupSwitchInsnNode) return false;

            return false;
        }

        return true;
    }

    public static VMCompiledMethod compile(JMethod method, Map<VMOpcode, Integer> isaTable, int xorKey) {
        var baos = new ByteArrayOutputStream();
        var constPool = new ArrayList<Object>();
        var usedOpcodes = new HashSet<VMOpcode>();
        var labelOffsets = new HashMap<LabelNode, Integer>();
        var jumpPatches = new ArrayList<JumpPatch>();
        boolean hasTryCatch = !method.traps().isEmpty();

        for (var insn : method.insns()) {
            if (insn instanceof LabelNode label) {
                labelOffsets.put(label, baos.size());
                continue;
            }
            if (insn instanceof LineNumberNode || insn instanceof FrameNode) continue;

            int op = insn.getOpcode();

            if (insn instanceof InsnNode) {
                if (op == NOP) {
                    emitOp(baos, VMOpcode.NOP, isaTable, usedOpcodes);
                } else if (op == ACONST_NULL) {
                    emitOp(baos, VMOpcode.CONST_NULL, isaTable, usedOpcodes);
                } else if (op >= ICONST_M1 && op <= ICONST_5) {
                    emitOp(baos, VMOpcode.CONST_INT, isaTable, usedOpcodes);
                    emitInt(baos, op - ICONST_0);
                } else if (op == LCONST_0 || op == LCONST_1) {
                    emitOp(baos, VMOpcode.CONST_LONG, isaTable, usedOpcodes);
                    emitLong(baos, op == LCONST_0 ? 0L : 1L);
                } else if (op == FCONST_0 || op == FCONST_1 || op == FCONST_2) {
                    emitOp(baos, VMOpcode.CONST_FLOAT, isaTable, usedOpcodes);
                    emitInt(baos, Float.floatToRawIntBits(op == FCONST_0 ? 0f : op == FCONST_1 ? 1f : 2f));
                } else if (op == DCONST_0 || op == DCONST_1) {
                    emitOp(baos, VMOpcode.CONST_DOUBLE, isaTable, usedOpcodes);
                    emitLong(baos, Double.doubleToRawLongBits(op == DCONST_0 ? 0.0 : 1.0));
                } else if (op == POP || op == POP2) {
                    emitOp(baos, VMOpcode.POP, isaTable, usedOpcodes);
                } else if (op == DUP) {
                    emitOp(baos, VMOpcode.DUP, isaTable, usedOpcodes);
                } else if (op == SWAP) {
                    emitOp(baos, VMOpcode.SWAP, isaTable, usedOpcodes);
                } else if (op >= IADD && op <= INEG) {
                    emitOp(baos, mapIntMath(op), isaTable, usedOpcodes);
                } else if (op >= LADD && op <= LOR) {
                    emitOp(baos, mapLongMath(op), isaTable, usedOpcodes);
                } else if (op >= FADD && op <= FNEG) {
                    emitOp(baos, mapFloatMath(op), isaTable, usedOpcodes);
                } else if (op >= DADD && op <= DNEG) {
                    emitOp(baos, mapDoubleMath(op), isaTable, usedOpcodes);
                } else if (op >= I2L && op <= I2S) {
                    emitOp(baos, mapI2x(op), isaTable, usedOpcodes);
                } else if (op >= L2I && op <= L2D) {
                    emitOp(baos, mapL2x(op), isaTable, usedOpcodes);
                } else if (op >= F2I && op <= F2D) {
                    emitOp(baos, mapF2x(op), isaTable, usedOpcodes);
                } else if (op >= D2I && op <= D2F) {
                    emitOp(baos, mapD2x(op), isaTable, usedOpcodes);
                } else if (op == LCMP) {
                    emitOp(baos, VMOpcode.LCMP, isaTable, usedOpcodes);
                } else if (op == FCMPL) {
                    emitOp(baos, VMOpcode.FCMPL, isaTable, usedOpcodes);
                } else if (op == FCMPG) {
                    emitOp(baos, VMOpcode.FCMPG, isaTable, usedOpcodes);
                } else if (op == DCMPL) {
                    emitOp(baos, VMOpcode.DCMPL, isaTable, usedOpcodes);
                } else if (op == DCMPG) {
                    emitOp(baos, VMOpcode.DCMPG, isaTable, usedOpcodes);
                } else if (op == RETURN) {
                    emitOp(baos, VMOpcode.RETURN_VOID, isaTable, usedOpcodes);
                } else if (op >= IRETURN && op <= ARETURN) {
                    emitOp(baos, VMOpcode.RETURN_VAL, isaTable, usedOpcodes);
                } else if (op == ARRAYLENGTH) {
                    emitOp(baos, VMOpcode.ARRAYLENGTH, isaTable, usedOpcodes);
                } else if (op >= IALOAD && op <= SALOAD) {
                    emitOp(baos, mapArrayLoad(op), isaTable, usedOpcodes);
                } else if (op >= IASTORE && op <= SASTORE) {
                    emitOp(baos, mapArrayStore(op), isaTable, usedOpcodes);
                } else if (op == ATHROW) {
                    emitOp(baos, VMOpcode.THROW, isaTable, usedOpcodes);
                } else if (op == MONITORENTER) {
                    emitOp(baos, VMOpcode.MONITOR_ENTER, isaTable, usedOpcodes);
                } else if (op == MONITOREXIT) {
                    emitOp(baos, VMOpcode.MONITOR_EXIT, isaTable, usedOpcodes);
                }
                continue;
            }

            if (insn instanceof VarInsnNode v) {
                if (v.getOpcode() >= ILOAD && v.getOpcode() <= ALOAD) {
                    emitOp(baos, VMOpcode.LOAD_VAR, isaTable, usedOpcodes);
                    emitShort(baos, (short) v.var);
                } else {
                    emitOp(baos, VMOpcode.STORE_VAR, isaTable, usedOpcodes);
                    emitShort(baos, (short) v.var);
                }
                continue;
            }

            if (insn instanceof IincInsnNode iinc) {
                emitOp(baos, VMOpcode.IINC, isaTable, usedOpcodes);
                emitShort(baos, (short) iinc.var);
                emitInt(baos, iinc.incr);
                continue;
            }

            if (insn instanceof IntInsnNode intInsn) {
                if (op == BIPUSH || op == SIPUSH) {
                    emitOp(baos, VMOpcode.CONST_INT, isaTable, usedOpcodes);
                    emitInt(baos, intInsn.operand);
                } else if (op == NEWARRAY) {
                    emitOp(baos, VMOpcode.NEWARRAY, isaTable, usedOpcodes);
                    baos.write(intInsn.operand & 0xFF);
                }
                continue;
            }

            if (insn instanceof LdcInsnNode ldc) {
                if (ldc.cst instanceof Integer intVal) {
                    emitOp(baos, VMOpcode.CONST_INT, isaTable, usedOpcodes);
                    emitInt(baos, intVal);
                } else if (ldc.cst instanceof Long longVal) {
                    emitOp(baos, VMOpcode.CONST_LONG, isaTable, usedOpcodes);
                    emitLong(baos, longVal);
                } else if (ldc.cst instanceof Float floatVal) {
                    emitOp(baos, VMOpcode.CONST_FLOAT, isaTable, usedOpcodes);
                    emitInt(baos, Float.floatToRawIntBits(floatVal));
                } else if (ldc.cst instanceof Double doubleVal) {
                    emitOp(baos, VMOpcode.CONST_DOUBLE, isaTable, usedOpcodes);
                    emitLong(baos, Double.doubleToRawLongBits(doubleVal));
                } else {
                    int poolIdx = constPool.indexOf(ldc.cst);
                    if (poolIdx == -1) {
                        poolIdx = constPool.size();
                        constPool.add(ldc.cst);
                    }
                    emitOp(baos, VMOpcode.CONST_POOL, isaTable, usedOpcodes);
                    emitShort(baos, (short) poolIdx);
                }
                continue;
            }

            if (insn instanceof JumpInsnNode jump) {
                emitOp(baos, mapJump(op), isaTable, usedOpcodes);
                int patchOffset = baos.size();
                baos.write(0);
                baos.write(0);
                jumpPatches.add(new JumpPatch(patchOffset, jump.label, baos.size()));
                continue;
            }

            if (insn instanceof FieldInsnNode f) {
                String descriptor = f.owner + ":" + f.name + ":" + f.desc;
                int poolIdx = constPool.indexOf(descriptor);
                if (poolIdx == -1) {
                    poolIdx = constPool.size();
                    constPool.add(descriptor);
                }
                VMOpcode vmOp = switch (op) {
                    case GETSTATIC -> VMOpcode.GETSTATIC;
                    case PUTSTATIC -> VMOpcode.PUTSTATIC;
                    case GETFIELD  -> VMOpcode.GETFIELD;
                    default        -> VMOpcode.PUTFIELD;
                };
                emitOp(baos, vmOp, isaTable, usedOpcodes);
                emitShort(baos, (short) poolIdx);
                continue;
            }

            if (insn instanceof MethodInsnNode m) {
                String descriptor = m.owner + ":" + m.name + ":" + m.desc + ":" + (m.itf ? "1" : "0");
                int poolIdx = constPool.indexOf(descriptor);
                if (poolIdx == -1) {
                    poolIdx = constPool.size();
                    constPool.add(descriptor);
                }
                VMOpcode vmOp = switch (op) {
                    case INVOKESTATIC    -> VMOpcode.INVOKESTATIC;
                    case INVOKESPECIAL   -> VMOpcode.INVOKESPECIAL;
                    case INVOKEINTERFACE -> VMOpcode.INVOKEINTERFACE;
                    default              -> VMOpcode.INVOKEVIRTUAL;
                };
                emitOp(baos, vmOp, isaTable, usedOpcodes);
                emitShort(baos, (short) poolIdx);
                continue;
            }

            if (insn instanceof TypeInsnNode t) {
                int poolIdx = constPool.indexOf(t.desc);
                if (poolIdx == -1) {
                    poolIdx = constPool.size();
                    constPool.add(t.desc);
                }
                VMOpcode vmOp = switch (op) {
                    case NEW       -> VMOpcode.NEW_INSTANCE;
                    case ANEWARRAY -> VMOpcode.ANEWARRAY;
                    case CHECKCAST -> VMOpcode.CHECKCAST;
                    default        -> VMOpcode.INSTANCEOF_CHECK;
                };
                emitOp(baos, vmOp, isaTable, usedOpcodes);
                emitShort(baos, (short) poolIdx);
                continue;
            }
        }

        byte[] rawBytes = baos.toByteArray();

        for (var patch : jumpPatches) {
            Integer targetOffset = labelOffsets.get(patch.target());
            if (targetOffset == null) targetOffset = rawBytes.length;
            short rel = (short) (targetOffset - patch.nextPc());
            rawBytes[patch.patchOffset()] = (byte) ((rel >> 8) & 0xFF);
            rawBytes[patch.patchOffset() + 1] = (byte) (rel & 0xFF);
        }

        for (int i = 0; i < rawBytes.length; i++) {
            rawBytes[i] ^= (byte) xorKey;
        }

        return new VMCompiledMethod(rawBytes, constPool, usedOpcodes, xorKey, hasTryCatch);
    }

    private static void emitOp(ByteArrayOutputStream baos, VMOpcode op, Map<VMOpcode, Integer> isaTable, Set<VMOpcode> used) {
        used.add(op);
        baos.write(isaTable.get(op) & 0xFF);
    }

    private static void emitShort(ByteArrayOutputStream baos, short s) {
        baos.write((s >> 8) & 0xFF);
        baos.write(s & 0xFF);
    }

    private static void emitInt(ByteArrayOutputStream baos, int i) {
        baos.write((i >> 24) & 0xFF);
        baos.write((i >> 16) & 0xFF);
        baos.write((i >> 8) & 0xFF);
        baos.write(i & 0xFF);
    }

    private static void emitLong(ByteArrayOutputStream baos, long l) {
        emitInt(baos, (int) (l >> 32));
        emitInt(baos, (int) l);
    }

    private static VMOpcode mapIntMath(int op) {
        return switch (op) {
            case IADD -> VMOpcode.IADD; case ISUB -> VMOpcode.ISUB;
            case IMUL -> VMOpcode.IMUL; case IDIV -> VMOpcode.IDIV;
            case IREM -> VMOpcode.IREM; case IXOR -> VMOpcode.IXOR;
            case IAND -> VMOpcode.IAND; case IOR  -> VMOpcode.IOR;
            case ISHL -> VMOpcode.ISHL; case ISHR -> VMOpcode.ISHR;
            case IUSHR-> VMOpcode.IUSHR; case INEG -> VMOpcode.INEG;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapLongMath(int op) {
        return switch (op) {
            case LADD -> VMOpcode.LADD; case LSUB -> VMOpcode.LSUB;
            case LMUL -> VMOpcode.LMUL; case LDIV -> VMOpcode.LDIV;
            case LREM -> VMOpcode.LREM; case LXOR -> VMOpcode.LXOR;
            case LAND -> VMOpcode.LAND; case LOR  -> VMOpcode.LOR;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapFloatMath(int op) {
        return switch (op) {
            case FADD -> VMOpcode.FADD; case FSUB -> VMOpcode.FSUB;
            case FMUL -> VMOpcode.FMUL; case FDIV -> VMOpcode.FDIV;
            case FREM -> VMOpcode.FREM; case FNEG -> VMOpcode.FNEG;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapDoubleMath(int op) {
        return switch (op) {
            case DADD -> VMOpcode.DADD; case DSUB -> VMOpcode.DSUB;
            case DMUL -> VMOpcode.DMUL; case DDIV -> VMOpcode.DDIV;
            case DREM -> VMOpcode.DREM; case DNEG -> VMOpcode.DNEG;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapI2x(int op) {
        return switch (op) {
            case I2L -> VMOpcode.I2L; case I2F -> VMOpcode.I2F;
            case I2D -> VMOpcode.I2D; case I2B -> VMOpcode.I2B;
            case I2C -> VMOpcode.I2C; case I2S -> VMOpcode.I2S;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapL2x(int op) {
        return switch (op) {
            case L2I -> VMOpcode.L2I; case L2F -> VMOpcode.L2F;
            case L2D -> VMOpcode.L2D;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapF2x(int op) {
        return switch (op) {
            case F2I -> VMOpcode.F2I; case F2L -> VMOpcode.F2L;
            case F2D -> VMOpcode.F2D;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapD2x(int op) {
        return switch (op) {
            case D2I -> VMOpcode.D2I; case D2L -> VMOpcode.D2L;
            case D2F -> VMOpcode.D2F;
            default -> VMOpcode.NOP;
        };
    }

    private static VMOpcode mapJump(int op) {
        return switch (op) {
            case GOTO       -> VMOpcode.GOTO;
            case IFEQ       -> VMOpcode.IFEQ;   case IFNE  -> VMOpcode.IFNE;
            case IFLT       -> VMOpcode.IFLT;   case IFGE  -> VMOpcode.IFGE;
            case IFGT       -> VMOpcode.IFGT;   case IFLE  -> VMOpcode.IFLE;
            case IF_ICMPEQ  -> VMOpcode.IF_ICMPEQ; case IF_ICMPNE -> VMOpcode.IF_ICMPNE;
            case IF_ICMPLT  -> VMOpcode.IF_ICMPLT; case IF_ICMPGE -> VMOpcode.IF_ICMPGE;
            case IF_ICMPGT  -> VMOpcode.IF_ICMPGT; case IF_ICMPLE -> VMOpcode.IF_ICMPLE;
            case IF_ACMPEQ  -> VMOpcode.IF_ACMPEQ; case IF_ACMPNE -> VMOpcode.IF_ACMPNE;
            case IFNULL     -> VMOpcode.IFNULL; case IFNONNULL -> VMOpcode.IFNONNULL;
            default         -> VMOpcode.GOTO;
        };
    }

    private static VMOpcode mapArrayLoad(int op) {
        return switch (op) {
            case IALOAD -> VMOpcode.IALOAD; case LALOAD -> VMOpcode.LALOAD;
            case FALOAD -> VMOpcode.FALOAD; case DALOAD -> VMOpcode.DALOAD;
            case AALOAD -> VMOpcode.AALOAD; case BALOAD -> VMOpcode.BALOAD;
            case CALOAD -> VMOpcode.CALOAD; case SALOAD -> VMOpcode.SALOAD;
            default -> VMOpcode.AALOAD;
        };
    }

    private static VMOpcode mapArrayStore(int op) {
        return switch (op) {
            case IASTORE -> VMOpcode.IASTORE; case LASTORE -> VMOpcode.LASTORE;
            case FASTORE -> VMOpcode.FASTORE; case DASTORE -> VMOpcode.DASTORE;
            case AASTORE -> VMOpcode.AASTORE; case BASTORE -> VMOpcode.BASTORE;
            case CASTORE -> VMOpcode.CASTORE; case SASTORE -> VMOpcode.SASTORE;
            default -> VMOpcode.AASTORE;
        };
    }
}

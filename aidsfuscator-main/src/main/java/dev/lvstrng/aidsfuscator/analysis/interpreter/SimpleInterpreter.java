package dev.lvstrng.aidsfuscator.analysis.interpreter;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.TypeUtils;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.List;

/**
 * Interpreter that keeps track of types and merges them accordingly.
 * @author lvstrng
 * @author brownie (proper merging)
 */
public class SimpleInterpreter extends Interpreter<SimpleValue> implements Opcodes {
    private final Context context;
    private final JMethod method;

    public static Type NULL_TYPE = Type.getObjectType("null");
    public static Type REFERENCE_TYPE = Type.getObjectType("java/lang/Object");

    public SimpleInterpreter(Context context, JMethod method) {
        super(ASM9);
        this.context = context;
        this.method = method;
    }

    @Override
    public SimpleValue newValue(Type type) {
        if(type == null)
            return SimpleValue.UNINITIALIZED_VALUE;

        return switch (type.getSort()) {
            case Type.VOID -> null;
            case Type.INT, Type.BOOLEAN, Type.SHORT, Type.BYTE, Type.CHAR -> SimpleValue.INT_VALUE;
            case Type.LONG -> SimpleValue.LONG_VALUE;
            case Type.FLOAT -> SimpleValue.FLOAT_VALUE;
            case Type.DOUBLE -> SimpleValue.DOUBLE_VALUE;
            case Type.OBJECT, Type.ARRAY -> SimpleValue.of(type);
            default -> throw new AssertionError();
        };
    }

    @Override
    public SimpleValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case ACONST_NULL:
                return newValue(NULL_TYPE);
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5, BIPUSH, SIPUSH:
                return SimpleValue.INT_VALUE;
            case LCONST_0:
            case LCONST_1:
                return SimpleValue.LONG_VALUE;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                return SimpleValue.FLOAT_VALUE;
            case DCONST_0:
            case DCONST_1:
                return SimpleValue.DOUBLE_VALUE;
            case LDC:
                var value = ((LdcInsnNode) insn).cst;
                switch (value) {
                    case Integer _ -> {
                        return SimpleValue.INT_VALUE;
                    }
                    case Float _ -> {
                        return SimpleValue.FLOAT_VALUE;
                    }
                    case Long _ -> {
                        return SimpleValue.LONG_VALUE;
                    }
                    case Double _ -> {
                        return SimpleValue.DOUBLE_VALUE;
                    }
                    case String _ -> {
                        return newValue(Type.getObjectType("java/lang/String"));
                    }
                    case Type type -> {
                        int sort = type.getSort();

                        if (sort == Type.OBJECT || sort == Type.ARRAY) {
                            return newValue(Type.getObjectType("java/lang/Class"));
                        } else if (sort == Type.METHOD) {
                            return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
                        } else {
                            throw new AnalyzerException(insn, "Illegal LDC value " + value);
                        }
                    }
                    case Handle _ -> {
                        return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
                    }
                    case ConstantDynamic condy -> {
                        return newValue(Type.getType(condy.getDescriptor()));
                    }
                    case null, default -> throw new AnalyzerException(insn, "Illegal LDC: " + value);
                }
            case JSR:
                return SimpleValue.RETURNADDRESS_VALUE;
            case GETSTATIC:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEW:
                return new SimpleValue(Type.getObjectType(((TypeInsnNode) insn).desc), insn);
            default:
                throw new AssertionError();
        }
    }

    @Override
    public SimpleValue copyOperation(AbstractInsnNode insn, SimpleValue value) {
        if(insn instanceof VarInsnNode v && v.var == 0 && method.isVirtual() && !value.isThis()) {
            value.setThis();
            if(!method.name().equals("<init>"))
                value.setInitializedThis();
        }
        return value;
    }

    @Override
    public SimpleValue unaryOperation(AbstractInsnNode insn, SimpleValue value) throws AnalyzerException {
        return switch (insn.getOpcode()) {
            case INEG, IINC, L2I, F2I, D2I, I2B, I2C, I2S, INSTANCEOF, ARRAYLENGTH -> SimpleValue.INT_VALUE;
            case FNEG, I2F, L2F, D2F -> SimpleValue.FLOAT_VALUE;
            case LNEG, I2L, F2L, D2L -> SimpleValue.LONG_VALUE;
            case DNEG, I2D, L2D, F2D -> SimpleValue.DOUBLE_VALUE;
            case IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN,
                 ARETURN, PUTSTATIC, MONITORENTER, MONITOREXIT, IFNULL, IFNONNULL, ATHROW -> null;
            case GETFIELD -> newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEWARRAY -> switch (((IntInsnNode) insn).operand) {
                case T_BOOLEAN -> newValue(Type.getType("[Z"));
                case T_CHAR -> newValue(Type.getType("[C"));
                case T_BYTE -> newValue(Type.getType("[B"));
                case T_SHORT -> newValue(Type.getType("[S"));
                case T_INT -> newValue(Type.getType("[I"));
                case T_FLOAT -> newValue(Type.getType("[F"));
                case T_DOUBLE -> newValue(Type.getType("[D"));
                case T_LONG -> newValue(Type.getType("[J"));
                default -> throw new AnalyzerException(insn, "Invalid array type");
            };

            case ANEWARRAY -> newValue(Type.getType("[" + Type.getObjectType(((TypeInsnNode) insn).desc)));
            case CHECKCAST -> newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
            default -> throw new AssertionError();
        };
    }

    @Override
    public SimpleValue binaryOperation(AbstractInsnNode insn, SimpleValue value1, SimpleValue value2) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR, LCMP, FCMPL, FCMPG, DCMPL, DCMPG:
                return SimpleValue.INT_VALUE;
            case FALOAD:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                return SimpleValue.FLOAT_VALUE;
            case LALOAD:
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                return SimpleValue.LONG_VALUE;
            case DALOAD:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                return SimpleValue.DOUBLE_VALUE;
            case AALOAD: {
                var arrayType = value1.type();
                if(arrayType.getDescriptor().startsWith("["))
                    return newValue(TypeUtils.getElementType(arrayType));

                if(arrayType == NULL_TYPE)
                    return newValue(REFERENCE_TYPE);

                throw new AnalyzerException(insn, "Illegal object array load, array type: " + arrayType.getDescriptor());
            }
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return null;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public SimpleValue ternaryOperation(AbstractInsnNode insn, SimpleValue value1, SimpleValue value2, SimpleValue value3) throws AnalyzerException {
        return null;
    }

    @Override
    public SimpleValue naryOperation(AbstractInsnNode insn, List<? extends SimpleValue> values) throws AnalyzerException {
        int opcode = insn.getOpcode();

        if (opcode == MULTIANEWARRAY) {
            return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
        } else if (opcode == INVOKEDYNAMIC) {
            return newValue(Type.getReturnType(((InvokeDynamicInsnNode) insn).desc));
        } else {
            return newValue(Type.getReturnType(((MethodInsnNode) insn).desc));
        }
    }

    public SimpleValue handleInitializer(AbstractInsnNode insn, String desc, List<? extends SimpleValue> values, SimpleFrame frame) {
        var value = values.getFirst();
        if(value.isThis() && !value.isInitializedThis()) {
            frame.setLocal(0, newValue(value.type()).setThis().setInitializedThis());
        }

        return newValue(Type.getReturnType(desc));
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, SimpleValue value, SimpleValue expected) throws AnalyzerException {
        // do nothing
    }

    @Override
    public SimpleValue merge(SimpleValue v1, SimpleValue v2) {
        if(v1.equals(v2))
            return v1;
        if (v1 == SimpleValue.UNINITIALIZED_VALUE || v2 == SimpleValue.UNINITIALIZED_VALUE)
            return SimpleValue.UNINITIALIZED_VALUE;

        if(v1.type() == NULL_TYPE && v2.isReference())
            return v2;
        if(v2.type() == NULL_TYPE && v1.isReference())
            return v1;

        if(v1.isReference() && v2.isReference()) {
            var internal1 = v1.type().getInternalName();
            var internal2 = v2.type().getInternalName();

            var common = context.hierarchy().commonSuperClass(internal1, internal2);
            return newValue(Type.getObjectType(common));
        }

        return SimpleValue.UNINITIALIZED_VALUE;
    }

    public Context context() {
        return context;
    }

    public JMethod method() {
        return method;
    }
}
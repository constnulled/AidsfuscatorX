package dev.lvstrng.aidsfuscator.transform.impl.vm;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.nio.charset.StandardCharsets;

import static org.objectweb.asm.Opcodes.*;

public final class VMCallSiteRewriter {

    private VMCallSiteRewriter() {}

    public static void rewrite(JClass clazz, JMethod method, JMethod vmMethod,
                               VMCompiler.VMCompiledMethod compiled) {
        var newInsns = new InsnList();

        String latin1String = new String(compiled.bytecode(), StandardCharsets.ISO_8859_1);
        newInsns.add(new LdcInsnNode(latin1String));
        newInsns.add(new FieldInsnNode(GETSTATIC, "java/nio/charset/StandardCharsets",
                "ISO_8859_1", "Ljava/nio/charset/Charset;"));
        newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String",
                "getBytes", "(Ljava/nio/charset/Charset;)[B", false));

        var constPool = compiled.constantPool();
        newInsns.add(ASMUtils.pushInt(constPool.size()));
        newInsns.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
        for (int i = 0; i < constPool.size(); i++) {
            newInsns.add(new InsnNode(DUP));
            newInsns.add(ASMUtils.pushInt(i));
            var cst = constPool.get(i);
            if (cst == null) {
                newInsns.add(new InsnNode(ACONST_NULL));
            } else if (cst instanceof Integer val) {
                newInsns.add(ASMUtils.pushInt(val));
                newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer",
                        "valueOf", "(I)Ljava/lang/Integer;", false));
            } else if (cst instanceof Long val) {
                newInsns.add(ASMUtils.pushLong(val));
                newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long",
                        "valueOf", "(J)Ljava/lang/Long;", false));
            } else if (cst instanceof Float val) {
                newInsns.add(ASMUtils.pushFloat(val));
                newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float",
                        "valueOf", "(F)Ljava/lang/Float;", false));
            } else if (cst instanceof Double val) {
                newInsns.add(ASMUtils.pushDouble(val));
                newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double",
                        "valueOf", "(D)Ljava/lang/Double;", false));
            } else {
                newInsns.add(new LdcInsnNode(cst));
            }
            newInsns.add(new InsnNode(AASTORE));
        }

        int maxLocals = Math.max(method.maxLocals(), 16);
        newInsns.add(ASMUtils.pushInt(maxLocals));
        newInsns.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));

        int localSlot = 0;
        if (!method.isStatic()) {
            newInsns.add(new InsnNode(DUP));
            newInsns.add(new InsnNode(ICONST_0));
            newInsns.add(new VarInsnNode(ALOAD, 0));
            newInsns.add(new InsnNode(AASTORE));
            localSlot = 1;
        }

        for (var argType : method.args()) {
            newInsns.add(new InsnNode(DUP));
            newInsns.add(ASMUtils.pushInt(localSlot));
            switch (argType.getSort()) {
                case Type.BOOLEAN -> {
                    newInsns.add(new VarInsnNode(ILOAD, localSlot));
                    newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean",
                            "valueOf", "(Z)Ljava/lang/Boolean;", false));
                    localSlot++;
                }
                case Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                    newInsns.add(new VarInsnNode(ILOAD, localSlot));
                    newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Integer",
                            "valueOf", "(I)Ljava/lang/Integer;", false));
                    localSlot++;
                }
                case Type.LONG -> {
                    newInsns.add(new VarInsnNode(LLOAD, localSlot));
                    newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Long",
                            "valueOf", "(J)Ljava/lang/Long;", false));
                    localSlot += 2;
                }
                case Type.FLOAT -> {
                    newInsns.add(new VarInsnNode(FLOAD, localSlot));
                    newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float",
                            "valueOf", "(F)Ljava/lang/Float;", false));
                    localSlot++;
                }
                case Type.DOUBLE -> {
                    newInsns.add(new VarInsnNode(DLOAD, localSlot));
                    newInsns.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double",
                            "valueOf", "(D)Ljava/lang/Double;", false));
                    localSlot += 2;
                }
                default -> {
                    newInsns.add(new VarInsnNode(ALOAD, localSlot));
                    localSlot++;
                }
            }
            newInsns.add(new InsnNode(AASTORE));
        }

        newInsns.add(ASMUtils.pushInt(compiled.xorKey()));
        newInsns.add(new MethodInsnNode(INVOKESTATIC, clazz.name(),
                vmMethod.name(), vmMethod.desc(), false));

        var ret = method.returnType();
        switch (ret.getSort()) {
            case Type.VOID -> {
                newInsns.add(new InsnNode(POP));
                newInsns.add(new InsnNode(RETURN));
            }
            case Type.BOOLEAN -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Boolean"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Boolean",
                        "booleanValue", "()Z", false));
                newInsns.add(new InsnNode(IRETURN));
            }
            case Type.BYTE -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number",
                        "byteValue", "()B", false));
                newInsns.add(new InsnNode(IRETURN));
            }
            case Type.CHAR -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Character"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Character",
                        "charValue", "()C", false));
                newInsns.add(new InsnNode(IRETURN));
            }
            case Type.SHORT -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number",
                        "shortValue", "()S", false));
                newInsns.add(new InsnNode(IRETURN));
            }
            case Type.INT -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number",
                        "intValue", "()I", false));
                newInsns.add(new InsnNode(IRETURN));
            }
            case Type.LONG -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number",
                        "longValue", "()J", false));
                newInsns.add(new InsnNode(LRETURN));
            }
            case Type.FLOAT -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number",
                        "floatValue", "()F", false));
                newInsns.add(new InsnNode(FRETURN));
            }
            case Type.DOUBLE -> {
                newInsns.add(new TypeInsnNode(CHECKCAST, "java/lang/Number"));
                newInsns.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/Number",
                        "doubleValue", "()D", false));
                newInsns.add(new InsnNode(DRETURN));
            }
            default -> {
                if (!ret.getInternalName().equals("java/lang/Object"))
                    newInsns.add(new TypeInsnNode(CHECKCAST, ret.getInternalName()));
                newInsns.add(new InsnNode(ARETURN));
            }
        }

        method.insns().clear();
        method.insns().add(newInsns);
        method.core().maxStack = 8;
        method.setMaxLocals(Math.max(localSlot + 1, method.maxLocals()));
        method.reinitUnsafeInstructions();
    }
}

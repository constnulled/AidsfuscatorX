package dev.lvstrng.aidsfuscator.transform.impl.dynamic;

import dev.lvstrng.aidsfuscator.classgen.impl.indy.ConstantIndyClassGenerator;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InvokeDynamicConstantTransformer extends Transformer {
    private final Setting<Boolean> constants          = setting("constants",          true);
    private final Setting<Boolean> classes            = setting("classes",            true);
    private final Setting<Boolean> math               = setting("math",               true);
    private final Setting<Boolean> skipSmallConstants = setting("skipSmallConstants", false);

    public InvokeDynamicConstantTransformer() {
        super("InvokeDynamic Constants", "InvokeDynamicConstantTransformer");
    }

    @Override
    public void transform(Context context) {
        var gen = new ConstantIndyClassGenerator();
        var indyClass = gen.create(context);

        var stringBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmStringMethod.name(),
                gen.bsmStringMethod.desc(),
                false
        );
        var classBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmClassMethod.name(),
                gen.bsmClassMethod.desc(),
                false
        );
        var intBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmIntMethod.name(),
                gen.bsmIntMethod.desc(),
                false
        );
        var longBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmLongMethod.name(),
                gen.bsmLongMethod.desc(),
                false
        );
        var floatBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmFloatMethod.name(),
                gen.bsmFloatMethod.desc(),
                false
        );
        var doubleBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmDoubleMethod.name(),
                gen.bsmDoubleMethod.desc(),
                false
        );
        var mathIntBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmMathIntMethod.name(),
                gen.bsmMathIntMethod.desc(),
                false
        );
        var mathLongBsmHandle = new Handle(
                H_INVOKESTATIC,
                indyClass.name(),
                gen.bsmMathLongMethod.name(),
                gen.bsmMathLongMethod.desc(),
                false
        );

        for (var clazz : context.classes()) {
            if (clazz.version() < Opcodes.V1_7)
                continue;
            if (Exclusions.GLOBAL.excluded(clazz))
                continue;
            if (Exclusions.INVOKEDYNAMIC_CONSTANTS.excluded(clazz))
                continue;
            if (clazz.isInterface() && clazz.version() < Opcodes.V1_8)
                continue;

            var stringCache = new HashMap<String, InvokeDynamicInsnNode>();
            var classCache = new HashMap<String, InvokeDynamicInsnNode>();
            var intCache = new HashMap<Integer, InvokeDynamicInsnNode>();
            var longCache = new HashMap<Long, InvokeDynamicInsnNode>();
            var floatCache = new HashMap<Float, InvokeDynamicInsnNode>();
            var doubleCache = new HashMap<Double, InvokeDynamicInsnNode>();
            var mathIntCache = new HashMap<Integer, InvokeDynamicInsnNode>();
            var mathLongCache = new HashMap<Integer, InvokeDynamicInsnNode>();

            for (var method : clazz.methods()) {
                if (method.isAbstract() || method.isNative() || method.insns() == null)
                    continue;
                if (Exclusions.INVOKEDYNAMIC_CONSTANTS.excluded(method))
                    continue;
                if (cantEditMethod(clazz, method, true, true))
                    continue;
                if (method.properties().has(Property.INTEGER_DECRYPTOR, Property.STRING_DECRYPTOR))
                    continue;

                var frames = method.frames(context);
                if (frames == null)
                    continue;

                var targets = new ArrayList<AbstractInsnNode>();
                for (var insn = method.insns().getFirst(); insn != null; insn = insn.getNext()) {
                    targets.add(insn);
                }

                boolean changed = false;

                for (var insn : targets) {
                    var frame = frames.get(insn);
                    if (frame == null || !frame.isInitThis())
                        continue;
                    if (method.isUnsafe(insn))
                        continue;

                    var props = context.properties().get(insn);
                    if (props.has(Property.SENSITIVE_CONSTANT, Property.UNPROTECTED_SALT,
                            Property.SALT_ARTIFACT, Property.FIELD_SALT_ARTIFACT))
                        continue;

                    // 1. Strings
                    if (constants.value() && insn instanceof LdcInsnNode ldc && ldc.cst instanceof String str) {
                        if (props.has(Property.IGNORE_STRING))
                            continue;
                        if (str.length() > Character.MAX_VALUE)
                            continue;

                        var indyProto = stringCache.computeIfAbsent(str, s -> {
                            int key1 = random.nextInt();
                            int key2 = random.nextInt();
                            var chars = s.toCharArray();
                            for (int i = 0; i < chars.length; i++) {
                                chars[i] = (char) (chars[i] ^ key1 ^ (key2 + i));
                            }
                            var cipher = new String(chars);
                            var indyName = context.dictionary().newMethodName(clazz, "()Ljava/lang/String;");
                            return new InvokeDynamicInsnNode(indyName, "()Ljava/lang/String;", stringBsmHandle, cipher, key1, key2);
                        });

                        var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                        context.properties().add(indy, Property.IGNORE_STRING, Property.IGNORE_REF_OBFUSCATION);
                        method.insns().insertBefore(insn, indy);
                        method.insns().remove(insn);
                        changed = true;
                        continue;
                    }

                    // 2. Class Literals (Type.class)
                    if (classes.value() && insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type) {
                        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                            if (props.has(Property.IGNORE_REF_OBFUSCATION))
                                continue;

                            String className = type.getSort() == Type.OBJECT
                                     ? type.getClassName()
                                    : type.getDescriptor().replace('/', '.');

                            var indyProto = classCache.computeIfAbsent(className, cName -> {
                                int key1 = random.nextInt();
                                int key2 = random.nextInt();
                                int salt = random.nextInt();
                                var chars = cName.toCharArray();
                                for (int i = 0; i < chars.length; i++) {
                                    chars[i] = (char) (chars[i] ^ key1 ^ (key2 + i));
                                }
                                var cipher = new String(chars);
                                var indyName = context.dictionary().newMethodName(clazz, "()Ljava/lang/Class;");
                                return new InvokeDynamicInsnNode(indyName, "()Ljava/lang/Class;", classBsmHandle, cipher, key1, key2, salt);
                            });

                            var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                            context.properties().add(indy, Property.IGNORE_REF_OBFUSCATION);
                            method.insns().insertBefore(insn, indy);
                            method.insns().remove(insn);
                            changed = true;
                            continue;
                        }
                    }

                    // 3. Integers
                    if (constants.value() && ASMUtils.isIntPush(insn)) {
                        if (props.has(Property.IGNORE_INTEGER))
                            continue;
                        if (skipSmallConstants.value() && ASMUtils.isIconst(insn))
                            continue;

                        int val = ASMUtils.getInt(insn);
                        var indyProto = intCache.computeIfAbsent(val, v -> {
                            int key1 = random.nextInt();
                            int key2 = random.nextInt();
                            int encrypted = (v ^ key1) - key2;
                            var indyName = context.dictionary().newMethodName(clazz, "()I");
                            return new InvokeDynamicInsnNode(indyName, "()I", intBsmHandle, encrypted, key1, key2);
                        });

                        var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                        context.properties().add(indy, Property.IGNORE_INTEGER, Property.IGNORE_REF_OBFUSCATION);
                        method.insns().insertBefore(insn, indy);
                        method.insns().remove(insn);
                        changed = true;
                        continue;
                    }

                    // 4. Longs
                    if (constants.value() && ASMUtils.isLongPush(insn)) {
                        if (props.has(Property.IGNORE_LONG))
                            continue;
                        if (skipSmallConstants.value() && ASMUtils.isLconst(insn))
                            continue;

                        long val = ASMUtils.getLong(insn);
                        var indyProto = longCache.computeIfAbsent(val, v -> {
                            long key1 = random.nextLong();
                            long key2 = random.nextLong();
                            long encrypted = (v ^ key1) - key2;
                            var indyName = context.dictionary().newMethodName(clazz, "()J");
                            return new InvokeDynamicInsnNode(indyName, "()J", longBsmHandle, encrypted, key1, key2);
                        });

                        var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                        context.properties().add(indy, Property.IGNORE_LONG, Property.IGNORE_REF_OBFUSCATION);
                        method.insns().insertBefore(insn, indy);
                        method.insns().remove(insn);
                        changed = true;
                        continue;
                    }

                    // 5. Floats
                    if (constants.value()) {
                        Float floatVal = extractFloat(insn);
                        if (floatVal != null) {
                            var indyProto = floatCache.computeIfAbsent(floatVal, f -> {
                                int bits = Float.floatToRawIntBits(f);
                                int key1 = random.nextInt();
                                int key2 = random.nextInt();
                                int encryptedBits = (bits ^ key1) - key2;
                                var indyName = context.dictionary().newMethodName(clazz, "()F");
                                return new InvokeDynamicInsnNode(indyName, "()F", floatBsmHandle, encryptedBits, key1, key2);
                            });

                            var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                            context.properties().add(indy, Property.IGNORE_INTEGER, Property.IGNORE_REF_OBFUSCATION);
                            method.insns().insertBefore(insn, indy);
                            method.insns().remove(insn);
                            changed = true;
                            continue;
                        }
                    }

                    // 6. Doubles
                    if (constants.value()) {
                        Double doubleVal = extractDouble(insn);
                        if (doubleVal != null) {
                            var indyProto = doubleCache.computeIfAbsent(doubleVal, d -> {
                                long bits = Double.doubleToRawLongBits(d);
                                int key1 = random.nextInt();
                                int key2 = random.nextInt();
                                long encryptedBits = (bits ^ (long) key1) - (long) key2;
                                var indyName = context.dictionary().newMethodName(clazz, "()D");
                                return new InvokeDynamicInsnNode(indyName, "()D", doubleBsmHandle, encryptedBits, key1, key2);
                            });

                            var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                            context.properties().add(indy, Property.IGNORE_LONG, Property.IGNORE_REF_OBFUSCATION);
                            method.insns().insertBefore(insn, indy);
                            method.insns().remove(insn);
                            changed = true;
                            continue;
                        }
                    }

                    // 7. Binary Integer Math Operators (IADD, ISUB, IMUL, IXOR, IOR, IAND, ISHL, ISHR, IUSHR)
                    if (math.value()) {
                        int op = getIntMathOp(insn.getOpcode());
                        if (op != -1) {
                            if (props.has(Property.IGNORE_INTEGER, Property.IGNORE_FLOW_INTS, Property.IGNORE_REF_OBFUSCATION))
                                continue;

                            var indyProto = mathIntCache.computeIfAbsent(op, o -> {
                                int key1 = random.nextInt();
                                int key2 = random.nextInt();
                                int salt = random.nextInt();
                                int encOp = (o ^ key1) + key2;
                                var indyName = context.dictionary().newMethodName(clazz, "(II)I");
                                return new InvokeDynamicInsnNode(indyName, "(II)I", mathIntBsmHandle, encOp, key1, key2, salt);
                            });

                            var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                            context.properties().add(indy, Property.IGNORE_INTEGER, Property.IGNORE_REF_OBFUSCATION);
                            method.insns().insertBefore(insn, indy);
                            method.insns().remove(insn);
                            changed = true;
                            continue;
                        }
                    }

                    // 8. Binary Long Math Operators (LADD, LSUB, LMUL, LXOR, LOR, LAND)
                    if (math.value()) {
                        int op = getLongMathOp(insn.getOpcode());
                        if (op != -1) {
                            if (props.has(Property.IGNORE_LONG, Property.IGNORE_REF_OBFUSCATION))
                                continue;

                            var indyProto = mathLongCache.computeIfAbsent(op, o -> {
                                int key1 = random.nextInt();
                                int key2 = random.nextInt();
                                int salt = random.nextInt();
                                long encOp = ((long) o ^ (long) key1) + (long) key2;
                                var indyName = context.dictionary().newMethodName(clazz, "(JJ)J");
                                return new InvokeDynamicInsnNode(indyName, "(JJ)J", mathLongBsmHandle, encOp, key1, key2, salt);
                            });

                            var indy = new InvokeDynamicInsnNode(indyProto.name, indyProto.desc, indyProto.bsm, indyProto.bsmArgs);
                            context.properties().add(indy, Property.IGNORE_LONG, Property.IGNORE_REF_OBFUSCATION);
                            method.insns().insertBefore(insn, indy);
                            method.insns().remove(insn);
                            changed = true;
                            continue;
                        }
                    }
                }

                if (changed) {
                    method.reinitUnsafeInstructions();
                    markChange();
                }
            }
        }

        if (changes() == 0) {
            context.artificials().remove(indyClass.name());
        }
    }

    private int getIntMathOp(int opcode) {
        return switch (opcode) {
            case IADD -> 0;
            case ISUB -> 1;
            case IMUL -> 2;
            case IXOR -> 3;
            case IOR  -> 4;
            case IAND -> 5;
            case ISHL -> 6;
            case ISHR -> 7;
            case IUSHR -> 8;
            default -> -1;
        };
    }

    private int getLongMathOp(int opcode) {
        return switch (opcode) {
            case LADD -> 0;
            case LSUB -> 1;
            case LMUL -> 2;
            case LXOR -> 3;
            case LOR  -> 4;
            case LAND -> 5;
            default -> -1;
        };
    }

    private Float extractFloat(AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Float f)
            return f;
        if (insn.getOpcode() == FCONST_0) return 0.0f;
        if (insn.getOpcode() == FCONST_1) return 1.0f;
        if (insn.getOpcode() == FCONST_2) return 2.0f;
        return null;
    }

    private Double extractDouble(AbstractInsnNode insn) {
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Double d)
            return d;
        if (insn.getOpcode() == DCONST_0) return 0.0d;
        if (insn.getOpcode() == DCONST_1) return 1.0d;
        return null;
    }
}

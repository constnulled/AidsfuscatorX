package dev.lvstrng.aidsfuscator.transform.impl.indirection;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Setting;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReflectionHidingTransformer extends Transformer {

    private final Setting<String> mode = setting("mode", "all");

    public ReflectionHidingTransformer() {
        super("Reflection Hiding", "ReflectionHidingTransformer");
    }

    @Override
    public void transform(Context context) {
        for (var clazz : context.classes()) {
            if (Exclusions.GLOBAL.excluded(clazz)) continue;
            if (Exclusions.REFLECTION_HIDING.excluded(clazz)) continue;
            if (clazz.isInterface()) continue;

            Map<String, String> stubCache = new LinkedHashMap<>();

            for (var method : List.copyOf(clazz.methods())) {
                if (method.isNative()) continue;
                if (method.insns() == null) continue;
                if (Exclusions.REFLECTION_HIDING.excluded(method)) continue;

                List<AbstractInsnNode> targets = new ArrayList<>();
                for (var insn : method.insns()) {
                    if (insn instanceof MethodInsnNode call && isReflectiveTarget(call)) {
                        targets.add(insn);
                    }
                }

                for (var insn : targets) {
                    var call = (MethodInsnNode) insn;
                    var sig = stubSignature(call);
                    if (sig == null) continue;

                    var cacheKey = call.owner + "." + call.name + sig;
                    var stubName = stubCache.computeIfAbsent(cacheKey, k -> {
                        var name = context.dictionary().newMethodName(clazz, sig);
                        generateStub(context, clazz, name, call);
                        return name;
                    });

                    var replacement = new InsnBuilder()
                            .method(INVOKESTATIC, clazz.name(), stubName, sig, false)
                            .addProps(context, Property.IGNORE_REF_OBFUSCATION)
                            .result();

                    method.insns().insertBefore(call, replacement);
                    method.insns().remove(call);
                    markChange();
                }

                if (!targets.isEmpty()) {
                    method.reinitUnsafeInstructions();
                }
            }
        }
    }

    private boolean isReflectiveTarget(MethodInsnNode call) {
        boolean all    = mode.value().equals("all");
        boolean lookup = all || mode.value().equals("lookup");
        boolean invoke = all || mode.value().equals("invoke");

        return switch (call.owner) {
            case "java/lang/Class" -> switch (call.name) {
                case "forName",
                     "getDeclaredMethod", "getMethod",
                     "getDeclaredField",  "getField",
                     "getDeclaredConstructor", "getConstructor",
                     "newInstance" -> lookup;
                default -> false;
            };
            case "java/lang/reflect/Method"      -> call.name.equals("invoke") && invoke;
            case "java/lang/reflect/Constructor" -> call.name.equals("newInstance") && invoke;
            case "java/lang/reflect/Field"       -> isFieldAccess(call.name) && invoke;
            case "java/lang/ClassLoader"         -> call.name.equals("loadClass") && lookup;
            default                              -> false;
        };
    }

    private boolean isFieldAccess(String name) {
        return switch (name) {
            case "get", "set",
                 "getBoolean", "setBoolean",
                 "getByte",    "setByte",
                 "getChar",    "setChar",
                 "getShort",   "setShort",
                 "getInt",     "setInt",
                 "getLong",    "setLong",
                 "getFloat",   "setFloat",
                 "getDouble",  "setDouble" -> true;
            default -> false;
        };
    }

    private String stubSignature(MethodInsnNode call) {
        if (call.getOpcode() == INVOKESTATIC) {
            return call.desc;
        }
        Type methodType = Type.getMethodType(call.desc);
        Type[] args = methodType.getArgumentTypes();
        Type receiver = receiverType(call.owner);
        Type[] stubArgs = new Type[args.length + 1];
        stubArgs[0] = receiver;
        System.arraycopy(args, 0, stubArgs, 1, args.length);
        return Type.getMethodDescriptor(methodType.getReturnType(), stubArgs);
    }

    private Type receiverType(String internalName) {
        return internalName.startsWith("[") ? Type.getType(internalName) : Type.getObjectType(internalName);
    }

    private void generateStub(Context context, JClass clazz, String stubName, MethodInsnNode call) {
        var sig = stubSignature(call);
        var stub = clazz.createMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, stubName, sig);

        var builder = new InsnBuilder();
        var argTypes = Type.getArgumentTypes(sig);
        int slot = 0;

        for (var argType : argTypes) {
            builder.add(new VarInsnNode(argType.getOpcode(ILOAD), slot));
            slot += argType.getSize();
        }

        boolean isStatic = call.getOpcode() == INVOKESTATIC;
        builder.method(
                isStatic ? INVOKESTATIC : (call.getOpcode() == INVOKEINTERFACE ? INVOKEINTERFACE : INVOKEVIRTUAL),
                call.owner,
                call.name,
                call.desc,
                call.getOpcode() == INVOKEINTERFACE
        ).addProps(context, Property.IGNORE_REF_OBFUSCATION);

        var returnType = Type.getReturnType(call.desc);
        builder.add(new InsnNode(returnType.getOpcode(IRETURN)));

        stub.insns().add(builder.result());

        clazz.reinsertRandomly(random, stub);
    }
}

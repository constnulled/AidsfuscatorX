package dev.lvstrng.aidsfuscator.transform.impl.dynamic;

import dev.lvstrng.aidsfuscator.classgen.impl.ref.ReferenceObfuscationClassGenerator;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import dev.lvstrng.aidsfuscator.utils.ASMUtils;
import dev.lvstrng.aidsfuscator.utils.CryptUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ReferenceObfuscationTransformer extends Transformer {
    private final LinkedHashMap<String, Integer> referenceIndex = new LinkedHashMap<>();

    public ReferenceObfuscationTransformer() {
        super("Reference Obfuscation", "ReferenceObfuscateTransformer");
    }

    @Override
    public void transform(Context context) {
        referenceIndex.clear();
        var chars = getChars();
        var indexXor = random.nextInt();

        var gen = new ReferenceObfuscationClassGenerator(chars, indexXor);
        var refClass = gen.create(context);

        for(var clazz : context.classes()) {
            if(Exclusions.REFERENCE_OBFUSCATE.excluded(clazz))
                continue;
            if(clazz.isEnum())
                continue;
            if(clazz.version() < Opcodes.V1_7)
                continue;

            for(var method : clazz.methods()) {
                if(Exclusions.REFERENCE_OBFUSCATE.excluded(method))
                    continue;

                if(method.properties().has(Property.STRING_DECRYPTOR, Property.INTEGER_DECRYPTOR))
                    continue;

                var frames = method.frames(context);
                if (frames == null)
                    continue;
                for(var insn : method.insns()) {
                    var frame = frames.get(insn);
                    if(frame == null)
                        continue;
                    if(!frame.isInitThis())
                        continue;
                    if(method.isUnsafe(insn))
                        continue;

                    switch (insn) {
                        case MethodInsnNode call -> {
                            if(call.owner.startsWith("["))
                                continue;
                            if(!context.referenceManager().canObfuscate(call))
                                continue;
                            if(context.properties().get(call).has(Property.IGNORE_REF_OBFUSCATION))
                                continue;

                            char callSiteChar;
                            if(call.getOpcode() == INVOKEVIRTUAL || call.getOpcode() == INVOKEINTERFACE) {
                                callSiteChar = chars[0];
                            } else if (call.getOpcode() == INVOKESTATIC) {
                                callSiteChar = chars[1];
                            } else {
                                continue;
                            }

                            var decKey = random.nextInt() >> 16;
                            var idx = add(call.owner, call.name, call.desc, decKey);

                            var handle = new Handle(H_INVOKESTATIC, refClass.name(), gen.outerInvoker.name(), gen.outerInvoker.desc(), false);
                            var desc = call.getOpcode() == INVOKESTATIC ? call.desc : call.desc.replace("(", "(Ljava/lang/Object;");

                            var list = new InsnList();
                            var indy = new InvokeDynamicInsnNode(
                                    Character.toString(callSiteChar),
                                    desc.replace(")", "II)"),
                                    handle
                            );

                            int xorIndex = idx ^ indexXor;
                            list.add(context.properties().add(ASMUtils.pushInt(xorIndex), Property.IGNORE_INTEGER));
                            list.add(method.protectedIntPush(context, (decKey << 16) | (method.hasSalt() ? 0 : random.nextInt(Short.MAX_VALUE))));
                            list.add(indy);

                            method.insns().insertBefore(call, list);
                            method.insns().remove(call);
                            markChange();
                        }
                        case FieldInsnNode field -> {
                            if(!context.referenceManager().canObfuscate(field))
                                continue;

                            if(context.properties().get(field).has(Property.IGNORE_REF_OBFUSCATION))
                                continue;

                            var getter = field.getOpcode() == GETSTATIC || field.getOpcode() == GETFIELD;
                            var virtual = field.getOpcode() == GETFIELD || field.getOpcode() == PUTFIELD;

                            if(!getter) {
                                var owner = context.findByName(field.owner);
                                if(owner == null)
                                    continue;

                                var fieldRef = owner.findFieldFull(context, field.name, field.desc);
                                if(fieldRef == null)
                                    continue;

                                if(owner.isLibField(fieldRef) || owner.isInterface())
                                    continue;

                                fieldRef.removeAccessFlags(ACC_FINAL);
                            }

                            char callSiteChar;
                            if(virtual) {
                                if(getter) {
                                    callSiteChar = chars[2];
                                } else callSiteChar = chars[4];
                            } else {
                                if(getter) {
                                    callSiteChar = chars[3];
                                } else callSiteChar = chars[5];
                            }

                            var bsmDesc = new StringBuilder("(");
                            if(virtual) bsmDesc.append("Ljava/lang/Object;");
                            if(getter) {
                                bsmDesc.append("II)").append(field.desc);
                            } else {
                                bsmDesc.append(field.desc).append("II)V");
                            }

                            var decKey = method.canSalt(frames.get(insn)) ? method.salt().value() >> 16 : random.nextInt() >> 16;
                            var idx = add(field.owner, field.name, "()" + field.desc, decKey);
                            var handle = new Handle(H_INVOKESTATIC, refClass.name(), gen.outerInvoker.name(), gen.outerInvoker.desc(), false);

                            var list = new InsnList();
                            var indy = new InvokeDynamicInsnNode(
                                    Character.toString(callSiteChar),
                                    bsmDesc.toString(),
                                    handle
                            );

                            int idxXor = idx ^ indexXor;
                            list.add(context.properties().add(ASMUtils.pushInt(idxXor), Property.IGNORE_INTEGER));
                            if(method.hasSalt()) {
                                var mask = method.seed();
                                var masked = method.salt().value() & mask;

                                list.add(method.salt().load());
                                list.add(context.properties().add(ASMUtils.pushInt(mask), Property.IGNORE_INTEGER));
                                list.add(new InsnNode(IAND));
                                list.add(context.properties().add(ASMUtils.pushInt(masked ^ (decKey << 16)), Property.IGNORE_INTEGER));
                                list.add(new InsnNode(IXOR));
                            } else {
                                list.add(context.properties().add(ASMUtils.pushInt((decKey << 16) | random.nextInt(Short.MAX_VALUE)), Property.IGNORE_INTEGER));
                            }
                            list.add(indy);

                            method.insns().insertBefore(field, list);
                            method.insns().remove(field);
                            markChange();
                        }
                        default -> {}
                    }
                }
                method.reinitUnsafeInstructions();
            }
        }

        if(referenceIndex.isEmpty()) {
            context.artificials().remove(refClass.name());
            return;
        }

        gen.generateClinit(context, refClass, referenceList());
        context.saltDispatcher().setIndyField(refClass);
    }

    private int add(String owner, String name, String desc, int decKey) {
        owner = owner.replace('/', '.');
        var token = String.format("%s:%s:%s", owner, name, desc);
        token = CryptUtils.xor(token, decKey, 0);

        return referenceIndex.computeIfAbsent(token, k -> referenceIndex.size());
    }

    private List<String> referenceList() {
        var list = new ArrayList<String>(referenceIndex.size());
        referenceIndex.forEach((token, idx) -> {
            while (list.size() <= idx) list.add(null);
            list.set(idx, token);
        });
        return list;
    }

    private char[] getChars() {
        var out = new char[6];
        for(int i = 0; i < out.length; i++)
            out[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
        return out;
    }
}
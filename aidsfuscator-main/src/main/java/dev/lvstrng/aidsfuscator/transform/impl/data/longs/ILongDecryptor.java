package dev.lvstrng.aidsfuscator.transform.impl.data.longs;

import dev.lvstrng.aidsfuscator.analysis.interpreter.SimpleFrame;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

public interface ILongDecryptor extends Opcodes {
    SecureRandom random = new SecureRandom();

    void generate(JClass clazz, String fieldName);

    void setName(String name);

    String getDescriptor();

    InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite,
                        Map<AbstractInsnNode, SimpleFrame> frames, List<Long> numbers, long num);

    default byte[] longToBytes(long v) {
        return new byte[]{
            (byte) (v >> 56), (byte) (v >> 48), (byte) (v >> 40), (byte) (v >> 32),
            (byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8),  (byte) v
        };
    }
}

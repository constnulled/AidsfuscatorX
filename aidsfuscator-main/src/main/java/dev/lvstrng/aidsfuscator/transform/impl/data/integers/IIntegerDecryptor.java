package dev.lvstrng.aidsfuscator.transform.impl.data.integers;

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

public interface IIntegerDecryptor extends Opcodes {
    SecureRandom random = new SecureRandom();

    void generate(JClass clazz, String fieldName);

    void setName(String name);

    String getDescriptor();

    InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<Integer> numbers, int num);

    default byte[] intToBytes(int i) {
        return new byte[] {
                (byte) (i >> 24), (byte) (i >> 16),
                (byte) (i >> 8), (byte) (i)
        };
    }
}

package dev.lvstrng.aidsfuscator.transform.impl.data.strings;

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

public interface IStringDecryptor extends Opcodes {
    SecureRandom random = new SecureRandom();

    void generate(Context context, JClass clazz, String fieldName, String cacheName);

    InsnList addAndCall(Context context, JMethod method, AbstractInsnNode callSite, Map<AbstractInsnNode, SimpleFrame> frames, List<String> strings, String str);

    void setName(String name);

    String getDescriptor();

    default boolean usesSharedArray() {
        return true;
    }
}

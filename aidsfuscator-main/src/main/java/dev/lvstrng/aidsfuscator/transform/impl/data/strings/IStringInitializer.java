package dev.lvstrng.aidsfuscator.transform.impl.data.strings;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import org.objectweb.asm.Opcodes;

import java.security.SecureRandom;
import java.util.List;

public interface IStringInitializer extends Opcodes {
    SecureRandom random = new SecureRandom();

    void generate(Context context, JClass clazz, String fieldName, String cacheName, List<String> strings);
}

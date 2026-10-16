package dev.lvstrng.aidsfuscator.transform.impl.data.longs;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import org.objectweb.asm.Opcodes;

import java.security.SecureRandom;
import java.util.List;

public interface ILongInitializer extends Opcodes {
    SecureRandom random = new SecureRandom();

    void generate(Context context, JClass clazz, String fieldName, List<Long> numbers);

    default byte[] longToBytes(long v) {
        return new byte[]{
            (byte) (v >> 56), (byte) (v >> 48), (byte) (v >> 40), (byte) (v >> 32),
            (byte) (v >> 24), (byte) (v >> 16), (byte) (v >> 8),  (byte) v
        };
    }
}

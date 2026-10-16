package dev.lvstrng.aidsfuscator.transform.impl.data.integers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import org.objectweb.asm.Opcodes;

import java.security.SecureRandom;
import java.util.List;

public interface IIntegerInitializer extends Opcodes {
    SecureRandom random = new SecureRandom();
    void generate(Context context, JClass clazz, String fieldName, List<Integer> numbers);

    default byte[] intToBytes(int i) {
        return new byte[] {
                (byte) (i >> 24), (byte) (i >> 16),
                (byte) (i >> 8), (byte) (i)
        };
    }
}

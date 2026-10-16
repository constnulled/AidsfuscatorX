package dev.lvstrng.aidsfuscator.utils;

import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

public class NamedOpcodes {
    private static final Map<Integer, String> names = new HashMap<>();

    private NamedOpcodes() {
        throw new IllegalArgumentException();
    }

    static {
        try {
            boolean found = false;
            for (var declaredField : Opcodes.class.getDeclaredFields()) {
                if (declaredField.getName().equals("NOP")) found = true;

                if (!found) continue;

                names.put(declaredField.getInt(null), declaredField.getName().toLowerCase());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String map(int opcode) {
        return names.getOrDefault(opcode, String.valueOf(opcode));
    }
}

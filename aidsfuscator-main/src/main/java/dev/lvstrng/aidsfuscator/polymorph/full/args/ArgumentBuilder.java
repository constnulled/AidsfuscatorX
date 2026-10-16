package dev.lvstrng.aidsfuscator.polymorph.full.args;

import dev.lvstrng.aidsfuscator.polymorph.full.PolymorphMethodContext;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * Builds arguments for a string decryptor method
 */
public class ArgumentBuilder {
    public static final Random random = new Random();
    private final List<Argument> arguments = new ArrayList<>();
    private final Map<Argument, Integer> varSlots = new HashMap<>();
    private String descriptor;
    private Argument indexParam;
    private int freeLocal;

    public ArgumentBuilder generate(PolymorphMethodContext context) {
        arguments.add(indexParam = new Argument(KeyType.INDEX_KEY, ArgType.INT));

        int keyCount = random.nextInt(2, 7);
        for(int i = 0; i < keyCount; i++) {
            var keyType = KeyType.values()[random.nextInt(1, KeyType.values().length)];
            var argType = ArgType.values()[random.nextInt(ArgType.values().length)];

            arguments.add(new Argument(keyType, argType));
        }

        Collections.shuffle(arguments);
        int slot = 0;
        for(var arg : arguments) {
            varSlots.put(arg, slot);
            slot += arg.type().size();
        }

        this.freeLocal = slot;
        return this;
    }

    public int getFreeLocal() {
        return freeLocal;
    }

    public int allocVar(int size) {
        return freeLocal += size;
    }

    public Argument indexParam() {
        return indexParam;
    }

    public int slot(Argument argument) {
        return varSlots.get(argument);
    }

    public List<Argument> list() {
        return arguments;
    }

    public String descriptor() {
        if(descriptor == null) {
            var builder = new StringBuilder("(");

            for(var arg : arguments) {
                builder.append(switch (arg.type()) {
                    case INT -> "I";
                    case SHORT -> "S";
                    case CHAR -> "C";
                    case BYTE -> "B";
                });
            }

            builder.append(")Ljava/lang/String;");
            descriptor = builder.toString();
        }

        return descriptor;
    }
}

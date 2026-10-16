package dev.lvstrng.aidsfuscator.transform.impl.indirection;

import dev.lvstrng.aidsfuscator.classgen.impl.mh.MethodHandleResolverClassGenerator;
import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.exclude.impl.Exclusions;
import dev.lvstrng.aidsfuscator.transform.Transformer;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

public class MethodHandleObfuscationTransformer extends Transformer {

    public MethodHandleObfuscationTransformer() {
        super("Obfuscate MethodHandle Constants", "MethodHandleObfuscationTransformer");
    }

    @Override
    public void transform(Context context) {
        MethodHandleResolverClassGenerator gen = null;

        for (var clazz : context.classes()) {
            if (Exclusions.METHODHANDLE_OBFUSCATE.excluded(clazz)) continue;

            for (var method : clazz.methods()) {
                if (cantEditMethod(clazz, method)) continue;
                if (Exclusions.METHODHANDLE_OBFUSCATE.excluded(clazz, method)) continue;

                for (var insn : method.insns().toArray()) {
                    if (!(insn instanceof LdcInsnNode ldc)) continue;
                    if (!(ldc.cst instanceof Handle handle)) continue;

                    if (gen == null) {
                        gen = new MethodHandleResolverClassGenerator();
                        gen.create(context);
                    }

                    int xorKey = random.nextInt();
                    int encKind = handle.getTag() ^ xorKey;
                    String encOwner = encrypt(handle.getOwner(), xorKey);
                    String encName = encrypt(handle.getName(), xorKey);
                    String encDesc = encrypt(handle.getDesc(), xorKey);

                    var bsmHandle = new Handle(
                            H_INVOKESTATIC,
                            gen.className(),
                            gen.bsmMethod.name(),
                            MethodHandleResolverClassGenerator.BSM_DESC,
                            false
                    );

                    var indy = new InvokeDynamicInsnNode(
                            "H",
                            "()Ljava/lang/invoke/MethodHandle;",
                            bsmHandle,
                            encKind, xorKey, encOwner, encName, encDesc
                    );

                    method.insns().set(insn, indy);
                    markChange();
                }
            }
        }
    }

    private String encrypt(String s, int key) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] ^= (char) (key ^ i);
        }
        return new String(chars);
    }
}
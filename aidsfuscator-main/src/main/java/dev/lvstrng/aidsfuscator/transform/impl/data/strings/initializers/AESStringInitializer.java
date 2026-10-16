package dev.lvstrng.aidsfuscator.transform.impl.data.strings.initializers;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.property.Property;
import dev.lvstrng.aidsfuscator.transform.impl.data.strings.IStringInitializer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.CryptUtils;
import dev.lvstrng.aidsfuscator.utils.InsnBuilder;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LabelNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class AESStringInitializer implements IStringInitializer {
    @Override
    public void generate(Context context, JClass clazz, String fieldName, String cacheName, List<String> strings) {
        var key = (char) random.nextInt(Character.MAX_VALUE);
        var strBuilder = new StringBuilder();
        var lengthStr = new StringBuilder();

        var aesKey = CryptUtils.getKey().getEncoded();
        var aesIv = CryptUtils.getIv();

        for (var str : strings) {
            var encryptedString = CryptUtils.encrypt(str, aesKey, aesIv);

            strBuilder.append(encryptedString);
            lengthStr.append((char) (encryptedString.length() ^ key));
        }

        var keyStr = new String(aesKey, StandardCharsets.ISO_8859_1);
        var ivStr  = new String(aesIv,  StandardCharsets.ISO_8859_1);
        var theStr = strBuilder.toString();
        var lenStr = lengthStr.toString();

        var method = clazz.findOrCreateClinit();

        var xorKeyVar  = method.allocVar();
        var arrVar     = method.allocVar();
        var strVar     = method.allocVar();
        var lenStrVar  = method.allocVar();
        var lenVar     = method.allocVar(Type.INT_TYPE);
        var keyVar     = method.allocVar();
        var ivVar      = method.allocVar();
        var cipherVar  = method.allocVar();
        var iVar       = method.allocVar(Type.INT_TYPE);
        var ptrVar     = method.allocVar(Type.INT_TYPE);
        var strLen     = method.allocVar(Type.INT_TYPE);
        var subStrVar  = method.allocVar();

        var body = new InsnBuilder().label(new LabelNode());

        if (clazz.hasSalt()) {
            body._int(key ^ clazz.salt().value())
                    .add(clazz.salt().load())
                    .ixor();
        } else {
            body._int(key);
        }
        body._var(ISTORE, xorKeyVar);

        var loopLabel = new LabelNode();
        body.label(new LabelNode())
                ._int(strings.size())
                .anewarray("java/lang/String")
                ._var(ASTORE, arrVar)

                .label(new LabelNode())
                ._const(theStr)
                ._var(ASTORE, strVar)

                .label(new LabelNode())
                ._const(lenStr)
                .dup()
                ._var(ASTORE, lenStrVar)
                .method(INVOKEVIRTUAL, "java/lang/String", "length", "()I").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._var(ISTORE, lenVar)

                .label(new LabelNode())
                ._const(keyStr)
                ._const("ISO-8859-1")
                .method(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._var(ASTORE, keyVar)

                .label(new LabelNode())
                ._const(ivStr)
                ._const("ISO-8859-1")
                .method(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._var(ASTORE, ivVar)

                .label(new LabelNode())
                ._const("AES/CBC/PKCS5Padding")
                .method(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._var(ASTORE, cipherVar)

                .label(new LabelNode())
                ._int(0)
                .dup()
                ._var(ISTORE, iVar)
                ._var(ISTORE, ptrVar)

                .label(loopLabel)
                ._var(ALOAD, strVar)
                ._var(ILOAD, ptrVar)
                .dup()
                ._var(ALOAD, lenStrVar)
                ._var(ILOAD, iVar)
                .method(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._var(ILOAD, xorKeyVar)
                .ixor()
                ._int(0xffff)
                .iand()
                .dup()
                ._var(ISTORE, strLen)
                .iadd()
                .method(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._var(ASTORE, subStrVar)

                .label(new LabelNode())
                ._var(ALOAD, cipherVar)
                ._int(2)
                .type(NEW, "javax/crypto/spec/SecretKeySpec")
                .dup()
                ._var(ALOAD, keyVar)
                ._const("AES")
                .method(INVOKESPECIAL, "javax/crypto/spec/SecretKeySpec", "<init>", "([BLjava/lang/String;)V")
                .type(NEW, "javax/crypto/spec/IvParameterSpec")
                .dup()
                ._var(ALOAD, ivVar)
                .method(INVOKESPECIAL, "javax/crypto/spec/IvParameterSpec", "<init>", "([B)V")
                .method(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V").addProps(context, Property.IGNORE_REF_OBFUSCATION)

                .label(new LabelNode())
                .type(NEW, "java/lang/String")
                .dup()
                ._var(ALOAD, cipherVar)
                .method(INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._var(ALOAD, subStrVar)
                .method(INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                .method(INVOKEVIRTUAL, "javax/crypto/Cipher", "doFinal", "([B)[B").addProps(context, Property.IGNORE_REF_OBFUSCATION)
                ._const("UTF-8")
                .method(INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/lang/String;)V")
                ._var(ASTORE, subStrVar)

                .label(new LabelNode())
                ._var(ALOAD, arrVar)
                ._var(ILOAD, iVar)
                ._var(ALOAD, subStrVar)
                .aastore()

                .label(new LabelNode())
                .iinc(iVar, 1)

                .label(new LabelNode())
                ._var(ILOAD, ptrVar)
                ._var(ILOAD, strLen)
                .iadd()
                ._var(ISTORE, ptrVar)

                .label(new LabelNode())
                ._var(ILOAD, iVar)
                ._var(ILOAD, lenVar)
                .jump(IF_ICMPNE, loopLabel)

                .label(new LabelNode())
                ._var(ALOAD, arrVar)
                .field(PUTSTATIC, clazz.name(), fieldName, "[Ljava/lang/String;")

                .label(new LabelNode())
                ._var(ILOAD, lenVar)
                .anewarray("java/lang/Object")
                .field(PUTSTATIC, clazz.name(), cacheName, "[Ljava/lang/Object;")
        ;

        method.insertSafe(body.result());
        clazz.reinsertRandomly(random, method);
    }
}

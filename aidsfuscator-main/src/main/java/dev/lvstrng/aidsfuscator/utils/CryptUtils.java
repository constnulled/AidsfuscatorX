package dev.lvstrng.aidsfuscator.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

public class CryptUtils {
    public static int[] generateKeys(Random random, int size, int maxValue) {
        int[] keys = new int[size];
        for (int i = 0; i < size; i++)
            keys[i] = random.nextInt(maxValue + 1);

        return keys;
    }

    public static String xor(String str, int key, int[] keys) {
        var chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int key2 = keys[i % keys.length];

            chars[i] = (char) (chars[i] ^ key2);
            chars[i] = (char) (chars[i] ^ key);
        }

        return new String(chars);
    }

    public static String xor(String string, int key1, int key2) {
        var chars = string.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] ^ key1 ^ key2);
        }

        return new String(chars);
    }

    public static SecretKey getKey() {
        try {
            var gen = KeyGenerator.getInstance("AES");
            gen.init(256, new SecureRandom());
            return gen.generateKey();
        } catch (NoSuchAlgorithmException _) {
            throw new RuntimeException("No such algorithm");
        }
    }

    public static byte[] getIv() {
        var iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    public static String encrypt(String str, byte[] key, byte[] iv) {
        try {
            var c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            return Base64.getEncoder().encodeToString(c.doFinal(str.getBytes()));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static String decrypt(String str, byte[] key, byte[] iv) {
        try {
            var c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            var bytes = Base64.getDecoder().decode(str);

            return new String(c.doFinal(bytes));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}

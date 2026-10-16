package dev.test.transform;

import dev.lvstrng.aidsfuscator.polymorph.semi.impl.*;

import java.security.SecureRandom;

public class NonLinearMaskTest {

    private static final SecureRandom random = new SecureRandom();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Non-Linear Mask Verification Tests ===\n");

        for (int pos = 0; pos < 4; pos++) {
            testSBoxMask(pos);
        }

        for (int i = 0; i < 10; i++) {
            testFeistelMask();
        }

        for (int i = 0; i < 50; i++) {
            testMBAMask();
        }

        for (int i = 0; i < 10; i++) {
            testModMulMask();
        }

        testModInverse();

        System.out.println("\n=== Results ===");
        System.out.printf("Passed: %d, Failed: %d%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testSBoxMask(int bytePosition) {
        var mask = new SBoxMask(bytePosition);
        for (int i = 0; i < 10000; i++) {
            int original = random.nextInt();
            int encrypted = mask.apply(original);
            int decrypted = mask.applyInverse(encrypted);
            if (decrypted != original) {
                System.err.printf("FAIL SBoxMask(pos=%d): original=%08X encrypted=%08X decrypted=%08X%n",
                        bytePosition, original, encrypted, decrypted);
                failed++;
                return;
            }
        }
        System.out.printf("PASS SBoxMask(pos=%d): 10000 round-trips OK%n", bytePosition);
        passed++;
    }

    private static void testFeistelMask() {
        var mask = new FeistelMask();
        for (int i = 0; i < 10000; i++) {
            int original = random.nextInt();
            int encrypted = mask.apply(original);
            int decrypted = mask.applyInverse(encrypted);
            if (decrypted != original) {
                System.err.printf("FAIL FeistelMask: original=%08X encrypted=%08X decrypted=%08X%n",
                        original, encrypted, decrypted);
                failed++;
                return;
            }
        }
        System.out.println("PASS FeistelMask: 10000 round-trips OK");
        passed++;
    }

    private static void testMBAMask() {
        var mask = new MBAMask();
        for (int i = 0; i < 10000; i++) {
            int original = random.nextInt();
            int encrypted = mask.apply(original);
            int decrypted = mask.applyInverse(encrypted);
            if (decrypted != original) {
                System.err.printf("FAIL MBAMask: original=%08X encrypted=%08X decrypted=%08X%n",
                        original, encrypted, decrypted);
                failed++;
                return;
            }
        }
        System.out.println("PASS MBAMask: 10000 round-trips OK");
        passed++;
    }

    private static void testModMulMask() {
        var mask = new ModMulMask();
        for (int i = 0; i < 10000; i++) {
            int original = random.nextInt();
            int encrypted = mask.apply(original);
            int decrypted = mask.applyInverse(encrypted);
            if (decrypted != original) {
                System.err.printf("FAIL ModMulMask: original=%08X encrypted=%08X decrypted=%08X%n",
                        original, encrypted, decrypted);
                failed++;
                return;
            }
        }
        System.out.println("PASS ModMulMask: 10000 round-trips OK");
        passed++;
    }

    private static void testModInverse() {
        for (int i = 0; i < 10000; i++) {
            int key = random.nextInt() | 1;
            int inv = ModMulMask.modInverse32(key);
            int product = key * inv;
            if (product != 1) {
                System.err.printf("FAIL modInverse: key=%08X inv=%08X product=%08X (expected 1)%n",
                        key, inv, product);
                failed++;
                return;
            }
        }
        System.out.println("PASS modInverse32: 10000 keys verified (key * inv == 1)");
        passed++;
    }
}
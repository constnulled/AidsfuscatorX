package dev.lvstrng.aidsfuscator.utils;

import java.util.Random;

public class Utils {
    public static long bytesToKB(long bytes) {
        return Math.round((bytes / 1024.0) * 100.0) / 100L;
    }

    public static int chance(Random random) {
        return random.nextInt(101);
    }

    public static boolean chance(Random random, int chance) {
        return chance(random) <= Math.min(chance, 100);
    }
}

package dev.lvstrng.aidsfuscator;

public class AidsfuscatorInfo {
    private static final int major = 2, minor = 10, patch = 0;

    public static String build() {
        return String.format("v%s.%s.%s", major, minor, patch);
    }

    public static String type() {
        return "Aidsfuscator Free";
    }

    public static String versionText() {
        return String.format("%s %s", type(), build());
    }
}

package dev.lvstrng.aidsfuscator.log;

public enum LogLevel {
    SUCCESS("[SUCCESS]", ConsoleColors.LIGHT_GREEN),
    INFO("[INFO]", ConsoleColors.CYAN),
    WARN("[WARN]", ConsoleColors.YELLOW),
    ERROR("[ERROR]", ConsoleColors.RED);

    private final String tag;
    private final String color;

    LogLevel(String tag, String color) {
        this.tag = tag;
        this.color = color;
    }

    public String color() {
        return color;
    }

    public String tag() {
        return tag;
    }
}

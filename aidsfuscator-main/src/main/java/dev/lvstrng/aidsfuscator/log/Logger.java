package dev.lvstrng.aidsfuscator.log;

import io.github.kusoroadeolu.clique.Clique;
import io.github.kusoroadeolu.clique.components.Divider;
import io.github.kusoroadeolu.clique.components.Frame;
import io.github.kusoroadeolu.clique.configuration.*;
import io.github.kusoroadeolu.clique.spi.AnsiCode;
import io.github.kusoroadeolu.clique.style.Ink;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final Ink ACCENT   = Clique.ink().hex("#BD93F9").bold();
    private static final Ink SUCCESS  = Clique.ink().hex("#50FA7B").bold();
    private static final Ink INFO_INK = Clique.ink().hex("#8BE9FD");
    private static final Ink WARN_INK = Clique.ink().hex("#FFB86C").bold();
    private static final Ink ERROR_INK= Clique.ink().hex("#FF5555").bold();
    private static final Ink TIME_INK = Clique.ink().hex("#6272A4");
    private static final Ink DIM_INK  = Clique.ink().hex("#44475A");
    private static final Ink CHANGES_INK = Clique.ink().hex("#F8F8F2");

    private static final AnsiCode BORDER_COLOR = Clique.hex("#BD93F9");
    private static final int      FRAME_WIDTH  = 72;

    private static String timestamp() {
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now());
    }

    private static String badge(String label, Ink labelInk) {
        return DIM_INK.on("[") + labelInk.on(label) + DIM_INK.on("]");
    }

    private static String timePart() {
        return DIM_INK.on("[") + TIME_INK.on(timestamp()) + DIM_INK.on("]");
    }

    private static void print(String line) {
        System.out.println(line);
    }

    private static void log(LogLevel level, String msg, Object... args) {
        String formatted = args.length > 0 ? String.format(msg, args) : msg;

        Ink labelInk = switch (level) {
            case SUCCESS -> SUCCESS;
            case INFO    -> INFO_INK;
            case WARN    -> WARN_INK;
            case ERROR   -> ERROR_INK;
        };

        print(timePart() + " " + badge(level.name(), labelInk) + " " + formatted);
    }

    public static void success(String msg, Object... args) { log(LogLevel.SUCCESS, msg, args); }
    public static void warn   (String msg, Object... args) { log(LogLevel.WARN,    msg, args); }
    public static void error  (String msg, Object... args) { log(LogLevel.ERROR,   msg, args); }
    public static void info   (String msg, Object... args) { log(LogLevel.INFO,    msg, args); }

    public static void banner(String name, String version) {
        var titleText = ACCENT.gradient("#BD93F9", "#FF79C6").on(name) + " " + DIM_INK.on(version);

        var frame = Clique.frame(BoxType.ROUNDED, BORDER_COLOR)
                .title(ACCENT.on("✦ Lapisfuscator"), TitleAlign.LEFT)
                .width(FRAME_WIDTH)
                .nest(titleText, FrameAlign.CENTER)
                .nest(DIM_INK.on("Java Bytecode Obfuscator"), FrameAlign.CENTER);

        System.out.print(frame.get());
        System.out.println();
    }

    public static void divider() {
        System.out.println(
            Clique.divider(FRAME_WIDTH, BORDER_COLOR).get()
        );
    }

    public static void divider(String title) {
        System.out.println(
            Clique.divider(FRAME_WIDTH, BORDER_COLOR)
                  .title(DIM_INK.on("───") + ACCENT.on(title) + DIM_INK.on(" ───"))
                  .get()
        );
    }

    public static void transformerStart(String name) {
        String label = DIM_INK.on("⟶") + INFO_INK.on(name);
        print(timePart() + " " + badge("RUN", INFO_INK) + " " + label);
    }

    public static void transformerDone(String name, int changes) {
        String changePart = changes == 0
                ? DIM_INK.on("(no changes)")
                : CHANGES_INK.on("(" + changes + " changes)");
        String label = SUCCESS.on("✔ ") + Clique.ink().hex("#F8F8F2").on(name) + " " + changePart;
        print(timePart() + " " + badge("DONE", SUCCESS) + " " + label);
    }

    public static void passStart(String passName) {
        System.out.println();
        divider(passName);
    }

    public static void passComplete(String passName) {
        print(timePart() + " " + badge("PASS", SUCCESS) + " " + SUCCESS.on(passName + " complete"));
        System.out.println();
    }

    public static void exportResult(String inputFile, long inputKb, String outputFile, long outputKb) {
        var frame = Clique.frame(BoxType.ROUNDED, BORDER_COLOR)
                .title(SUCCESS.on("✦ Export Complete"), TitleAlign.LEFT)
                .width(FRAME_WIDTH)
                .nest(
                    DIM_INK.on("Input  : ") + INFO_INK.on(inputFile)  +
                    DIM_INK.on(" (" + inputKb  + " kb)"),
                    FrameAlign.LEFT
                )
                .nest(
                    DIM_INK.on("Output : ") + SUCCESS.on(outputFile) +
                    DIM_INK.on(" (" + outputKb + " kb)"),
                    FrameAlign.LEFT
                );

        System.out.println();
        System.out.print(frame.get());
        System.out.println();
    }
}

package dev.lvstrng.aidsfuscator.context.library;

import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.log.Logger;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.utils.ClassUtils;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.zip.*;

public class LibraryLoader {
    private static final Predicate<File> filter = f -> f.getName().endsWith(".jar") || f.getName().endsWith(".jmod");
    private final Context context;
    private String javaPath;

    public LibraryLoader(Context context) {
        this.context = context;
    }

    public void setJavaPath(String path) {
        this.javaPath = path;
    }

    public void loadLibraries(String path) {
        Logger.info("Loading libraries...");
        if (javaPath == null)
            loadJavaClasspath();
        else loadCustomJavaClasspath();

        var depDir = new File(path);
        if (!depDir.isDirectory())
            return;

        var jarFiles = listFiles(depDir);
        for (var jar : jarFiles) {
            parseJar(jar);
        }
    }

    private void loadCustomJavaClasspath() {
        var javaHome = Path.of(javaPath);
        if (!Files.exists(javaHome)) {
            Logger.warn("Couldn't find Java home (%s), falling back to runtime classpath", javaHome);
            loadJavaClasspath();
            return;
        }

        var jmods = javaHome.resolve("jmods");
        if (Files.isDirectory(jmods)) {
            Logger.info("Found Java 9+ classpath (%s), loading...", javaHome);

            var count = new AtomicInteger(0);
            try (var stream = Files.list(jmods)) {
                stream.filter(p -> p.toString().endsWith(".jmod"))
                        .forEach(e -> {
                            parseJar(e.toFile());
                            count.getAndIncrement();
                        });
            } catch (IOException _) {}

            Logger.info("Loaded %s `.jmod` files from classpath", count.get());
            return;
        }

        var rtJar = javaHome.resolve("jre").resolve("lib").resolve("rt.jar");
        if (!Files.exists(rtJar))
            rtJar = javaHome.resolve("lib").resolve("rt.jar");

        Logger.info("Found Java 8 `rt.jar` (%s), loading...", rtJar);
        if (Files.exists(rtJar)) {
            parseJar(rtJar.toFile());
        } else {
            Logger.warn("Couldn't find Java 8 `rt.jar` (%s), falling back to runtime classpath", rtJar);
            loadJavaClasspath();
        }
    }

    private void loadJavaClasspath() {
        Logger.info("Loading runtime java classpath...");
        try {
            var fs     = getJRTFS();
            var stream = Files.walk(fs.getPath("/modules"));

            stream.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                try (var in = Files.newInputStream(path)) {
                    var node = new JClass(ClassUtils.readAsDependency(in));
                    node.setLibrary();
                    context.addLibrary(node);
                } catch (IOException _) {}
            });
        } catch (IOException _) {}
    }

    public void parseJar(File path) {
        try (var zip = new ZipFile(path)) {
            for (var entry : Collections.list(zip.entries()))
                handleEntry(zip, entry);
        } catch (IOException _) {}
    }

    private void handleEntry(ZipFile zip, ZipEntry entry) {
        var name = entry.getName();
        try (var in = zip.getInputStream(entry)) {
            switch (name) {
                case String s when s.endsWith(".class") -> {
                    var clazz = new JClass(ClassUtils.readAsDependency(in));
                    clazz.setLibrary();
                    context.addLibrary(clazz);
                }
                case String s when s.endsWith(".jar") -> parseJar(in.readAllBytes());
                default -> {}
            }
        } catch (IOException _) {}
    }

    public void parseJar(byte[] jarBytes) {
        try (var zis = new ZipInputStream(new ByteArrayInputStream(jarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                handleEntry(zis, entry);
                zis.closeEntry();
            }
        } catch (IOException _) {}
    }

    private void handleEntry(ZipInputStream zis, ZipEntry entry) {
        var name = entry.getName();
        if (!name.endsWith(".class"))
            return;

        try {
            var clazz = new JClass(ClassUtils.readAsDependency(zis));
            clazz.setLibrary();
            context.addLibrary(clazz);
        } catch (IOException _) {}
    }

    private FileSystem getJRTFS() throws IOException {
        var uri = URI.create("jrt:/");
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(uri, Collections.emptyMap());
        }
    }

    private static List<File> listFiles(File dir) {
        var ls = new ArrayList<File>();
        listFiles(dir, ls);
        return ls;
    }

    private static void listFiles(File dir, List<File> checked) {
        var entries = dir.listFiles();
        if (entries == null) {
            Logger.warn("Could not list files in directory: %s (not a directory or I/O error)", dir.getAbsolutePath());
            return;
        }

        for (var f : entries) {
            if (f.isDirectory()) {
                listFiles(f, checked);
                continue;
            }

            if (!filter.test(f))
                continue;

            checked.add(f);
        }
    }
}

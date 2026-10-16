package dev.lvstrng.aidsfuscator.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;

public class ClassUtils {
    public static ClassNode readClass(byte[] bytes) {
        var node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    public static ClassNode readClass(InputStream stream) throws IOException {
        return readClass(stream.readAllBytes());
    }

    public static ClassNode readAsDependency(InputStream in) throws IOException {
        var node = new ClassNode();
        new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        return node;
    }
}
package dev.lvstrng.aidsfuscator.classgen.impl.hash;

import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import dev.lvstrng.aidsfuscator.tree.impl.JMethod;

import java.io.IOException;
import java.util.Random;

public interface IHashIntegrityClass {
    Random random = new Random();

    void init();

    JClass get();

    JMethod retriever();

    void add(JClass clazz, byte[] bytes);

    int classValue(JClass clazz);

    int paramValue(JClass clazz);

    void addResource() throws IOException;
}

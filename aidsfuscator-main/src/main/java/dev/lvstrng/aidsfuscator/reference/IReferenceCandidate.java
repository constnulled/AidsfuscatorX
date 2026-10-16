package dev.lvstrng.aidsfuscator.reference;

public interface IReferenceCandidate {
    boolean test(String owner, String name, String desc);

    String getFilterString();
}

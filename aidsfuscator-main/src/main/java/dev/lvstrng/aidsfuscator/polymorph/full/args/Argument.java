package dev.lvstrng.aidsfuscator.polymorph.full.args;

public record Argument(KeyType keyType, ArgType type) {
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Argument))
            return false;

        return obj.hashCode() == hashCode();
    }
}

package dev.lvstrng.aidsfuscator.utils;

public class Pair<F, S> {
    public F first;
    public S second;

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public static <F, S> Pair<F, S> of(F f, S s) {
        return new Pair<>(f, s);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Pair<?,?> p))
            return false;

        return p.first.equals(first) && p.second.equals(second);
    }

    @Override
    public int hashCode() {
        return first.hashCode() + second.hashCode();
    }
}

package dev.lvstrng.aidsfuscator.tree;

import java.util.HashSet;
import java.util.Set;

public interface IHierarchical<T> {

    Set<T> parents();
    Set<T> children();

    default Set<T> tree() {
        var set = new HashSet<>(parents());
        set.addAll(children());

        return set;
    }

    default boolean hasParent(T member) {
        return parents().contains(member);
    }

    default boolean hasChild(T member) {
        return children().contains(member);
    }

    default boolean isNonHierarchical() {
        return false;
    }

    default void clear() {
        parents().clear();
        children().clear();
    }
}

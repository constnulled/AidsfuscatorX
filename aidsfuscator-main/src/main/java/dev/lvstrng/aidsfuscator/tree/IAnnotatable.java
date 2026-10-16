package dev.lvstrng.aidsfuscator.tree;

import dev.lvstrng.aidsfuscator.utils.MemberUtils;
import org.objectweb.asm.tree.AnnotationNode;

import java.util.List;

public interface IAnnotatable {
    List<AnnotationNode> annotations();

    default boolean isAnnotatedBy(String annotation) {
        return MemberUtils.hasAnnotation(annotations(), annotation);
    }

    void removeAnnotation(String annotation);
}

package dev.lvstrng.aidsfuscator.utils;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class TypeUtils {
    private static final List<Integer> SORT_ORDER = new ArrayList<>();

    public static Type getElementType(Type arrayType) {
        var str = arrayType.getInternalName();
        str = str.substring(1);

        return Type.getType(str);
    }

    public static int getPromotionIndex(int sort) {
        return SORT_ORDER.indexOf(sort);
    }

    public static boolean isPromotion(Type t1, Type t2) {
        if(t2 == null)
            return true;

        var i1 = getPromotionIndex(t1.getSort());
        var i2 = getPromotionIndex(t2.getSort());

        return i1 >= i2;
    }

    public static boolean isPrimitive(Type t) {
        return t == null || t.getSort() < Type.ARRAY;
    }

    static {
        SORT_ORDER.add(0);
        SORT_ORDER.add(1);
        SORT_ORDER.add(3);
        SORT_ORDER.add(4);
        SORT_ORDER.add(2);
        SORT_ORDER.add(5);
        SORT_ORDER.add(6);
        SORT_ORDER.add(8);
        SORT_ORDER.add(7);
        SORT_ORDER.add(9);
        SORT_ORDER.add(10);
    }
}

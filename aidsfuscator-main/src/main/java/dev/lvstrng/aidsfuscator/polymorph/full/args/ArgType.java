package dev.lvstrng.aidsfuscator.polymorph.full.args;

public enum ArgType {
    INT(Integer.MIN_VALUE, Integer.MAX_VALUE, 1),
    CHAR(Character.MIN_VALUE, Character.MAX_VALUE, 1),
    SHORT(Short.MIN_VALUE, Short.MAX_VALUE, 1),
    BYTE(Byte.MIN_VALUE, Byte.MAX_VALUE, 1)
    ;

    private final int min, max, size;

    ArgType(int min, int max, int size) {
        this.min = min;
        this.max = max;
        this.size = size;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int randomValue() {
        return ArgumentBuilder.random.nextInt(min, max);
    }

    public int size() {
        return size;
    }
}

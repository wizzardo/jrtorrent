package com.wizzardo.jrt;

/**
 * Created by wizzardo on 07.10.15.
 */
public enum FilePriority {
    OFF("0"), LOW("1"), NORMAL("2"), HIGH("3");

    final String value;

    FilePriority(String value) {
        this.value = value;
    }

    public static FilePriority byInt(int i) {
        if (i == 0)
            return OFF;
        if (i == 1)
            return LOW;
        if (i == 2)
            return NORMAL;
        if (i == 3)
            return HIGH;

        throw new IllegalArgumentException("Argument is not in a valid range (0-3)");
    }

    public static FilePriority byString(String s) {
        if ("0".equals(s))
            return OFF;
        if ("1".equals(s))
            return LOW;
        if ("2".equals(s))
            return NORMAL;
        if ("3".equals(s))
            return HIGH;

        throw new IllegalArgumentException("Argument is not in a valid range (0-3)");
    }
}

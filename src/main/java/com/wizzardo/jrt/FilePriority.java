package com.wizzardo.jrt;

/**
 * Created by wizzardo on 07.10.15.
 */
public enum FilePriority {
    OFF(0), NORMAL(1), HIGH(2);

    final String s;
    final int i;

    FilePriority(int value) {
        this.s = String.valueOf(value);
        this.i = value;
    }

    public static FilePriority byInt(int i) {
        if (i == 0)
            return OFF;
        if (i == 1)
            return NORMAL;
        if (i == 2)
            return HIGH;

        throw new IllegalArgumentException("Argument is not in a valid range (0-2)");
    }

    public static FilePriority byString(String s) {
        if ("0".equals(s))
            return OFF;
        if ("1".equals(s))
            return NORMAL;
        if ("2".equals(s))
            return HIGH;

        throw new IllegalArgumentException("Argument is not in a valid range (0-2)");
    }
}

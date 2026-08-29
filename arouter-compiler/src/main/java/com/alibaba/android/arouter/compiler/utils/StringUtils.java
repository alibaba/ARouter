package com.alibaba.android.arouter.compiler.utils;

public final class StringUtils {
    private StringUtils() {
    }

    public static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    public static boolean isNotEmpty(CharSequence value) {
        return !isEmpty(value);
    }
}

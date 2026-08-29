package com.alibaba.android.arouter.compiler.utils;

import java.util.Map;

public final class MapUtils {
    private MapUtils() {
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return map != null && !map.isEmpty();
    }
}

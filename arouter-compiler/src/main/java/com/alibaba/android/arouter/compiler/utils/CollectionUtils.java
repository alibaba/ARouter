package com.alibaba.android.arouter.compiler.utils;

import java.util.Collection;

public final class CollectionUtils {
    private CollectionUtils() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
}

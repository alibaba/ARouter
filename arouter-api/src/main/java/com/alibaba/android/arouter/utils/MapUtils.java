package com.alibaba.android.arouter.utils;

import java.util.Map;

/**
 * Copy from apache common.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/3/10 下午3:26
 */
public class MapUtils {
    /**
     * Null-safe check if the specified map is not empty.
     * <p>
     * Null returns false.
     *
     * @param map  the map to check, may be null
     * @return true if non-null and non-empty
     * @since 3.2
     */
    public static boolean isNotEmpty(final Map<?,?> map) {
        return !isEmpty(map);
    }

    /**
     * Null-safe check if the specified map is empty.
     * <p>
     * Null returns true.
     *
     * @param map  the map to check, may be null
     * @return true if empty or null
     * @since 3.2
     */
    public static boolean isEmpty(final Map<?,?> map) {
        return map == null || map.isEmpty();
    }
}

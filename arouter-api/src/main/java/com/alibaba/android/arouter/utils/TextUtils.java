package com.alibaba.android.arouter.utils;

import android.net.Uri;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Text utils
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/9/9 14:40
 */
public class TextUtils {

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Print thread stack
     */
    public static String formatStackTrace(StackTraceElement[] stackTrace) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append("    at ").append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Split query parameters
     * @param rawUri raw uri
     * @return map with params
     */
    public static Map<String, String> splitQueryParameters(Uri rawUri) {
        String query = rawUri.getEncodedQuery();

        if (query == null) {
            return Collections.emptyMap();
        }

        Map<String, String> paramMap = new LinkedHashMap<>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);

            if (!android.text.TextUtils.isEmpty(name)) {
                String value = (separator == end ? "" : query.substring(separator + 1, end));
                paramMap.put(Uri.decode(name), Uri.decode(value));
            }

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableMap(paramMap);
    }

    /**
     * Split key with |
     *
     * @param key raw key
     * @return left key
     */
    public static String getLeft(String key) {
        if (key.contains("|") && !key.endsWith("|")) {
            return key.substring(0, key.indexOf("|"));
        } else {
            return key;
        }
    }

    /**
     * Split key with |
     *
     * @param key raw key
     * @return right key
     */
    public static String getRight(String key) {
        if (key.contains("|") && !key.startsWith("|")) {
            return key.substring(key.indexOf("|") + 1);
        } else {
            return key;
        }
    }
}

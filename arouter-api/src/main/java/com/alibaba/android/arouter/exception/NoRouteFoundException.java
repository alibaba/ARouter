package com.alibaba.android.arouter.exception;

/**
 * As its name
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/24 10:43
 */
public class NoRouteFoundException extends RuntimeException {
    /**
     * Constructs a new {@code RuntimeException} with the current stack trace
     * and the specified detail message.
     *
     * @param detailMessage the detail message for this exception.
     */
    public NoRouteFoundException(String detailMessage) {
        super(detailMessage);
    }
}

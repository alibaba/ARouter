package com.alibaba.android.arouter.exception;

/**
 * 初始化相关异常
 *
 * @author zhilong <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2015-12-07 14:17:30
 */
public class InitException extends RuntimeException {
    public InitException(String detailMessage) {
        super(detailMessage);
    }
}

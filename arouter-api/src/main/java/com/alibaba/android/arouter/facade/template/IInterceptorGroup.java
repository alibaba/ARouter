package com.alibaba.android.arouter.facade.template;

import java.util.Map;

/**
 * Template of interceptor group.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/29 09:51
 */
public interface IInterceptorGroup {
    /**
     * Load interceptor to input
     *
     * @param interceptor input
     */
    void loadInto(Map<Integer, Class<? extends IInterceptor>> interceptor);
}

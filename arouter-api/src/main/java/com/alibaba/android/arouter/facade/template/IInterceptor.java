package com.alibaba.android.arouter.facade.template;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;

/**
 * Used for inject custom logic when navigation.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 13:56
 */
public interface IInterceptor extends IProvider {

    /**
     * The operation of this interceptor.
     * <p>
     * Implementations must invoke exactly one method on {@code callback}. The
     * callback may be invoked asynchronously, but navigation is interrupted if
     * the entire interceptor chain does not finish before the postcard timeout.
     * Calls made after the first callback or after the timeout are ignored.
     *
     * @param postcard meta
     * @param callback cb
     */
    void process(Postcard postcard, InterceptorCallback callback);
}

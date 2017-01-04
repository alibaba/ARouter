package com.alibaba.android.arouter.facade.callback;

import com.alibaba.android.arouter.facade.Postcard;

/**
 * Callback after navigation.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2016/9/22 14:15
 */
public interface NavigationCallback {

    /**
     * Callback after you find the destination.
     * @param postcard meta
     */
    void onFound(Postcard postcard);

    /**
     * Callback after you lose your way.
     * @param postcard meta
     */
    void onLost(Postcard postcard);
}

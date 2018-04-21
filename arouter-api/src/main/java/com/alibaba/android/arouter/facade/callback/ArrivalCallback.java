package com.alibaba.android.arouter.facade.callback;

import com.alibaba.android.arouter.facade.Postcard;

/**
 * Callback after arrive at the destination.
 *
 * @author Victor Chiu <a href="mailto:4332weizi@gmail.com">Contact me.</a>
 * @version 1.3.2
 * @since 2018/4/21 14:13
 */
public interface ArrivalCallback {

    /**
     * Callback after navigation.
     *
     * @param postcard meta
     */
    void onArrival(Postcard postcard);
}

package com.alibaba.android.arouter.facade.callback;

import com.alibaba.android.arouter.facade.Postcard;

/**
 * Callback after found the destination.
 *
 * @author Victor Chiu <a href="mailto:4332weizi@gmail.com">Contact me.</a>
 * @version 1.3.2
 * @since 2018/4/21 14:13
 */
public interface FoundCallback {

    /**
     * Callback when find the destination.
     *
     * @param postcard meta
     */
    void onFound(Postcard postcard);
}

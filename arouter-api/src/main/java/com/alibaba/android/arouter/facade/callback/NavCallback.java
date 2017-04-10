package com.alibaba.android.arouter.facade.callback;

import com.alibaba.android.arouter.facade.Postcard;

/**
 * Easy to use navigation callback.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/4/10 下午12:59
 */
public abstract class NavCallback implements NavigationCallback {
    @Override
    public void onFound(Postcard postcard) {
        // Do nothing
    }

    @Override
    public void onLost(Postcard postcard) {
        // Do nothing
    }

    @Override
    public abstract void onArrival(Postcard postcard);

    @Override
    public void onInterrupt(Postcard postcard) {
        // Do nothing
    }
}

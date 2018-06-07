package com.alibaba.android.arouter.facade.callback;

import com.alibaba.android.arouter.facade.Postcard;

/**
 * Proxying implementation of NavigationCallback.
 *
 * @author Victor Chiu <a href="mailto:4332weizi@gmail.com">Contact me.</a>
 * @version 1.3.2
 * @since 2018/4/21 14:14
 */
public class NavigationCallbackWrapper implements NavigationCallback {

    private FoundCallback mFoundCallback;
    private LostCallback mLostCallback;
    private ArrivalCallback mArrivalCallback;
    private InterruptCallback mInterruptCallback;

    public NavigationCallbackWrapper(FoundCallback found, LostCallback lost, ArrivalCallback arrival, InterruptCallback interrupt) {
        mFoundCallback = found;
        mLostCallback = lost;
        mArrivalCallback = arrival;
        mInterruptCallback = interrupt;
    }

    @Override
    public void onInterrupt(Postcard postcard) {
        if (mInterruptCallback != null) {
            mInterruptCallback.onInterrupt(postcard);
        }
    }

    @Override
    public void onLost(Postcard postcard) {
        if (mLostCallback != null) {
            mLostCallback.onLost(postcard);
        }
    }

    @Override
    public void onArrival(Postcard postcard) {
        if (mArrivalCallback != null) {
            mArrivalCallback.onArrival(postcard);
        }
    }

    @Override
    public void onFound(Postcard postcard) {
        if (mFoundCallback != null) {
            mFoundCallback.onFound(postcard);
        }
    }
}

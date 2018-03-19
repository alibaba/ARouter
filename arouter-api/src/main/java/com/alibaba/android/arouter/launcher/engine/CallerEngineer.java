package com.alibaba.android.arouter.launcher.engine;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.launcher.Engineer;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class CallerEngineer extends Engineer<Caller> {

    public CallerEngineer(Caller caller) {
        super(caller);
    }

    @Override
    protected void start(Caller caller, int requestCode, Postcard postcard, Intent intent) {
        if (requestCode > 0) {  // Need start for result
            caller.startActivityForResult(intent, requestCode, postcard.getOptionsBundle());
        } else {
            caller.startActivity(intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim())) {    // Old version.
            caller.overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
        }
    }

    @Nullable
    @Override
    protected Context getContext() {
        return getT() != null ? getT().getContext() : null;
    }
}

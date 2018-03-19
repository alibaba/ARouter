package com.alibaba.android.arouter.launcher.engine;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.launcher.Engineer;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class SupportFragmentEngineer extends Engineer<Fragment> {
    public SupportFragmentEngineer(Fragment fragment) {
        super(fragment);
    }

    @Override
    protected void start(Fragment caller, int requestCode, Postcard postcard, Intent intent) {
        if (requestCode > 0) {  // Need start for result
            caller.startActivityForResult(intent, requestCode, postcard.getOptionsBundle());
        } else {
            caller.startActivity(intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim())) {    // Old version.
            //ignore
        }
    }

    @Nullable
    @Override
    protected Context getContext() {
        return getT() != null ? getT().getContext() : null;
    }
}

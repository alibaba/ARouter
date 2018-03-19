package com.alibaba.android.arouter.launcher.engine;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.launcher.Engineer;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class FragmentEngineer extends Engineer<Fragment> {
    public FragmentEngineer(Fragment fragment) {
        super(fragment);
    }

    @Override
    protected void start(Fragment caller, int requestCode, Postcard postcard, Intent intent) {
        if (requestCode > 0) {  // Need start for result
            if (Build.VERSION.SDK_INT >= 16) {
                caller.startActivityForResult(intent, requestCode, postcard.getOptionsBundle());
            } else {
                caller.startActivityForResult(intent, requestCode);
            }
        } else {
            if (Build.VERSION.SDK_INT >= 16) {
                caller.startActivity(intent, postcard.getOptionsBundle());
            } else {
                caller.startActivity(intent);
            }
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim())) {    // Old version.
            //ignore
        }
    }

    @Nullable
    @Override
    protected Context getContext() {
        return getT() != null ? getT().getActivity() : null;
    }
}

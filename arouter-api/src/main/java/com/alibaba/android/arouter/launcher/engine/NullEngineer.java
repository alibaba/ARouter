package com.alibaba.android.arouter.launcher.engine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.launcher.Engineer;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class NullEngineer extends Engineer<Context> {

    public NullEngineer(Context application) {
        super(application);
    }


    @Override
    protected void start(Context caller, int requestCode, Postcard postcard, Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (requestCode > 0) {  // Need start for result
            ActivityCompat.startActivityForResult((Activity) getContext(), intent, requestCode, postcard.getOptionsBundle());
        } else {
            ActivityCompat.startActivity(getContext(), intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim()) && getContext() instanceof Activity) {    // Old version.
            ((Activity) getContext()).overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
        }
    }

    @Nullable
    @Override
    protected Context getContext() {
        return getT();
    }
}

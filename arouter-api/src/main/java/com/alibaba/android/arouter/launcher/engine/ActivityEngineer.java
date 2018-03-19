package com.alibaba.android.arouter.launcher.engine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.launcher.Engineer;

import static com.alibaba.android.arouter.launcher.ARouter.logger;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class ActivityEngineer extends Engineer<Activity> {
    private static final String TAG = ActivityEngineer.class.toString();
    private final String mTag;

    public ActivityEngineer(Activity context) {
        super(context);
        mTag = context.getClass().getSimpleName();
    }

    @Override
    protected void start(Activity caller, int requestCode, Postcard postcard, Intent intent) {
        if (getT() == null) {
            logger.info(TAG, "activity: " + mTag + "is null,return");
            return;
        }
        if (requestCode > 0) {  // Need start for result
            ActivityCompat.startActivityForResult(getT(), intent, requestCode, postcard.getOptionsBundle());
        } else {
            ActivityCompat.startActivity(getT(), intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim())) {    // Old version.
            getT().overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
        }
    }

    @Nullable
    @Override
    protected Context getContext() {
        return getT();
    }
}

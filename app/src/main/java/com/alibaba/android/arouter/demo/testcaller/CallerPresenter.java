package com.alibaba.android.arouter.demo.testcaller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.launcher.engine.Caller;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class CallerPresenter implements Caller {
    private static final String TAG = CallerPresenter.class.toString();
    private static final int REQ = 123;

    private Fragment mFragment;

    public CallerPresenter(Fragment fragment) {
        mFragment = fragment;
    }


    public void start() {
        ARouter.getInstance()
                .build("/test/activity2")
                .navigation();
    }

    public void startForResult() {
        ARouter.getInstance().build("/test/activity2").navigation(this, REQ);
    }

    public void onResult(int requestCode, int resultCode, Intent data) {
        if (REQ == requestCode) {
            Log.e(TAG, "onResult: " + resultCode);
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        mFragment.startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        mFragment.startActivity(intent, options);
    }

    @Override
    public void overridePendingTransition(int enterAnim, int exitAnim) {
        //ignore
    }

    @Nullable
    @Override
    public Context getContext() {
        return mFragment.getContext();
    }
}

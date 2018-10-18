package com.alibaba.android.arouter.launcher;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

import java.lang.ref.WeakReference;


/**
 * 获取当期activity
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
final class CurrentActivityLifecycleCallback implements Application.ActivityLifecycleCallbacks {

    private WeakReference<Activity> mCurrentActivity;

    public CurrentActivityLifecycleCallback() {
    }

    public Activity getCurrentActivity() {
        return mCurrentActivity == null ? null : mCurrentActivity.get();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mCurrentActivity = new WeakReference<>(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}

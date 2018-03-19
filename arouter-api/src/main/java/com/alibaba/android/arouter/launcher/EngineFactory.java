package com.alibaba.android.arouter.launcher;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.alibaba.android.arouter.launcher.engine.ActivityEngineer;
import com.alibaba.android.arouter.launcher.engine.Caller;
import com.alibaba.android.arouter.launcher.engine.CallerEngineer;
import com.alibaba.android.arouter.launcher.engine.FragmentEngineer;
import com.alibaba.android.arouter.launcher.engine.NullEngineer;
import com.alibaba.android.arouter.launcher.engine.SupportFragmentEngineer;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

class EngineFactory {

    private EngineFactory() {
    }

    static <T> Engineer<T> createCaller(T obj) {
        if (null == obj) {
            return (Engineer<T>) new NullEngineer(_ARouter.mContext);
        } else if (obj instanceof Activity) {
            return (Engineer<T>) new ActivityEngineer((Activity) obj);
        } else if (obj instanceof Fragment) {
            return (Engineer<T>) new SupportFragmentEngineer((Fragment) obj);
        } else if (obj instanceof android.app.Fragment) {
            return (Engineer<T>) new FragmentEngineer((android.app.Fragment) obj);
        } else if (obj instanceof Caller) {
            return (Engineer<T>) new CallerEngineer((Caller) obj);
        } else {
            throw new IllegalArgumentException("can't find the " + obj.getClass().getSimpleName() + "type caller");
        }
    }
}

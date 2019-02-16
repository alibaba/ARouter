package com.alibaba.android.arouter.launcher;

import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Launch to Activity and get result.
 * <p>
 * {@link android.app.Activity#onActivityResult(int, int, Intent)}
 *
 * @author act262@gmail.com
 */
public interface OnResultCallback {

    /**
     * Activity result return here.
     * <p>
     * {@link android.app.Activity#onActivityResult(int, int, Intent)}
     */
    void onResult(int resultCode, Intent data);
}

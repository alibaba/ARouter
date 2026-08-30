package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.os.Bundle;

import com.alibaba.android.arouter.launcher.ARouter;

/**
 * Host activity used by the Postcard array extras regression test.
 */
public class PostcardArrayHostActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ARouter.getInstance()
                .build("/test/activity2")
                .withStringArray("strings", new String[]{"alpha", null, "omega"})
                .withIntArray("ints", new int[]{-7, 0, 42})
                .withLongArray("longs", new long[]{1L})
                .withLongArray("longs", new long[]{Long.MIN_VALUE, 0L, Long.MAX_VALUE})
                .withDoubleArray("doubles", new double[]{-1.5d, 0.0d, Math.PI})
                .withStringArray("nullStrings", null)
                .navigation(this);
    }
}

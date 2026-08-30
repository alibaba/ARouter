package com.alibaba.android.arouter.configcache;

import android.app.Activity;
import android.os.Bundle;

import com.alibaba.android.arouter.launcher.ARouter;

public class ProbeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ARouter.init(getApplication());
        ARouter.getInstance().build("/cache/second").navigation(this);
    }
}

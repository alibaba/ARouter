package com.alibaba.android.arouter.configcache;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.android.arouter.facade.template.IProvider;
import com.alibaba.android.arouter.launcher.ARouter;

public class ProbeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ARouter.init(getApplication());

        String featureRoute = getPackageName().endsWith(".daily")
                ? "/fixture/daily"
                : "/fixture/online";
        Object provider = ARouter.getInstance().build(featureRoute).navigation();
        if (!(provider instanceof IProvider)) {
            throw new IllegalStateException("Cannot resolve feature provider " + featureRoute);
        }
        Log.i("ARouterFixture", "Resolved feature provider " + featureRoute);

        ARouter.getInstance().build("/cache/second").navigation(this);
    }
}

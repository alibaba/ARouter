package com.alibaba.android.arouter.configcache;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityOptionsCompat;

import com.alibaba.android.arouter.facade.Postcard;
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

        Object fragment = ARouter.getInstance()
                .build("/cache/androidx-fragment")
                .withString("source", "configuration-cache-fixture")
                .navigation();
        if (!(fragment instanceof AndroidXProbeFragment)) {
            throw new IllegalStateException("Cannot resolve AndroidX Fragment route");
        }
        Bundle arguments = ((AndroidXProbeFragment) fragment).getArguments();
        if (arguments == null
                || !"configuration-cache-fixture".equals(arguments.getString("source"))) {
            throw new IllegalStateException("Cannot pass AndroidX Fragment route arguments");
        }
        ARouter.getInstance().inject(fragment);
        if (!"configuration-cache-fixture".equals(((AndroidXProbeFragment) fragment).source)) {
            throw new IllegalStateException("Cannot inject AndroidX Fragment route arguments");
        }

        Object dialog = ARouter.getInstance().build("/cache/androidx-dialog")
                .withString("source", "dialog-fixture").navigation();
        if (!(dialog instanceof AndroidXProbeDialogFragment)) {
            throw new IllegalStateException("Cannot resolve AndroidX DialogFragment route");
        }
        AndroidXProbeDialogFragment routedDialog = (AndroidXProbeDialogFragment) dialog;
        ARouter.getInstance().inject(routedDialog);
        if (routedDialog.getArguments() == null
                || !"dialog-fixture".equals(routedDialog.getArguments().getString("source"))
                || !"dialog-fixture".equals(routedDialog.source)
                || routedDialog.isAdded() || routedDialog.getDialog() != null) {
            throw new IllegalStateException("DialogFragment creation/injection contract changed");
        }

        Postcard activityPostcard = ARouter.getInstance().build("/cache/second");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
            );
            if (activityPostcard.withOptionsCompat(options) != activityPostcard
                    || activityPostcard.getOptionsBundle() == null) {
                throw new IllegalStateException("AndroidX ActivityOptionsCompat contract is unavailable");
            }
        }

        activityPostcard.navigation(this);
    }
}

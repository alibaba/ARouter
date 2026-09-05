package com.alibaba.android.arouter.configcache;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class AndroidXConsumerInstrumentedTest {
    @Test
    public void publishedArtifactsRouteWithConsumerAndroidXDependencies() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        // Use component names instead of target-APK API calls. Release tests
        // must exercise the real consumer R8 rules, without test-only keep rules.
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(
                "com.alibaba.android.arouter.configcache.SecondActivity", null, false);
        Activity probe = null;
        Activity destination = null;
        try {
            Intent intent = new Intent();
            intent.setClassName(instrumentation.getTargetContext(),
                    "com.alibaba.android.arouter.configcache.ProbeActivity");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            probe = instrumentation.startActivitySync(intent);
            // ProbeActivity only navigates after provider registration, Fragment
            // arguments/injection and ActivityOptionsCompat have all succeeded.
            destination = monitor.waitForActivityWithTimeout(10000);
            assertNotNull("AndroidX consumer navigation did not arrive", destination);
        } finally {
            finish(instrumentation, destination);
            finish(instrumentation, probe);
            instrumentation.removeMonitor(monitor);
        }
    }

    private static void finish(Instrumentation instrumentation, final Activity activity) {
        if (activity != null) {
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    activity.finish();
                }
            });
            instrumentation.waitForIdleSync();
        }
    }
}

package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.alibaba.android.arouter.demo.module1.testactivity.Test2Activity;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PostcardArrayExtrasTest {
    private final List<Activity> activities = new ArrayList<Activity>();

    @BeforeClass
    public static void initializeRouter() {
        Application application = (Application) InstrumentationRegistry.getTargetContext().getApplicationContext();
        ARouter.openDebug();
        ARouter.openLog();
        ARouter.init(application);
    }

    @After
    public void finishActivities() {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        for (int i = activities.size() - 1; i >= 0; i--) {
            final Activity activity = activities.get(i);
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    activity.finish();
                }
            });
        }
        instrumentation.waitForIdleSync();
    }

    @Test
    public void allMissingBundleArrayTypesReachTheDestinationActivity() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test2Activity.class.getName(), null, false);

        try {
            Intent hostIntent = new Intent(InstrumentationRegistry.getTargetContext(), PostcardArrayHostActivity.class);
            hostIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activities.add(instrumentation.startActivitySync(hostIntent));

            Activity destination = monitor.waitForActivityWithTimeout(5000);
            assertTrue("destination activity was not launched", null != destination);
            activities.add(destination);

            Intent intent = destination.getIntent();
            assertArrayEquals(new String[]{"alpha", null, "omega"}, intent.getStringArrayExtra("strings"));
            assertArrayEquals(new int[]{-7, 0, 42}, intent.getIntArrayExtra("ints"));
            assertArrayEquals(new long[]{Long.MIN_VALUE, 0L, Long.MAX_VALUE}, intent.getLongArrayExtra("longs"));
            assertArrayEquals(new double[]{-1.5d, 0.0d, Math.PI}, intent.getDoubleArrayExtra("doubles"), 0.0d);
            assertTrue(intent.hasExtra("nullStrings"));
            assertNull(intent.getStringArrayExtra("nullStrings"));

            instrumentation.waitForIdleSync();
            assertEquals(1, monitor.getHits());
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }
}

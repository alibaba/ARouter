package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alibaba.android.arouter.demo.module1.testactivity.ReorderProbeActivity;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ActivityTaskFlagsInstrumentedTest {
    private Instrumentation instrumentation;
    private Application application;
    private ActivityResultHostActivity host;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        application = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        ARouter.openDebug();
        ARouter.init(application);
    }

    @After
    public void tearDown() {
        final ReorderProbeActivity probe = ReorderProbeActivity.currentActivity();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                if (null != probe && !probe.isFinishing()) {
                    probe.finish();
                }
                if (null != host && !host.isFinishing()) {
                    host.finish();
                }
            }
        });
        instrumentation.waitForIdleSync();
    }

    @Test
    public void activityContextReordersExistingInstance() throws Exception {
        final ReorderProbeActivity firstInstance = prepareStack();
        bringHostToFront(firstInstance);
        ReorderProbeActivity.expectLaunch();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance()
                        .build("/task/reorder")
                        .withFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        .navigation(host);
            }
        });

        assertTrue(ReorderProbeActivity.awaitLaunch());
        assertSame(firstInstance, ReorderProbeActivity.currentActivity());
        assertEquals(1, ReorderProbeActivity.creationCount());
        assertEquals(1, ReorderProbeActivity.newIntentCount());
        assertEquals(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
                withoutSystemAddedFlags(ReorderProbeActivity.lastIntent().getFlags())
        );
    }

    @Test
    public void applicationContextMatchesDirectIntent() throws Exception {
        LaunchOutcome router = runApplicationContextScenario(true);
        LaunchOutcome direct = runApplicationContextScenario(false);

        assertEquals(1, router.created + router.newIntent);
        assertEquals(router.created, direct.created);
        assertEquals(router.newIntent, direct.newIntent);
        assertEquals(router.reused, direct.reused);
        assertEquals(withoutSystemAddedFlags(router.flags), withoutSystemAddedFlags(direct.flags));
        assertEquals(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK,
                withoutSystemAddedFlags(router.flags)
        );
        assertEquals(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK,
                withoutSystemAddedFlags(direct.flags)
        );
    }

    private static int withoutSystemAddedFlags(int flags) {
        // Android may mark an Intent delivered to an existing task as BROUGHT_TO_FRONT.
        // The bit describes task-manager history; neither ARouter nor the direct caller requested it.
        return flags & ~Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT;
    }

    private LaunchOutcome runApplicationContextScenario(final boolean useRouter) throws Exception {
        final ReorderProbeActivity firstInstance = prepareStack();
        bringHostToFront(firstInstance);
        final int creationsBefore = ReorderProbeActivity.creationCount();
        final int newIntentsBefore = ReorderProbeActivity.newIntentCount();
        ReorderProbeActivity.expectLaunch();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                if (useRouter) {
                    ARouter.getInstance()
                            .build("/task/reorder")
                            .withFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            .navigation(application);
                } else {
                    Intent direct = new Intent(application, ReorderProbeActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                    application.startActivity(direct);
                }
            }
        });

        assertTrue(ReorderProbeActivity.awaitLaunch());
        ReorderProbeActivity current = ReorderProbeActivity.currentActivity();
        assertNotNull(current);
        return new LaunchOutcome(
                ReorderProbeActivity.creationCount() - creationsBefore,
                ReorderProbeActivity.newIntentCount() - newIntentsBefore,
                current == firstInstance,
                ReorderProbeActivity.lastIntent().getFlags()
        );
    }

    private ReorderProbeActivity prepareStack() throws Exception {
        Intent hostIntent = new Intent(application, ActivityResultHostActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        host = (ActivityResultHostActivity) instrumentation.startActivitySync(hostIntent);
        instrumentation.waitForIdleSync();
        ReorderProbeActivity.resetProbe();

        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ARouter.getInstance().build("/task/reorder").navigation(host);
            }
        });
        assertTrue(ReorderProbeActivity.awaitCreated());
        ReorderProbeActivity probe = ReorderProbeActivity.currentActivity();
        assertNotNull(probe);
        assertEquals(1, ReorderProbeActivity.creationCount());
        return probe;
    }

    private void bringHostToFront(final ReorderProbeActivity probe) throws Exception {
        final CountDownLatch resumed = new CountDownLatch(1);
        Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                if (activity == host) {
                    resumed.countDown();
                }
            }

            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        };
        application.registerActivityLifecycleCallbacks(callbacks);
        try {
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(probe, ActivityResultHostActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    probe.startActivity(intent);
                }
            });
            assertTrue(resumed.await(5, TimeUnit.SECONDS));
            instrumentation.waitForIdleSync();
        } finally {
            application.unregisterActivityLifecycleCallbacks(callbacks);
        }
    }

    private static final class LaunchOutcome {
        private final int created;
        private final int newIntent;
        private final boolean reused;
        private final int flags;

        private LaunchOutcome(int created, int newIntent, boolean reused, int flags) {
            this.created = created;
            this.newIntent = newIntent;
            this.reused = reused;
            this.flags = flags;
        }
    }
}

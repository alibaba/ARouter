package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleCallback;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitor;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.alibaba.android.arouter.demo.module1.testactivity.RedirectLoginActivity;
import com.alibaba.android.arouter.demo.module1.testactivity.RedirectProtectedActivity;
import com.alibaba.android.arouter.demo.module1.testactivity.Test2Activity;
import com.alibaba.android.arouter.demo.module1.testinterceptor.LoginRedirectInterceptor;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class InterceptorRedirectInstrumentedTest {
    private static final int REDIRECT_COUNT = 12;

    private Instrumentation instrumentation;
    private Activity currentActivity;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        Application application =
                (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        ARouter.openDebug();
        ARouter.init(application);
        LoginRedirectInterceptor.resetProbe();
    }

    @After
    public void tearDown() throws InterruptedException {
        try {
            finishCurrentActivity();
        } finally {
            LoginRedirectInterceptor.resetProbe();
        }
    }

    @Test
    public void redirectStartsLoginAndInterruptsOriginalNavigationOnce() throws Exception {
        Instrumentation.ActivityMonitor loginMonitor =
                instrumentation.addMonitor(RedirectLoginActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor protectedMonitor =
                instrumentation.addMonitor(RedirectProtectedActivity.class.getName(), null, false);
        RecordingNavigationCallback callback = new RecordingNavigationCallback();

        try {
            navigateProtected(callback);
            currentActivity = loginMonitor.waitForActivityWithTimeout(5000);

            assertNotNull(currentActivity);
            assertTrue(callback.await());
            instrumentation.waitForIdleSync();
            assertEquals(1, loginMonitor.getHits());
            assertEquals(0, protectedMonitor.getHits());
            assertEquals(1, LoginRedirectInterceptor.protectedProcessCount());
            assertTrue(callback.found.get());
            assertTrue(callback.interrupted.get());
            assertFalse(callback.lost.get());
            assertFalse(callback.arrived.get());
        } finally {
            instrumentation.removeMonitor(loginMonitor);
            instrumentation.removeMonitor(protectedMonitor);
        }
    }

    @Test
    public void repeatedRedirectsDoNotBlockLaterNavigation() throws Exception {
        Instrumentation.ActivityMonitor protectedMonitor =
                instrumentation.addMonitor(RedirectProtectedActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor normalMonitor =
                instrumentation.addMonitor(Test2Activity.class.getName(), null, false);

        try {
            Activity previousLogin = null;
            for (int index = 0; index < REDIRECT_COUNT; index++) {
                // ActivityMonitor can record the same instance during both
                // creation and resume. Reusing it can mistake the previous
                // login's resume for the next redirect's arrival.
                Instrumentation.ActivityMonitor loginMonitor =
                        instrumentation.addMonitor(RedirectLoginActivity.class.getName(), null, false);
                try {
                    RecordingNavigationCallback redirect = new RecordingNavigationCallback();
                    navigateProtected(redirect);
                    currentActivity = instrumentation.waitForMonitorWithTimeout(loginMonitor, 5000);

                    assertNotNull("Login redirect " + index + " did not arrive", currentActivity);
                    assertNotSame("A redirect reused the previous monitor result", previousLogin, currentActivity);
                    previousLogin = currentActivity;
                    assertTrue(redirect.await());
                    assertTrue(redirect.interrupted.get());
                    assertFalse(redirect.arrived.get());
                    assertEquals(index + 1, LoginRedirectInterceptor.protectedProcessCount());
                } finally {
                    instrumentation.removeMonitor(loginMonitor);
                }
                finishCurrentActivity();
            }

            assertEquals(0, protectedMonitor.getHits());
            assertEquals(REDIRECT_COUNT, LoginRedirectInterceptor.protectedProcessCount());

            RecordingNavigationCallback normal = new RecordingNavigationCallback();
            Postcard normalPostcard = ARouter.getInstance().build("/test/activity2");
            LoginRedirectInterceptor.watchPostcard(normalPostcard);
            normalPostcard.navigation(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    normal
            );
            currentActivity = normalMonitor.waitForActivityWithTimeout(5000);

            assertNotNull(currentActivity);
            assertTrue(normal.await());
            assertTrue(normal.found.get());
            assertTrue(normal.arrived.get());
            assertFalse(normal.interrupted.get());
            assertEquals(1, LoginRedirectInterceptor.watchedPostcardProcessCount());
        } finally {
            instrumentation.removeMonitor(protectedMonitor);
            instrumentation.removeMonitor(normalMonitor);
        }
    }

    private void navigateProtected(RecordingNavigationCallback callback) {
        ARouter.getInstance()
                .build("/redirect/protected")
                .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), callback);
    }

    private void finishCurrentActivity() throws InterruptedException {
        final Activity activity = currentActivity;
        currentActivity = null;
        if (activity == null) {
            return;
        }

        final CountDownLatch destroyed = new CountDownLatch(1);
        final ActivityLifecycleMonitor lifecycle = ActivityLifecycleMonitorRegistry.getInstance();
        final ActivityLifecycleCallback callback = new ActivityLifecycleCallback() {
            @Override
            public void onActivityLifecycleChanged(Activity changed, Stage stage) {
                if (changed == activity && stage == Stage.DESTROYED) {
                    destroyed.countDown();
                }
            }
        };
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                lifecycle.addLifecycleCallback(callback);
                if (lifecycle.getLifecycleStageOf(activity) == Stage.DESTROYED) {
                    destroyed.countDown();
                } else {
                    activity.finish();
                }
            }
        });
        try {
            // An idle main queue does not mean the platform has completed the
            // close transaction. Wait before installing the next login monitor.
            assertTrue("The previous activity was not destroyed", destroyed.await(5, TimeUnit.SECONDS));
            instrumentation.waitForIdleSync();
        } finally {
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    lifecycle.removeLifecycleCallback(callback);
                }
            });
        }
    }

    private static final class RecordingNavigationCallback implements NavigationCallback {
        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicBoolean found = new AtomicBoolean();
        private final AtomicBoolean lost = new AtomicBoolean();
        private final AtomicBoolean arrived = new AtomicBoolean();
        private final AtomicBoolean interrupted = new AtomicBoolean();

        @Override
        public void onFound(Postcard postcard) {
            found.set(true);
        }

        @Override
        public void onLost(Postcard postcard) {
            lost.set(true);
            completed.countDown();
        }

        @Override
        public void onArrival(Postcard postcard) {
            arrived.set(true);
            completed.countDown();
        }

        @Override
        public void onInterrupt(Postcard postcard) {
            interrupted.set(true);
            completed.countDown();
        }

        private boolean await() throws InterruptedException {
            return completed.await(5, TimeUnit.SECONDS);
        }
    }
}

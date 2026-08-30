package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

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
                (Application) InstrumentationRegistry.getTargetContext().getApplicationContext();
        ARouter.openDebug();
        ARouter.init(application);
        LoginRedirectInterceptor.resetProbe();
    }

    @After
    public void tearDown() {
        finishCurrentActivity();
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
            assertEquals(1, LoginRedirectInterceptor.processCount());
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
        Instrumentation.ActivityMonitor loginMonitor =
                instrumentation.addMonitor(RedirectLoginActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor protectedMonitor =
                instrumentation.addMonitor(RedirectProtectedActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor normalMonitor =
                instrumentation.addMonitor(Test2Activity.class.getName(), null, false);

        try {
            for (int index = 0; index < REDIRECT_COUNT; index++) {
                RecordingNavigationCallback redirect = new RecordingNavigationCallback();
                navigateProtected(redirect);
                currentActivity = loginMonitor.waitForActivityWithTimeout(5000);

                assertNotNull(currentActivity);
                assertTrue(redirect.await());
                assertTrue(redirect.interrupted.get());
                assertFalse(redirect.arrived.get());
                assertEquals(index + 1, LoginRedirectInterceptor.processCount());
                finishCurrentActivity();
            }

            assertEquals(0, protectedMonitor.getHits());
            assertEquals(REDIRECT_COUNT, LoginRedirectInterceptor.processCount());

            RecordingNavigationCallback normal = new RecordingNavigationCallback();
            ARouter.getInstance()
                    .build("/test/activity2")
                    .navigation(InstrumentationRegistry.getTargetContext(), normal);
            currentActivity = normalMonitor.waitForActivityWithTimeout(5000);

            assertNotNull(currentActivity);
            assertTrue(normal.await());
            assertTrue(normal.found.get());
            assertTrue(normal.arrived.get());
            assertFalse(normal.interrupted.get());
            assertEquals(REDIRECT_COUNT + 1, LoginRedirectInterceptor.processCount());
        } finally {
            instrumentation.removeMonitor(loginMonitor);
            instrumentation.removeMonitor(protectedMonitor);
            instrumentation.removeMonitor(normalMonitor);
        }
    }

    private void navigateProtected(RecordingNavigationCallback callback) {
        ARouter.getInstance()
                .build("/redirect/protected")
                .navigation(InstrumentationRegistry.getTargetContext(), callback);
    }

    private void finishCurrentActivity() {
        final Activity activity = currentActivity;
        currentActivity = null;
        if (activity == null || activity.isFinishing()) {
            return;
        }

        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.finish();
            }
        });
        instrumentation.waitForIdleSync();
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

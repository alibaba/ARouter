package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alibaba.android.arouter.demo.module1.BlankFragment;
import com.alibaba.android.arouter.demo.module1.testactivity.Test2Activity;
import com.alibaba.android.arouter.demo.service.HelloService;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.facade.callback.NavigationLauncher;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ActivityResultNavigationTest {
    private Activity activityToFinish;

    @BeforeClass
    public static void initializeRouter() {
        Application application = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        ARouter.openDebug();
        ARouter.openLog();
        ARouter.init(application);
    }

    @After
    public void finishActivity() {
        if (null == activityToFinish) {
            return;
        }

        final Activity activity = activityToFinish;
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.finish();
            }
        });
        instrumentation.waitForIdleSync();
    }

    @Test
    public void launcherReceivesCompletedIntentOnceOnMainThreadWithoutFallbackLaunch() throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test2Activity.class.getName(), null, false);
        final AtomicInteger launches = new AtomicInteger();
        final AtomicReference<Intent> launchedIntent = new AtomicReference<Intent>();
        final AtomicBoolean launchedOnMainThread = new AtomicBoolean();
        final CountDownLatch completed = new CountDownLatch(1);
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());

        try {
            ARouter.getInstance()
                    .build("/test/activity2")
                    .withString("key1", "launcher-value")
                    .withAction("com.alibaba.android.arouter.TEST_RESULT")
                    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), new NavigationLauncher() {
                        @Override
                        public void launch(Intent intent) {
                            events.add("launch");
                            launches.incrementAndGet();
                            launchedIntent.set(intent);
                            launchedOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
                        }
                    }, new RecordingCallback(events, completed));

            assertTrue(completed.await(5, TimeUnit.SECONDS));
            instrumentation.waitForIdleSync();
            assertNull(monitor.waitForActivityWithTimeout(500));
            assertEquals(0, monitor.getHits());
            assertEquals(1, launches.get());
            assertTrue(launchedOnMainThread.get());
            assertEquals(asList("found", "launch", "arrival"), events);

            Intent intent = launchedIntent.get();
            assertNotNull(intent);
            assertNotNull(intent.getComponent());
            assertEquals(Test2Activity.class.getName(), intent.getComponent().getClassName());
            assertEquals("launcher-value", intent.getStringExtra("key1"));
            assertEquals("com.alibaba.android.arouter.TEST_RESULT", intent.getAction());
            assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
            assertFalse((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void callerOwnedLauncherRemovesForwardResultAndReceivesResultExactlyOnce() throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        ActivityResultHostActivity host = launchHostActivity(instrumentation);
        activityToFinish = host;
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test2Activity.class.getName(), null, false);
        final CountDownLatch navigated = new CountDownLatch(1);
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());

        try {
            final Postcard postcard = ARouter.getInstance()
                    .build("/test/activity2")
                    .withString("key1", "result-value")
                    .withFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            final ActivityResultHostActivity resultHost = host;
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    resultHost.navigateForResult(postcard, new RecordingCallback(events, navigated));
                }
            });

            Activity target = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(target);
            assertTrue(navigated.await(5, TimeUnit.SECONDS));
            assertEquals("result-value", target.getIntent().getStringExtra("key1"));
            assertEquals(0, target.getIntent().getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            assertTrue((target.getIntent().getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
            assertTrue((postcard.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
            assertEquals(1, monitor.getHits());
            assertEquals(asList("found", "arrival"), events);

            final Activity resultActivity = target;
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    resultActivity.finish();
                }
            });

            assertTrue(host.awaitResult());
            assertEquals(ActivityResultHostActivity.REQUEST_CODE, host.getReceivedRequestCode());
            assertEquals(999, host.getReceivedResultCode());
            assertNull(host.getReceivedData());
            instrumentation.waitForIdleSync();
            assertEquals(1, monitor.getHits());
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void legacyRequestCodeNavigationStillReturnsResultExactlyOnce() throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        ActivityResultHostActivity host = launchHostActivity(instrumentation);
        activityToFinish = host;
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test2Activity.class.getName(), null, false);
        final CountDownLatch navigated = new CountDownLatch(1);
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());

        try {
            final Postcard postcard = ARouter.getInstance()
                    .build("/test/activity2")
                    .withString("key1", "legacy-result-value");
            final ActivityResultHostActivity resultHost = host;
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    resultHost.navigateWithLegacyRequestCode(postcard, new RecordingCallback(events, navigated));
                }
            });

            Activity target = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(target);
            assertTrue(navigated.await(5, TimeUnit.SECONDS));
            assertEquals("legacy-result-value", target.getIntent().getStringExtra("key1"));
            assertEquals(1, monitor.getHits());
            assertEquals(asList("found", "arrival"), events);

            final Activity resultActivity = target;
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    resultActivity.finish();
                }
            });

            assertTrue(host.awaitResult());
            assertEquals(ActivityResultHostActivity.REQUEST_CODE, host.getReceivedRequestCode());
            assertEquals(999, host.getReceivedResultCode());
            assertNull(host.getReceivedData());
            instrumentation.waitForIdleSync();
            assertEquals(1, monitor.getHits());
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void requestCodeNavigationRemovesForwardResultFlagAndReturnsResult() throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        ActivityResultHostActivity host = launchHostActivity(instrumentation);
        activityToFinish = host;
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test2Activity.class.getName(), null, false);
        final CountDownLatch navigated = new CountDownLatch(1);
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());
        final AtomicReference<Throwable> launchFailure = new AtomicReference<Throwable>();

        try {
            final Postcard postcard = ARouter.getInstance()
                    .build("/test/activity2")
                    .withString("key1", "forward-result-value")
                    .withFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            final ActivityResultHostActivity resultHost = host;
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    try {
                        resultHost.navigateWithLegacyRequestCode(
                                postcard,
                                new RecordingCallback(events, navigated)
                        );
                    } catch (Throwable failure) {
                        launchFailure.set(failure);
                    }
                }
            });

            assertNull("navigation must not pass conflicting flags to Android", launchFailure.get());
            Activity target = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(target);
            assertTrue(navigated.await(5, TimeUnit.SECONDS));
            assertEquals("forward-result-value", target.getIntent().getStringExtra("key1"));
            assertEquals(0, target.getIntent().getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            assertTrue((target.getIntent().getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
            assertTrue((postcard.getFlags() & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0);
            assertEquals(1, monitor.getHits());
            assertEquals(asList("found", "arrival"), events);

            final Activity resultActivity = target;
            instrumentation.runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    resultActivity.finish();
                }
            });

            assertTrue(host.awaitResult());
            assertEquals(ActivityResultHostActivity.REQUEST_CODE, host.getReceivedRequestCode());
            assertEquals(999, host.getReceivedResultCode());
            assertNull(host.getReceivedData());
            instrumentation.waitForIdleSync();
            assertEquals(1, monitor.getHits());
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void legacyApplicationContextNavigationStillAddsNewTaskAndLaunchesOnce() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test2Activity.class.getName(), null, false);
        CountDownLatch completed = new CountDownLatch(1);
        List<String> events = Collections.synchronizedList(new ArrayList<String>());

        try {
            ARouter.getInstance()
                    .build("/test/activity2")
                    .withString("key1", "legacy-value")
                    .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), new RecordingCallback(events, completed));

            Activity target = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(target);
            activityToFinish = target;
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals("legacy-value", target.getIntent().getStringExtra("key1"));
            assertTrue((target.getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
            instrumentation.waitForIdleSync();
            assertEquals(1, monitor.getHits());
            assertEquals(asList("found", "arrival"), events);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void nonActivityRoutesReturnNormallyWithoutInvokingLauncher() {
        final AtomicInteger launches = new AtomicInteger();
        NavigationLauncher launcher = new NavigationLauncher() {
            @Override
            public void launch(Intent intent) {
                launches.incrementAndGet();
            }
        };

        Object fragment = ARouter.getInstance()
                .build("/test/fragment")
                .withString("name", "fragment-value")
                .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), launcher, null);
        Object provider = ARouter.getInstance()
                .build("/yourservicegroupname/hello")
                .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), launcher, null);

        assertTrue(fragment instanceof BlankFragment);
        Bundle arguments = ((BlankFragment) fragment).getArguments();
        assertNotNull(arguments);
        assertEquals("fragment-value", arguments.getString("name"));
        assertTrue(provider instanceof HelloService);
        assertEquals(0, launches.get());
    }

    @Test
    public void missingRouteReportsLostWithoutInvokingLauncher() throws Exception {
        final AtomicInteger launches = new AtomicInteger();
        CountDownLatch completed = new CountDownLatch(1);
        List<String> events = Collections.synchronizedList(new ArrayList<String>());

        ARouter.getInstance()
                .build("/missing/activity-result-route")
                .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), new NavigationLauncher() {
                    @Override
                    public void launch(Intent intent) {
                        launches.incrementAndGet();
                    }
                }, new RecordingCallback(events, completed));

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(0, launches.get());
        assertEquals(asList("lost"), events);
    }

    private static ActivityResultHostActivity launchHostActivity(Instrumentation instrumentation) {
        Intent intent = new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), ActivityResultHostActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return (ActivityResultHostActivity) instrumentation.startActivitySync(intent);
    }

    private static List<String> asList(String... values) {
        List<String> result = new ArrayList<String>();
        Collections.addAll(result, values);
        return result;
    }

    private static final class RecordingCallback implements NavigationCallback {
        private final List<String> events;
        private final CountDownLatch completed;

        RecordingCallback(List<String> events, CountDownLatch completed) {
            this.events = events;
            this.completed = completed;
        }

        @Override
        public void onFound(Postcard postcard) {
            events.add("found");
        }

        @Override
        public void onLost(Postcard postcard) {
            events.add("lost");
            completed.countDown();
        }

        @Override
        public void onArrival(Postcard postcard) {
            events.add("arrival");
            completed.countDown();
        }

        @Override
        public void onInterrupt(Postcard postcard) {
            events.add("interrupt");
            completed.countDown();
        }
    }
}

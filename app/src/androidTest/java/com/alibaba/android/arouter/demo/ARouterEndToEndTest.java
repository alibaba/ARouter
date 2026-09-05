package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alibaba.android.arouter.demo.kotlin.KotlinTestActivity;
import com.alibaba.android.arouter.demo.kotlin.RecordingPretreatmentService;
import com.alibaba.android.arouter.demo.module1.BlankFragment;
import com.alibaba.android.arouter.demo.module1.testactivity.Test1Activity;
import com.alibaba.android.arouter.demo.module1.testactivity.Test2Activity;
import com.alibaba.android.arouter.demo.service.HelloService;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.launcher.ARouter;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ARouterEndToEndTest {

    @BeforeClass
    public static void initializeRouter() {
        Application application = (Application) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        ARouter.openDebug();
        ARouter.openLog();
        ARouter.init(application);
    }

    @After
    public void clearPretreatmentRecording() {
        RecordingPretreatmentService.stopRecording();
    }

    @Test
    public void providerAndFragmentRoutesResolve() {
        HelloService service = ARouter.getInstance().navigation(HelloService.class);
        Object fragment = ARouter.getInstance()
                .build("/test/fragment")
                .withString("name", "fragment-user")
                .navigation();

        assertNotNull(service);
        assertTrue(fragment instanceof Fragment);
        assertTrue(fragment instanceof BlankFragment);
        Bundle arguments = ((BlankFragment) fragment).getArguments();
        assertNotNull(arguments);
        assertEquals("fragment-user", arguments.getString("name"));
    }

    @Test
    public void androidXActivityOptionsRemainPartOfPostcardContract() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                context,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        Postcard postcard = ARouter.getInstance().build("/test/activity2");

        assertSame(postcard, postcard.withOptionsCompat(options));
        assertNotNull(postcard.getOptionsBundle());
    }

    @Test
    public void kotlinPretreatmentUsesApplicationContextForContextlessNavigation() {
        RecordingPretreatmentService.startRecording();
        Context applicationContext = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();

        Object fragment = ARouter.getInstance()
                .build("/test/fragment")
                .navigation();

        assertTrue(fragment instanceof BlankFragment);
        assertEquals(1, RecordingPretreatmentService.invocationCount);
        assertSame(applicationContext, RecordingPretreatmentService.lastContext);
        assertNotNull(RecordingPretreatmentService.lastPostcard);
        assertEquals("/test/fragment", RecordingPretreatmentService.lastPostcard.getPath());
    }

    @Test
    public void kotlinPretreatmentPreservesExplicitNavigationContext() {
        RecordingPretreatmentService.startRecording();
        Context explicitContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Object fragment = ARouter.getInstance()
                .build("/test/fragment")
                .navigation(explicitContext);

        assertTrue(fragment instanceof BlankFragment);
        assertEquals(1, RecordingPretreatmentService.invocationCount);
        assertSame(explicitContext, RecordingPretreatmentService.lastContext);
        assertNotNull(RecordingPretreatmentService.lastPostcard);
        assertEquals("/test/fragment", RecordingPretreatmentService.lastPostcard.getPath());
    }

    @Test
    public void javaActivityRouteRunsInterceptorsAndAutowiredInjection() throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test1Activity.class.getName(), null, false);
        final RecordingNavigationCallback callback = new RecordingNavigationCallback();

        try {
            ARouter.getInstance()
                    .build("/test/activity1")
                    .withString("name", "java-user")
                    .withInt("age", 27)
                    .withBoolean("boy", true)
                    .withLong("high", 181)
                    .withString("url", "https://example.test")
                    .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), callback);

            Activity activity = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(activity);
            assertTrue(callback.await());
            assertTrue(callback.found.get());
            assertTrue(callback.arrived.get());
            assertFalse(callback.lost.get());
            assertFalse(callback.interrupted.get());
            assertEquals("java-user", activity.getIntent().getStringExtra("name"));
            assertEquals(27, activity.getIntent().getIntExtra("age", -1));
            String renderedParameters = readText(
                    instrumentation,
                    (TextView) activity.findViewById(com.alibaba.android.arouter.demo.module1.R.id.test2)
            );
            assertTrue(renderedParameters.contains("name=java-user"));
            assertTrue(renderedParameters.contains("age=27"));
            assertTrue(renderedParameters.contains("girl=true"));
            assertTrue(renderedParameters.contains("url=https://example.test"));
            finishActivity(instrumentation, activity);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void kotlinActivityRouteRunsInterceptorsAndAutowiredInjection() throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(KotlinTestActivity.class.getName(), null, false);

        try {
            ARouter.getInstance()
                    .build("/kotlin/test")
                    .withString("name", "kotlin-user")
                    .withInt("age", 23)
                    .navigation();

            Activity activity = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(activity);
            String renderedParameters = readText(
                    instrumentation,
                    (TextView) activity.findViewById(com.alibaba.android.arouter.demo.kotlin.R.id.content)
            );
            assertEquals("name = kotlin-user, age = 23", renderedParameters);
            finishActivity(instrumentation, activity);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void normalActivityRouteArrivesAfterPassingInterceptors() throws Exception {
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(Test2Activity.class.getName(), null, false);
        final RecordingNavigationCallback callback = new RecordingNavigationCallback();

        try {
            ARouter.getInstance()
                    .build("/test/activity2")
                    .withString("key1", "interceptor-chain-passed")
                    .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), callback);

            Activity activity = monitor.waitForActivityWithTimeout(5000);
            assertNotNull(activity);
            assertTrue(callback.await());
            instrumentation.waitForIdleSync();
            Thread.sleep(100);
            assertEquals(1, monitor.getHits());
            assertTrue(callback.found.get());
            assertTrue(callback.arrived.get());
            assertFalse(callback.lost.get());
            assertFalse(callback.interrupted.get());
            assertNotNull(callback.foundPostcard.get());
            assertEquals(7, callback.foundPostcard.get().getPriority());
            assertEquals("interceptor-chain-passed", activity.getIntent().getStringExtra("key1"));
            finishActivity(instrumentation, activity);
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void missingRouteReportsLostWithoutArrival() throws Exception {
        RecordingNavigationCallback callback = new RecordingNavigationCallback();

        ARouter.getInstance()
                .build("/missing/route")
                .navigation(InstrumentationRegistry.getInstrumentation().getTargetContext(), callback);

        assertTrue(callback.await());
        assertTrue(callback.lost.get());
        assertFalse(callback.found.get());
        assertFalse(callback.arrived.get());
        assertFalse(callback.interrupted.get());
    }

    private static void finishActivity(Instrumentation instrumentation, final Activity activity) {
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                activity.finish();
            }
        });
        instrumentation.waitForIdleSync();
    }

    private static String readText(Instrumentation instrumentation, final TextView textView) {
        final AtomicReference<String> text = new AtomicReference<String>();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                text.set(textView.getText().toString());
            }
        });
        return text.get();
    }

    private static final class RecordingNavigationCallback implements NavigationCallback {
        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicBoolean found = new AtomicBoolean();
        private final AtomicBoolean lost = new AtomicBoolean();
        private final AtomicBoolean arrived = new AtomicBoolean();
        private final AtomicBoolean interrupted = new AtomicBoolean();
        private final AtomicReference<Postcard> foundPostcard = new AtomicReference<Postcard>();

        @Override
        public void onFound(Postcard postcard) {
            foundPostcard.set(postcard);
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

        boolean await() throws InterruptedException {
            return completed.await(5, TimeUnit.SECONDS);
        }
    }
}

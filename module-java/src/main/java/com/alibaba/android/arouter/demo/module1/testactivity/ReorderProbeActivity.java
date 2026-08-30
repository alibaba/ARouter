package com.alibaba.android.arouter.demo.module1.testactivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.alibaba.android.arouter.facade.annotation.Route;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Activity used to verify task-stack flags in the demo instrumentation tests.
 */
@Route(path = "/task/reorder")
public class ReorderProbeActivity extends Activity {
    private static final AtomicInteger CREATIONS = new AtomicInteger();
    private static final AtomicInteger NEW_INTENTS = new AtomicInteger();
    private static final AtomicReference<ReorderProbeActivity> CURRENT =
            new AtomicReference<ReorderProbeActivity>();
    private static final AtomicReference<Intent> LAST_INTENT = new AtomicReference<Intent>();
    private static volatile CountDownLatch created = new CountDownLatch(1);
    private static volatile CountDownLatch launch = new CountDownLatch(1);

    public static void resetProbe() {
        CREATIONS.set(0);
        NEW_INTENTS.set(0);
        CURRENT.set(null);
        LAST_INTENT.set(null);
        created = new CountDownLatch(1);
        launch = new CountDownLatch(1);
    }

    public static void expectLaunch() {
        launch = new CountDownLatch(1);
    }

    public static boolean awaitCreated() throws InterruptedException {
        return created.await(5, TimeUnit.SECONDS);
    }

    public static boolean awaitLaunch() throws InterruptedException {
        return launch.await(5, TimeUnit.SECONDS);
    }

    public static int creationCount() {
        return CREATIONS.get();
    }

    public static int newIntentCount() {
        return NEW_INTENTS.get();
    }

    public static ReorderProbeActivity currentActivity() {
        return CURRENT.get();
    }

    public static Intent lastIntent() {
        return LAST_INTENT.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CREATIONS.incrementAndGet();
        CURRENT.set(this);
        LAST_INTENT.set(getIntent());
        created.countDown();
        launch.countDown();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        NEW_INTENTS.incrementAndGet();
        CURRENT.set(this);
        LAST_INTENT.set(intent);
        launch.countDown();
    }

    @Override
    protected void onDestroy() {
        CURRENT.compareAndSet(this, null);
        super.onDestroy();
    }
}

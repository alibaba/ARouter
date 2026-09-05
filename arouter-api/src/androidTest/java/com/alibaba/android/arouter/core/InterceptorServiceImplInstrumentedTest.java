package com.alibaba.android.arouter.core;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.ILogger;
import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.DefaultLogger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class InterceptorServiceImplInstrumentedTest {
    private ThreadPoolExecutor previousExecutor;
    private ThreadPoolExecutor testExecutor;
    private ILogger previousLogger;

    @Before
    public void setUp() {
        Warehouse.clear();
        PassingInterceptor.reset();
        BlockingInitInterceptor.reset();

        previousExecutor = LogisticsCenter.executor;
        previousLogger = ARouter.logger;
        ARouter.logger = new DefaultLogger();
        testExecutor = new ThreadPoolExecutor(
                6,
                6,
                1,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>()
        );
        LogisticsCenter.executor = testExecutor;
    }

    @After
    public void tearDown() throws Exception {
        BlockingInitInterceptor.releaseInitialization();
        testExecutor.shutdownNow();
        assertTrue(testExecutor.awaitTermination(5, TimeUnit.SECONDS));
        LogisticsCenter.executor = previousExecutor;
        ARouter.logger = previousLogger;
        Warehouse.clear();
    }

    @Test
    public void successfulInitializationRunsTheCompleteInterceptorChain() throws Exception {
        Warehouse.interceptorsIndex.put(1, PassingInterceptor.class);
        Warehouse.interceptorsIndex.put(2, SecondPassingInterceptor.class);
        InterceptorServiceImpl service = new InterceptorServiceImpl();
        RecordingCallback callback = new RecordingCallback();
        Postcard postcard = new Postcard("/test/success", "test");

        service.init(targetContext());
        service.doInterceptions(postcard, callback);

        assertTrue(callback.await());
        assertSame(postcard, callback.continued.get());
        assertNull(callback.interrupted.get());
        assertEquals(2, PassingInterceptor.processCount.get());
    }

    @Test
    public void retiredInitializationDoesNotModifyTheReplacementService() throws Exception {
        ThreadPoolExecutor retiringExecutor = new ThreadPoolExecutor(
                1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()
        );
        try {
            Warehouse.interceptorsIndex.put(1, BlockingInitInterceptor.class);
            LogisticsCenter.executor = retiringExecutor;
            new InterceptorServiceImpl().init(targetContext());
            assertTrue(BlockingInitInterceptor.awaitInitializationStart());

            // Debug destroy/re-init clears the registry while the old initializer can still run.
            Warehouse.clear();
            Warehouse.interceptorsIndex.put(1, PassingInterceptor.class);
            LogisticsCenter.executor = testExecutor;
            InterceptorServiceImpl replacement = new InterceptorServiceImpl();
            replacement.init(targetContext());

            RecordingCallback firstCallback = new RecordingCallback();
            replacement.doInterceptions(new Postcard("/test/replacement/first", "test"), firstCallback);
            assertTrue(firstCallback.await());
            assertNull(firstCallback.interrupted.get());
            assertEquals(1, PassingInterceptor.processCount.get());

            BlockingInitInterceptor.releaseInitialization();
            retiringExecutor.shutdown();
            assertTrue(retiringExecutor.awaitTermination(5, TimeUnit.SECONDS));

            PassingInterceptor.reset();
            RecordingCallback secondCallback = new RecordingCallback();
            Postcard postcard = new Postcard("/test/replacement/second", "test");
            replacement.doInterceptions(postcard, secondCallback);
            assertTrue(secondCallback.await());
            assertSame(postcard, secondCallback.continued.get());
            assertNull(secondCallback.interrupted.get());
            assertEquals(1, PassingInterceptor.processCount.get());
            assertEquals(0, BlockingInitInterceptor.processCount.get());
        } finally {
            BlockingInitInterceptor.releaseInitialization();
            retiringExecutor.shutdownNow();
            LogisticsCenter.executor = testExecutor;
            assertTrue(retiringExecutor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void registryResetDoesNotSkipTheInitializingServicesInterceptors() throws Exception {
        Warehouse.interceptorsIndex.put(1, BlockingInitInterceptor.class);
        Warehouse.interceptorsIndex.put(2, PassingInterceptor.class);
        InterceptorServiceImpl service = new InterceptorServiceImpl();
        service.init(targetContext());
        assertTrue(BlockingInitInterceptor.awaitInitializationStart());

        Warehouse.clear();
        BlockingInitInterceptor.releaseInitialization();
        RecordingCallback callback = new RecordingCallback();
        Postcard postcard = new Postcard("/test/registry-reset", "test");
        service.doInterceptions(postcard, callback);

        assertTrue(callback.await());
        assertSame(postcard, callback.continued.get());
        assertNull(callback.interrupted.get());
        assertEquals(1, BlockingInitInterceptor.processCount.get());
        assertEquals(1, PassingInterceptor.processCount.get());
    }

    @Test
    public void emptyRegistryContinuesWithoutSchedulingWork() {
        testExecutor.shutdown();
        InterceptorServiceImpl service = new InterceptorServiceImpl();
        service.init(targetContext());
        RecordingCallback callback = new RecordingCallback();
        Postcard postcard = new Postcard("/test/no-interceptors", "test");

        service.doInterceptions(postcard, callback);

        assertSame(postcard, callback.continued.get());
        assertNull(callback.interrupted.get());
        assertEquals(0, testExecutor.getTaskCount());
    }

    @Test
    public void initializationFailureInterruptsNavigationWithTheOriginalCause() throws Exception {
        Warehouse.interceptorsIndex.put(1, FailingInitInterceptor.class);
        InterceptorServiceImpl service = new InterceptorServiceImpl();
        RecordingCallback callback = new RecordingCallback();

        long startedAt = System.nanoTime();
        service.init(targetContext());
        service.doInterceptions(new Postcard("/test/failure", "test"), callback);
        long elapsedMillis = elapsedMillis(startedAt);

        assertTrue(callback.await());
        assertNull(callback.continued.get());
        assertTrue(callback.interrupted.get() instanceof HandlerException);
        assertTrue(callback.interrupted.get().getCause() instanceof IllegalStateException);
        assertEquals("init failed", callback.interrupted.get().getCause().getMessage());
        assertTrue("failure should not wait for the timeout: " + elapsedMillis + " ms", elapsedMillis < 2000);
    }

    @Test
    public void slowInitializationStopsAtOneTotalDeadline() throws Exception {
        Warehouse.interceptorsIndex.put(1, BlockingInitInterceptor.class);
        InterceptorServiceImpl service = new InterceptorServiceImpl();
        RecordingCallback callback = new RecordingCallback();

        service.init(targetContext());
        assertTrue(BlockingInitInterceptor.awaitInitializationStart());

        long startedAt = System.nanoTime();
        service.doInterceptions(new Postcard("/test/timeout", "test"), callback);
        long elapsedMillis = elapsedMillis(startedAt);

        assertTrue(callback.await());
        assertNull(callback.continued.get());
        assertTrue(callback.interrupted.get() instanceof HandlerException);
        assertEquals("Interceptors initialization takes too much time.", callback.interrupted.get().getMessage());
        assertTrue("returned before the ten-second deadline: " + elapsedMillis + " ms", elapsedMillis >= 9000);
        assertTrue("wait exceeded one total deadline: " + elapsedMillis + " ms", elapsedMillis < 15000);
    }

    @Test
    public void concurrentNavigationContinuesAfterInitializationCompletes() throws Exception {
        final int navigationCount = 8;
        Warehouse.interceptorsIndex.put(1, BlockingInitInterceptor.class);
        final InterceptorServiceImpl service = new InterceptorServiceImpl();
        final CountDownLatch started = new CountDownLatch(navigationCount);
        final CountDownLatch continued = new CountDownLatch(navigationCount);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        Thread[] navigationThreads = new Thread[navigationCount];

        service.init(targetContext());
        assertTrue(BlockingInitInterceptor.awaitInitializationStart());

        for (int i = 0; i < navigationCount; i++) {
            final int index = i;
            navigationThreads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    started.countDown();
                    service.doInterceptions(new Postcard("/test/concurrent/" + index, "test"), new InterceptorCallback() {
                        @Override
                        public void onContinue(Postcard postcard) {
                            continued.countDown();
                        }

                        @Override
                        public void onInterrupt(Throwable exception) {
                            failure.compareAndSet(null, exception);
                            continued.countDown();
                        }
                    });
                }
            }, "navigation-" + i);
            navigationThreads[i].start();
        }

        assertTrue(started.await(2, TimeUnit.SECONDS));
        Thread.sleep(100);
        BlockingInitInterceptor.releaseInitialization();

        assertTrue(continued.await(5, TimeUnit.SECONDS));
        assertNull(failure.get());
        for (Thread navigationThread : navigationThreads) {
            navigationThread.join(1000);
            assertFalse(navigationThread.isAlive());
        }
    }

    @Test
    public void interruptedNavigationRestoresTheThreadInterruptFlag() throws Exception {
        Warehouse.interceptorsIndex.put(1, BlockingInitInterceptor.class);
        final InterceptorServiceImpl service = new InterceptorServiceImpl();
        final RecordingCallback callback = new RecordingCallback();
        final CountDownLatch navigationStarted = new CountDownLatch(1);
        final AtomicBoolean interruptedAfterReturn = new AtomicBoolean();

        service.init(targetContext());
        assertTrue(BlockingInitInterceptor.awaitInitializationStart());

        Thread navigationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                navigationStarted.countDown();
                service.doInterceptions(new Postcard("/test/interrupted", "test"), callback);
                interruptedAfterReturn.set(Thread.currentThread().isInterrupted());
            }
        }, "interrupted-navigation");
        navigationThread.start();

        assertTrue(navigationStarted.await(1, TimeUnit.SECONDS));
        Thread.sleep(100);
        navigationThread.interrupt();
        navigationThread.join(2000);

        assertFalse(navigationThread.isAlive());
        assertTrue(callback.await());
        assertTrue(callback.interrupted.get() instanceof HandlerException);
        assertTrue(callback.interrupted.get().getCause() instanceof InterruptedException);
        assertTrue(interruptedAfterReturn.get());
    }

    private static Context targetContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static final class RecordingCallback implements InterceptorCallback {
        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicReference<Postcard> continued = new AtomicReference<Postcard>();
        private final AtomicReference<Throwable> interrupted = new AtomicReference<Throwable>();

        @Override
        public void onContinue(Postcard postcard) {
            continued.set(postcard);
            completed.countDown();
        }

        @Override
        public void onInterrupt(Throwable exception) {
            interrupted.set(exception);
            completed.countDown();
        }

        boolean await() throws InterruptedException {
            return completed.await(5, TimeUnit.SECONDS);
        }
    }

    public static class PassingInterceptor implements IInterceptor {
        static final AtomicInteger processCount = new AtomicInteger();

        static void reset() {
            processCount.set(0);
        }

        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            processCount.incrementAndGet();
            callback.onContinue(postcard);
        }

        @Override
        public void init(Context context) {
        }
    }

    public static class SecondPassingInterceptor extends PassingInterceptor {
    }

    public static class FailingInitInterceptor implements IInterceptor {
        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            callback.onContinue(postcard);
        }

        @Override
        public void init(Context context) {
            throw new IllegalStateException("init failed");
        }
    }

    public static class BlockingInitInterceptor implements IInterceptor {
        static final AtomicInteger processCount = new AtomicInteger();
        private static CountDownLatch initializationStarted;
        private static CountDownLatch initializationRelease;

        static void reset() {
            processCount.set(0);
            initializationStarted = new CountDownLatch(1);
            initializationRelease = new CountDownLatch(1);
        }

        static boolean awaitInitializationStart() throws InterruptedException {
            return initializationStarted.await(2, TimeUnit.SECONDS);
        }

        static void releaseInitialization() {
            initializationRelease.countDown();
        }

        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            processCount.incrementAndGet();
            callback.onContinue(postcard);
        }

        @Override
        public void init(Context context) {
            initializationStarted.countDown();
            try {
                initializationRelease.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("init interrupted", ex);
            }
        }
    }
}

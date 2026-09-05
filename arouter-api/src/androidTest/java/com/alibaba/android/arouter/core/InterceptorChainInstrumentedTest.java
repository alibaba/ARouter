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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class InterceptorChainInstrumentedTest {
    private ScheduledThreadPoolExecutor timeoutExecutor;
    private ExecutorService completionExecutor;
    private ThreadPoolExecutor testExecutor;
    private ThreadPoolExecutor previousExecutor;
    private ILogger previousLogger;

    @Before
    public void setUp() {
        Warehouse.clear();
        timeoutExecutor = new ScheduledThreadPoolExecutor(1);
        completionExecutor = Executors.newFixedThreadPool(2);
        previousExecutor = LogisticsCenter.executor;
        previousLogger = ARouter.logger;
        ARouter.logger = new DefaultLogger();
    }

    @After
    public void tearDown() throws Exception {
        if (null != testExecutor) {
            testExecutor.shutdownNow();
            assertTrue(testExecutor.awaitTermination(5, TimeUnit.SECONDS));
        }
        timeoutExecutor.shutdownNow();
        assertTrue(timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS));
        completionExecutor.shutdownNow();
        assertTrue(completionExecutor.awaitTermination(5, TimeUnit.SECONDS));

        LogisticsCenter.executor = previousExecutor;
        ARouter.logger = previousLogger;
        Warehouse.clear();
    }

    @Test
    public void validAsyncInterceptorWaitsForItsCallback() throws Exception {
        Postcard postcard = new Postcard("/test/async", "test");
        DeferredInterceptor deferred = new DeferredInterceptor();
        CountingInterceptor downstream = new CountingInterceptor();
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, deferred, downstream);

        chain.scheduleTimeout(2, TimeUnit.SECONDS);
        chain.run();

        assertTrue(deferred.awaitInvocation());
        assertFalse(completion.await(100, TimeUnit.MILLISECONDS));
        assertEquals(0, downstream.processCount.get());

        deferred.continueWith(postcard);
        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(1, completion.continueCount.get());
        assertEquals(0, completion.interruptCount.get());
        assertEquals(1, downstream.processCount.get());
        assertTrue(waitForTimeoutQueueToEmpty());
    }

    @Test
    public void duplicateContinueOnlyAdvancesTheChainOnce() throws Exception {
        Postcard postcard = new Postcard("/test/duplicate", "test");
        CountingInterceptor downstream = new CountingInterceptor();
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, new DoubleContinueInterceptor(), downstream);

        chain.scheduleTimeout(2, TimeUnit.SECONDS);
        chain.run();

        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(1, downstream.processCount.get());
        assertEquals(1, completion.continueCount.get());
        assertEquals(0, completion.interruptCount.get());
    }

    @Test
    public void continueThenInterruptCompletesOnlyOnce() throws Exception {
        Postcard postcard = new Postcard("/test/conflicting", "test");
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, new ContinueThenInterruptInterceptor());

        chain.scheduleTimeout(2, TimeUnit.SECONDS);
        chain.run();

        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(1, completion.continueCount.get());
        assertEquals(0, completion.interruptCount.get());
        assertNull(completion.interruption.get());
    }

    @Test
    public void racingCallbacksCompleteTheChainOnce() throws Exception {
        Postcard postcard = new Postcard("/test/racing", "test");
        RacingInterceptor racing = new RacingInterceptor();
        CountingInterceptor downstream = new CountingInterceptor();
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, racing, downstream);

        chain.scheduleTimeout(2, TimeUnit.SECONDS);
        chain.run();

        assertTrue(racing.awaitCallbacks());
        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(1, completion.continueCount.get() + completion.interruptCount.get());
        assertTrue(downstream.processCount.get() <= 1);
    }

    @Test
    public void timeoutCancelsLateCallbackAndDownstreamWork() throws Exception {
        Postcard postcard = new Postcard("/test/late", "test");
        DeferredInterceptor deferred = new DeferredInterceptor();
        CountingInterceptor downstream = new CountingInterceptor();
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, deferred, downstream);

        chain.scheduleTimeout(100, TimeUnit.MILLISECONDS);
        chain.run();

        assertTrue(deferred.awaitInvocation());
        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(0, completion.continueCount.get());
        assertEquals(1, completion.interruptCount.get());
        assertTrue(completion.interruption.get() instanceof HandlerException);
        assertEquals("The interceptor processing timed out.", completion.interruption.get().getMessage());

        deferred.continueWith(postcard);
        Thread.sleep(100);
        assertEquals(0, downstream.processCount.get());
        assertEquals(0, completion.continueCount.get());
        assertEquals(1, completion.interruptCount.get());
    }

    @Test
    public void processExceptionInterruptsTheChainOnce() throws Exception {
        Postcard postcard = new Postcard("/test/exception", "test");
        IllegalStateException failure = new IllegalStateException("process failed");
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, new ThrowingInterceptor(failure));

        chain.scheduleTimeout(2, TimeUnit.SECONDS);
        chain.run();

        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(0, completion.continueCount.get());
        assertEquals(1, completion.interruptCount.get());
        assertSame(failure, completion.interruption.get());
    }

    @Test
    public void nullInterruptKeepsTheExistingErrorContract() throws Exception {
        Postcard postcard = new Postcard("/test/null-interrupt", "test");
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, new NullInterruptInterceptor());

        chain.scheduleTimeout(2, TimeUnit.SECONDS);
        chain.run();

        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(0, completion.continueCount.get());
        assertEquals(1, completion.interruptCount.get());
        assertTrue(completion.interruption.get() instanceof HandlerException);
        assertEquals("No message.", completion.interruption.get().getMessage());
    }

    @Test
    public void silentlyRejectedAsyncCompletionUsesTheFallbackOnce() throws Exception {
        CountDownLatch workerRelease = occupySilentlyRejectingExecutor();

        final Postcard postcard = new Postcard("/test/rejected-completion", "test");
        final DeferredInterceptor deferred = new DeferredInterceptor();
        final RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, deferred);
        chain.scheduleTimeout(2, TimeUnit.SECONDS);
        chain.run();
        assertTrue(deferred.awaitInvocation());

        Thread asyncCallback = new Thread(new Runnable() {
            @Override
            public void run() {
                deferred.continueWith(postcard);
            }
        }, "async-interceptor-callback");
        asyncCallback.start();
        asyncCallback.join(1000);

        assertFalse(asyncCallback.isAlive());
        assertTrue(completion.await(1, TimeUnit.SECONDS));
        assertEquals(1, completion.continueCount.get());
        assertEquals(0, completion.interruptCount.get());
        workerRelease.countDown();
    }

    @Test
    public void silentlyRejectedChainStartStillTimesOutOnce() throws Exception {
        CountDownLatch workerRelease = occupySilentlyRejectingExecutor();
        Postcard postcard = new Postcard("/test/rejected-start", "test");
        RecordingCallback completion = new RecordingCallback();
        InterceptorChain chain = newChain(postcard, completion, new CountingInterceptor());

        chain.scheduleTimeout(1, TimeUnit.SECONDS);
        testExecutor.execute(chain);

        assertTrue(completion.await(2, TimeUnit.SECONDS));
        assertEquals(0, completion.continueCount.get());
        assertEquals(1, completion.interruptCount.get());
        assertTrue(completion.interruption.get() instanceof HandlerException);
        assertEquals("The interceptor processing timed out.", completion.interruption.get().getMessage());
        workerRelease.countDown();
    }

    @Test
    public void blockingFallbackCallbackDoesNotBlockOtherTimeouts() throws Exception {
        CountDownLatch workerRelease = occupySilentlyRejectingExecutor();
        final CountDownLatch blockingCallbackStarted = new CountDownLatch(1);
        final CountDownLatch blockingCallbackRelease = new CountDownLatch(1);

        InterceptorCallback blockingCallback = new InterceptorCallback() {
            @Override
            public void onContinue(Postcard postcard) {
                blockCompletion();
            }

            @Override
            public void onInterrupt(Throwable exception) {
                blockCompletion();
            }

            private void blockCompletion() {
                blockingCallbackStarted.countDown();
                try {
                    blockingCallbackRelease.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        try {
            InterceptorChain blockingChain = new InterceptorChain(
                    Arrays.<IInterceptor>asList(new MissingCallbackInterceptor()),
                    new Postcard("/test/blocking-fallback", "test"),
                    blockingCallback,
                    timeoutExecutor,
                    completionExecutor
            );
            blockingChain.scheduleTimeout(50, TimeUnit.MILLISECONDS);
            blockingChain.run();
            assertTrue(blockingCallbackStarted.await(1, TimeUnit.SECONDS));

            RecordingCallback independentCompletion = new RecordingCallback();
            InterceptorChain independentChain = new InterceptorChain(
                    Arrays.<IInterceptor>asList(new MissingCallbackInterceptor()),
                    new Postcard("/test/independent-timeout", "test"),
                    independentCompletion,
                    timeoutExecutor,
                    completionExecutor
            );
            independentChain.scheduleTimeout(50, TimeUnit.MILLISECONDS);
            independentChain.run();

            assertTrue(independentCompletion.await(1, TimeUnit.SECONDS));
            assertEquals(0, independentCompletion.continueCount.get());
            assertEquals(1, independentCompletion.interruptCount.get());
        } finally {
            blockingCallbackRelease.countDown();
            workerRelease.countDown();
        }
    }

    @Test
    public void missingCallbacksDoNotExhaustTheSharedExecutor() throws Exception {
        final int navigationCount = 20;
        testExecutor = new ThreadPoolExecutor(
                2,
                2,
                1,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(64)
        );
        LogisticsCenter.executor = testExecutor;
        Warehouse.interceptorsIndex.put(1, MissingCallbackInterceptor.class);

        InterceptorServiceImpl service = new InterceptorServiceImpl();
        service.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        final CountDownLatch completed = new CountDownLatch(navigationCount);
        final AtomicInteger continued = new AtomicInteger();
        final AtomicInteger interrupted = new AtomicInteger();

        for (int index = 0; index < navigationCount; index++) {
            Postcard postcard = new Postcard("/test/missing/" + index, "test").setTimeout(1);
            service.doInterceptions(postcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard postcard) {
                    continued.incrementAndGet();
                    completed.countDown();
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    interrupted.incrementAndGet();
                    completed.countDown();
                }
            });
        }

        assertTrue("shared workers remained blocked", waitForSharedExecutorToBecomeIdle());
        assertTrue("timeouts were serialized by the shared pool", completed.await(3, TimeUnit.SECONDS));
        assertEquals(0, continued.get());
        assertEquals(navigationCount, interrupted.get());

        Warehouse.clear();
        Warehouse.interceptorsIndex.put(1, CountingInterceptor.class);
        InterceptorServiceImpl subsequentService = new InterceptorServiceImpl();
        subsequentService.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        RecordingCallback subsequentNavigation = new RecordingCallback();
        subsequentService.doInterceptions(new Postcard("/test/subsequent", "test").setTimeout(2), subsequentNavigation);

        assertTrue(subsequentNavigation.await(1, TimeUnit.SECONDS));
        assertEquals(1, subsequentNavigation.continueCount.get());
        assertEquals(0, subsequentNavigation.interruptCount.get());
    }

    private InterceptorChain newChain(Postcard postcard,
                                      RecordingCallback completion,
                                      IInterceptor... interceptors) {
        return new InterceptorChain(
                Arrays.asList(interceptors),
                postcard,
                completion,
                timeoutExecutor,
                completionExecutor
        );
    }

    private boolean waitForTimeoutQueueToEmpty() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadline) {
            if (timeoutExecutor.getQueue().isEmpty()) {
                return true;
            }
            Thread.sleep(10);
        }
        return timeoutExecutor.getQueue().isEmpty();
    }

    private boolean waitForSharedExecutorToBecomeIdle() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(750);
        while (System.nanoTime() < deadline) {
            if (0 == testExecutor.getActiveCount() && testExecutor.getQueue().isEmpty()) {
                return true;
            }
            Thread.sleep(10);
        }
        return 0 == testExecutor.getActiveCount() && testExecutor.getQueue().isEmpty();
    }

    private CountDownLatch occupySilentlyRejectingExecutor() throws InterruptedException {
        final CountDownLatch workerStarted = new CountDownLatch(1);
        final CountDownLatch workerRelease = new CountDownLatch(1);
        testExecutor = new ThreadPoolExecutor(
                1,
                1,
                1,
                TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
                        // Match ARouter's legacy rejection behavior: log/drop without throwing.
                    }
                }
        );
        LogisticsCenter.executor = testExecutor;
        testExecutor.execute(new Runnable() {
            @Override
            public void run() {
                workerStarted.countDown();
                try {
                    workerRelease.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        assertTrue(workerStarted.await(1, TimeUnit.SECONDS));
        return workerRelease;
    }

    private static final class RecordingCallback implements InterceptorCallback {
        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicInteger continueCount = new AtomicInteger();
        private final AtomicInteger interruptCount = new AtomicInteger();
        private final AtomicReference<Throwable> interruption = new AtomicReference<Throwable>();

        @Override
        public void onContinue(Postcard postcard) {
            continueCount.incrementAndGet();
            completed.countDown();
        }

        @Override
        public void onInterrupt(Throwable exception) {
            interruption.compareAndSet(null, exception);
            interruptCount.incrementAndGet();
            completed.countDown();
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return completed.await(timeout, unit);
        }
    }

    private static final class DeferredInterceptor implements IInterceptor {
        private final CountDownLatch invoked = new CountDownLatch(1);
        private final AtomicReference<InterceptorCallback> callback = new AtomicReference<InterceptorCallback>();

        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            this.callback.set(callback);
            invoked.countDown();
        }

        @Override
        public void init(Context context) {
        }

        boolean awaitInvocation() throws InterruptedException {
            return invoked.await(1, TimeUnit.SECONDS);
        }

        void continueWith(Postcard postcard) {
            callback.get().onContinue(postcard);
        }
    }

    public static final class MissingCallbackInterceptor implements IInterceptor {
        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
        }

        @Override
        public void init(Context context) {
        }
    }

    public static final class CountingInterceptor implements IInterceptor {
        private final AtomicInteger processCount = new AtomicInteger();

        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            processCount.incrementAndGet();
            callback.onContinue(postcard);
        }

        @Override
        public void init(Context context) {
        }
    }

    private static final class DoubleContinueInterceptor implements IInterceptor {
        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            callback.onContinue(postcard);
            callback.onContinue(postcard);
        }

        @Override
        public void init(Context context) {
        }
    }

    private static final class ContinueThenInterruptInterceptor implements IInterceptor {
        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            callback.onContinue(postcard);
            callback.onInterrupt(new IllegalStateException("late interrupt"));
        }

        @Override
        public void init(Context context) {
        }
    }

    private static final class RacingInterceptor implements IInterceptor {
        private final CountDownLatch callbacksFinished = new CountDownLatch(2);

        @Override
        public void process(final Postcard postcard, final InterceptorCallback callback) {
            final CountDownLatch start = new CountDownLatch(1);
            Thread continueThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    await(start);
                    callback.onContinue(postcard);
                    callbacksFinished.countDown();
                }
            }, "racing-continue");
            Thread interruptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    await(start);
                    callback.onInterrupt(new IllegalStateException("racing interrupt"));
                    callbacksFinished.countDown();
                }
            }, "racing-interrupt");
            continueThread.start();
            interruptThread.start();
            start.countDown();
        }

        @Override
        public void init(Context context) {
        }

        boolean awaitCallbacks() throws InterruptedException {
            return callbacksFinished.await(1, TimeUnit.SECONDS);
        }

        private static void await(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class ThrowingInterceptor implements IInterceptor {
        private final RuntimeException failure;

        ThrowingInterceptor(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            throw failure;
        }

        @Override
        public void init(Context context) {
        }
    }

    private static final class NullInterruptInterceptor implements IInterceptor {
        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            callback.onInterrupt(null);
        }

        @Override
        public void init(Context context) {
        }
    }
}

package com.alibaba.android.arouter.core;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class InterceptorInitStateTest {

    @Test
    public void successWakesWaitingNavigation() throws Exception {
        final InterceptorInitState state = new InterceptorInitState();
        final CountDownLatch waiting = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        state.start();

        try {
            Future<InterceptorInitState.Result> future = executor.submit(new Callable<InterceptorInitState.Result>() {
                @Override
                public InterceptorInitState.Result call() throws Exception {
                    waiting.countDown();
                    return state.await(5, TimeUnit.SECONDS);
                }
            });

            assertTrue(waiting.await(1, TimeUnit.SECONDS));
            state.succeed();

            InterceptorInitState.Result result = future.get(1, TimeUnit.SECONDS);
            assertSame(InterceptorInitState.Outcome.SUCCESS, result.getOutcome());
            assertNull(result.getFailure());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void failureWakesWaitingNavigationAndPreservesCause() throws Exception {
        final InterceptorInitState state = new InterceptorInitState();
        final IllegalStateException failure = new IllegalStateException("boom");
        final CountDownLatch waiting = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        state.start();

        try {
            Future<InterceptorInitState.Result> future = executor.submit(new Callable<InterceptorInitState.Result>() {
                @Override
                public InterceptorInitState.Result call() throws Exception {
                    waiting.countDown();
                    return state.await(5, TimeUnit.SECONDS);
                }
            });

            assertTrue(waiting.await(1, TimeUnit.SECONDS));
            state.fail(failure);

            InterceptorInitState.Result result = future.get(1, TimeUnit.SECONDS);
            assertSame(InterceptorInitState.Outcome.FAILURE, result.getOutcome());
            assertSame(failure, result.getFailure());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void timeoutUsesOneTotalDeadline() throws Exception {
        InterceptorInitState state = new InterceptorInitState();
        state.start();

        long startedAt = System.nanoTime();
        InterceptorInitState.Result result = state.await(40, TimeUnit.MILLISECONDS);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertSame(InterceptorInitState.Outcome.TIMEOUT, result.getOutcome());
        assertTrue("returned before the requested deadline: " + elapsedMillis + " ms", elapsedMillis >= 20);
        assertTrue("deadline was extended unexpectedly: " + elapsedMillis + " ms", elapsedMillis < 2000);
    }

    @Test
    public void nonTerminalWakeupDoesNotBypassInitialization() throws Exception {
        final InterceptorInitState state = new InterceptorInitState();
        final CountDownLatch waiting = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        state.start();

        try {
            Future<InterceptorInitState.Result> future = executor.submit(new Callable<InterceptorInitState.Result>() {
                @Override
                public InterceptorInitState.Result call() throws Exception {
                    waiting.countDown();
                    return state.await(5, TimeUnit.SECONDS);
                }
            });

            assertTrue(waiting.await(1, TimeUnit.SECONDS));
            state.start();
            Thread.sleep(50);
            assertFalse("a non-terminal signal bypassed initialization", future.isDone());

            state.succeed();
            assertSame(InterceptorInitState.Outcome.SUCCESS, future.get(1, TimeUnit.SECONDS).getOutcome());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void allConcurrentWaitersObserveSuccess() throws Exception {
        final int waiterCount = 8;
        final InterceptorInitState state = new InterceptorInitState();
        final CountDownLatch waiting = new CountDownLatch(waiterCount);
        ExecutorService executor = Executors.newFixedThreadPool(waiterCount);
        List<Future<InterceptorInitState.Result>> futures = new ArrayList<Future<InterceptorInitState.Result>>();
        state.start();

        try {
            for (int i = 0; i < waiterCount; i++) {
                futures.add(executor.submit(new Callable<InterceptorInitState.Result>() {
                    @Override
                    public InterceptorInitState.Result call() throws Exception {
                        waiting.countDown();
                        return state.await(5, TimeUnit.SECONDS);
                    }
                }));
            }

            assertTrue(waiting.await(1, TimeUnit.SECONDS));
            state.succeed();

            for (Future<InterceptorInitState.Result> future : futures) {
                assertSame(InterceptorInitState.Outcome.SUCCESS, future.get(1, TimeUnit.SECONDS).getOutcome());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void interruptionStopsWaiting() throws Exception {
        final InterceptorInitState state = new InterceptorInitState();
        final CountDownLatch waiting = new CountDownLatch(1);
        final AtomicReference<Throwable> thrown = new AtomicReference<Throwable>();
        state.start();

        Thread waiter = new Thread(new Runnable() {
            @Override
            public void run() {
                waiting.countDown();
                try {
                    state.await(5, TimeUnit.SECONDS);
                } catch (Throwable ex) {
                    thrown.set(ex);
                }
            }
        }, "interceptor-init-waiter");

        waiter.start();
        assertTrue(waiting.await(1, TimeUnit.SECONDS));
        waiter.interrupt();
        waiter.join(1000);

        assertFalse("waiter did not stop after interruption", waiter.isAlive());
        assertTrue(thrown.get() instanceof InterruptedException);
    }
}

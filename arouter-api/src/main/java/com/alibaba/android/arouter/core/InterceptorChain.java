package com.alibaba.android.arouter.core;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.IInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.alibaba.android.arouter.launcher.ARouter.logger;
import static com.alibaba.android.arouter.utils.Consts.TAG;

/**
 * Executes an interceptor chain without occupying a worker while an
 * interceptor is waiting to complete asynchronously.
 */
final class InterceptorChain implements Runnable {
    private static final long COMPLETION_FALLBACK_DELAY_MILLIS = 100;
    private static final AtomicInteger COMPLETION_THREAD_ID = new AtomicInteger();
    private static final ScheduledThreadPoolExecutor DEFAULT_TIMEOUT_EXECUTOR = createTimeoutExecutor();
    private static final ExecutorService DEFAULT_COMPLETION_EXECUTOR = createCompletionExecutor();

    private final List<IInterceptor> interceptors;
    private final Postcard postcard;
    private final InterceptorCallback completionCallback;
    private final ScheduledThreadPoolExecutor timeoutExecutor;
    private final ExecutorService completionExecutor;
    private final AtomicBoolean timeoutScheduled = new AtomicBoolean();
    private final AtomicBoolean executionStarted = new AtomicBoolean();
    private final AtomicBoolean completed = new AtomicBoolean();

    private volatile ScheduledFuture<?> timeoutFuture;
    private volatile String pendingInterceptor = "not started";
    private volatile Thread executionThread;

    InterceptorChain(List<IInterceptor> interceptors, Postcard postcard, InterceptorCallback completionCallback) {
        this(interceptors, postcard, completionCallback, DEFAULT_TIMEOUT_EXECUTOR, DEFAULT_COMPLETION_EXECUTOR);
    }

    InterceptorChain(List<IInterceptor> interceptors,
                     Postcard postcard,
                     InterceptorCallback completionCallback,
                     ScheduledThreadPoolExecutor timeoutExecutor) {
        this(interceptors, postcard, completionCallback, timeoutExecutor, DEFAULT_COMPLETION_EXECUTOR);
    }

    InterceptorChain(List<IInterceptor> interceptors,
                     Postcard postcard,
                     InterceptorCallback completionCallback,
                     ScheduledThreadPoolExecutor timeoutExecutor,
                     ExecutorService completionExecutor) {
        this.interceptors = new ArrayList<IInterceptor>(interceptors);
        this.postcard = postcard;
        this.completionCallback = completionCallback;
        this.timeoutExecutor = timeoutExecutor;
        this.completionExecutor = completionExecutor;
    }

    void scheduleTimeout(long timeout, TimeUnit unit) {
        if (!timeoutScheduled.compareAndSet(false, true)) {
            throw new IllegalStateException("Interceptor timeout has already been scheduled.");
        }

        ScheduledFuture<?> future = timeoutExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                onTimeout();
            }
        }, timeout, unit);
        timeoutFuture = future;

        // A zero-length timeout may finish before schedule() returns.
        if (completed.get()) {
            cancelTimeout(future);
        }
    }

    @Override
    public void run() {
        if (!executionStarted.compareAndSet(false, true)) {
            logWarning("Interceptor chain execution was requested more than once. path = [" + postcard.getPath() + "]");
            return;
        }

        executionThread = Thread.currentThread();
        execute(0, postcard);
    }

    void interrupt(Throwable exception) {
        completeWithInterrupt(null == exception ? new HandlerException("No message.") : exception);
    }

    private void execute(final int index, final Postcard currentPostcard) {
        if (completed.get()) {
            logLateCallback(index);
            return;
        }

        if (index >= interceptors.size()) {
            completeWithContinue();
            return;
        }

        final IInterceptor interceptor = interceptors.get(index);
        pendingInterceptor = interceptor.getClass().getName();
        final AtomicBoolean callbackCompleted = new AtomicBoolean();

        try {
            interceptor.process(currentPostcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard nextPostcard) {
                    if (callbackCompleted.compareAndSet(false, true)) {
                        if (completed.get()) {
                            logLateCallback(index);
                            return;
                        }
                        execute(index + 1, nextPostcard);
                    } else {
                        logDuplicateCallback(interceptor, "onContinue");
                    }
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    if (callbackCompleted.compareAndSet(false, true)) {
                        if (completed.get()) {
                            logLateCallback(index);
                            return;
                        }
                        completeWithInterrupt(null == exception ? new HandlerException("No message.") : exception);
                    } else {
                        logDuplicateCallback(interceptor, "onInterrupt");
                    }
                }
            });
        } catch (Exception exception) {
            if (callbackCompleted.compareAndSet(false, true)) {
                completeWithInterrupt(exception);
            } else {
                logDuplicateCallback(interceptor, "exception after callback");
            }
        }
    }

    private void onTimeout() {
        if (completed.compareAndSet(false, true)) {
            logWarning("Interceptor processing timed out. path = [" + postcard.getPath()
                    + "], interceptor = [" + pendingInterceptor + "]");
            cancelTimeout(timeoutFuture);
            dispatchCompletion(new Runnable() {
                @Override
                public void run() {
                    completionCallback.onInterrupt(new HandlerException("The interceptor processing timed out."));
                }
            });
        }
    }

    private void completeWithContinue() {
        if (completed.compareAndSet(false, true)) {
            cancelTimeout(timeoutFuture);
            dispatchCompletion(new Runnable() {
                @Override
                public void run() {
                    completionCallback.onContinue(postcard);
                }
            });
        }
    }

    private void completeWithInterrupt(final Throwable exception) {
        if (completed.compareAndSet(false, true)) {
            cancelTimeout(timeoutFuture);
            dispatchCompletion(new Runnable() {
                @Override
                public void run() {
                    completionCallback.onInterrupt(exception);
                }
            });
        }
    }

    private void dispatchCompletion(final Runnable completion) {
        if (Thread.currentThread() == executionThread) {
            completion.run();
            return;
        }

        final AtomicBoolean delivered = new AtomicBoolean();
        final AtomicReference<ScheduledFuture<?>> fallbackFuture = new AtomicReference<ScheduledFuture<?>>();
        final Runnable deliverOnce = new Runnable() {
            @Override
            public void run() {
                if (delivered.compareAndSet(false, true)) {
                    try {
                        completion.run();
                    } finally {
                        cancelTimeout(fallbackFuture.get());
                    }
                }
            }
        };

        boolean submitted = false;
        if (null != LogisticsCenter.executor) {
            try {
                LogisticsCenter.executor.execute(deliverOnce);
                submitted = true;
            } catch (RuntimeException ignored) {
                // The timeout executor fallback below still guarantees delivery.
            }
        }

        try {
            ScheduledFuture<?> fallback = timeoutExecutor.schedule(
                    new Runnable() {
                        @Override
                        public void run() {
                            dispatchCompletionFallback(deliverOnce);
                        }
                    },
                    submitted ? COMPLETION_FALLBACK_DELAY_MILLIS : 0,
                    TimeUnit.MILLISECONDS
            );
            fallbackFuture.set(fallback);
            if (delivered.get()) {
                cancelTimeout(fallback);
            }
        } catch (RuntimeException ignored) {
            dispatchCompletionFallback(deliverOnce);
        }
    }

    private void dispatchCompletionFallback(Runnable completion) {
        try {
            completionExecutor.execute(completion);
        } catch (RuntimeException rejected) {
            // Never run application callbacks on the timeout scheduler. A one-off daemon thread
            // is the last-resort path if the dedicated completion executor rejects the task.
            Thread emergency = newCompletionThread(completion);
            emergency.start();
        }
    }

    private void cancelTimeout(ScheduledFuture<?> future) {
        if (null != future) {
            try {
                future.cancel(false);
                timeoutExecutor.purge();
            } catch (RuntimeException exception) {
                logWarning("Failed to remove interceptor timeout task. path = [" + postcard.getPath() + "]");
            }
        }
    }

    private void logDuplicateCallback(IInterceptor interceptor, String callbackName) {
        logWarning("Interceptor callback ignored because it completed more than once. path = ["
                + postcard.getPath() + "], interceptor = [" + interceptor.getClass().getName()
                + "], callback = [" + callbackName + "]");
    }

    private void logLateCallback(int index) {
        String interceptorName = index < interceptors.size()
                ? interceptors.get(index).getClass().getName()
                : pendingInterceptor;
        logWarning("Interceptor callback ignored after chain completion. path = ["
                + postcard.getPath() + "], interceptor = [" + interceptorName + "]");
    }

    private static void logWarning(String message) {
        if (null != logger) {
            try {
                logger.warning(TAG, message);
            } catch (RuntimeException ignored) {
                // Logging must never change navigation completion semantics.
            }
        }
    }

    private static ScheduledThreadPoolExecutor createTimeoutExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ARouter interceptor timeout");
                thread.setDaemon(true);
                return thread;
            }
        });
        return executor;
    }

    private static ExecutorService createCompletionExecutor() {
        return Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                return newCompletionThread(runnable);
            }
        });
    }

    private static Thread newCompletionThread(Runnable runnable) {
        Thread thread = new Thread(
                runnable,
                "ARouter interceptor completion " + COMPLETION_THREAD_ID.incrementAndGet()
        );
        thread.setDaemon(true);
        return thread;
    }
}

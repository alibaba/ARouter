package com.alibaba.android.arouter.core;

import java.util.concurrent.TimeUnit;

/**
 * Coordinates interceptor initialization and navigation without allowing a
 * spurious wake-up to bypass initialization or extend the total wait time.
 */
final class InterceptorInitState {
    enum Outcome {
        SUCCESS,
        FAILURE,
        TIMEOUT
    }

    static final class Result {
        private final Outcome outcome;
        private final Throwable failure;

        Result(Outcome outcome, Throwable failure) {
            this.outcome = outcome;
            this.failure = failure;
        }

        Outcome getOutcome() {
            return outcome;
        }

        Throwable getFailure() {
            return failure;
        }
    }

    private enum Status {
        IDLE,
        INITIALIZING,
        SUCCEEDED,
        FAILED
    }

    private final Object lock = new Object();
    private Status status = Status.IDLE;
    private Throwable failure;

    void start() {
        synchronized (lock) {
            status = Status.INITIALIZING;
            failure = null;
            lock.notifyAll();
        }
    }

    void succeed() {
        synchronized (lock) {
            status = Status.SUCCEEDED;
            failure = null;
            lock.notifyAll();
        }
    }

    void fail(Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException("Interceptor initialization failure must have a cause.");
        }

        synchronized (lock) {
            status = Status.FAILED;
            failure = cause;
            lock.notifyAll();
        }
    }

    Result await(long timeout, TimeUnit unit) throws InterruptedException {
        if (unit == null) {
            throw new NullPointerException("TimeUnit must not be null.");
        }

        long timeoutNanos = unit.toNanos(timeout);
        long startNanos = System.nanoTime();

        synchronized (lock) {
            while (status != Status.SUCCEEDED && status != Status.FAILED) {
                long elapsedNanos = System.nanoTime() - startNanos;
                long remainingNanos = timeoutNanos - elapsedNanos;
                if (remainingNanos <= 0) {
                    return new Result(Outcome.TIMEOUT, null);
                }

                TimeUnit.NANOSECONDS.timedWait(lock, remainingNanos);
            }

            if (status == Status.FAILED) {
                return new Result(Outcome.FAILURE, failure);
            }
            return new Result(Outcome.SUCCESS, null);
        }
    }
}

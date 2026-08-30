package com.alibaba.android.arouter.core;

import android.content.Context;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.IInterceptor;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RoutingFailureDiagnosticsTest {

    @Test
    public void interceptorFailurePreservesCauseWithEmptyMessage() {
        NullPointerException cause = new NullPointerException();

        HandlerException failure = InterceptorServiceImpl.interceptorInitFailure(
                ProbeInterceptor.class,
                cause
        );

        assertSame(cause, failure.getCause());
        assertTrue(failure.getMessage().contains(ProbeInterceptor.class.getName()));
        assertTrue(failure.getMessage().contains(NullPointerException.class.getName()));
        assertFalse(failure.getMessage().contains("reason = [null]"));
    }

    @Test
    public void failureDescriptionIncludesTypeAndMessage() {
        IllegalStateException cause = new IllegalStateException("diagnostic detail");

        assertTrue(LogisticsCenter.describeFailure(cause)
                .contains("java.lang.IllegalStateException: diagnostic detail"));
    }

    public static final class ProbeInterceptor implements IInterceptor {
        @Override
        public void process(Postcard postcard, InterceptorCallback callback) {
            callback.onContinue(postcard);
        }

        @Override
        public void init(Context context) {
        }
    }
}

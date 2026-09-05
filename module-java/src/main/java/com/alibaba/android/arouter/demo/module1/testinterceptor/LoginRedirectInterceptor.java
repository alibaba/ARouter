package com.alibaba.android.arouter.demo.module1.testinterceptor;

import android.content.Context;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Interceptor;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Interceptor(priority = 5, name = "login redirect contract probe")
public class LoginRedirectInterceptor implements IInterceptor {
    private static final String PROTECTED_PATH = "/redirect/protected";
    private static final String LOGIN_PATH = "/redirect/login";
    private static final AtomicInteger PROTECTED_PROCESS_COUNT = new AtomicInteger();
    private static final AtomicInteger WATCHED_POSTCARD_PROCESS_COUNT = new AtomicInteger();
    private static final AtomicReference<Postcard> WATCHED_POSTCARD = new AtomicReference<Postcard>();

    public static void resetProbe() {
        WATCHED_POSTCARD.set(null);
        PROTECTED_PROCESS_COUNT.set(0);
        WATCHED_POSTCARD_PROCESS_COUNT.set(0);
    }

    public static int protectedProcessCount() {
        return PROTECTED_PROCESS_COUNT.get();
    }

    public static void watchPostcard(Postcard postcard) {
        WATCHED_POSTCARD.set(postcard);
        WATCHED_POSTCARD_PROCESS_COUNT.set(0);
    }

    public static int watchedPostcardProcessCount() {
        return WATCHED_POSTCARD_PROCESS_COUNT.get();
    }

    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
        if (postcard == WATCHED_POSTCARD.get()) {
            WATCHED_POSTCARD_PROCESS_COUNT.incrementAndGet();
        }
        if (PROTECTED_PATH.equals(postcard.getPath())) {
            PROTECTED_PROCESS_COUNT.incrementAndGet();
            callback.onInterrupt(new IllegalStateException("Login is required."));
            ARouter.getInstance()
                    .build(LOGIN_PATH)
                    .greenChannel()
                    .navigation(postcard.getContext());
        } else {
            callback.onContinue(postcard);
        }
    }

    @Override
    public void init(Context context) {
    }
}

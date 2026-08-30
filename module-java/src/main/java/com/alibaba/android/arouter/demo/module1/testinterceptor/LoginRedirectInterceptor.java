package com.alibaba.android.arouter.demo.module1.testinterceptor;

import android.content.Context;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Interceptor;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.alibaba.android.arouter.launcher.ARouter;

import java.util.concurrent.atomic.AtomicInteger;

@Interceptor(priority = 5, name = "login redirect contract probe")
public class LoginRedirectInterceptor implements IInterceptor {
    private static final String PROTECTED_PATH = "/redirect/protected";
    private static final String LOGIN_PATH = "/redirect/login";
    private static final AtomicInteger PROCESS_COUNT = new AtomicInteger();

    public static void resetProbe() {
        PROCESS_COUNT.set(0);
    }

    public static int processCount() {
        return PROCESS_COUNT.get();
    }

    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
        PROCESS_COUNT.incrementAndGet();
        if (PROTECTED_PATH.equals(postcard.getPath())) {
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

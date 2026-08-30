package com.alibaba.android.arouter.core;

import android.content.Context;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.service.InterceptorService;
import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.alibaba.android.arouter.utils.MapUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.alibaba.android.arouter.launcher.ARouter.logger;
import static com.alibaba.android.arouter.utils.Consts.TAG;

/**
 * All of interceptors
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/23 下午2:09
 */
@Route(path = "/arouter/service/interceptor")
public class InterceptorServiceImpl implements InterceptorService {
    private static final long INTERCEPTOR_INIT_TIMEOUT_SECONDS = 10;
    private final InterceptorInitState interceptorInitState = new InterceptorInitState();

    @Override
    public void doInterceptions(final Postcard postcard, final InterceptorCallback callback) {
        if (MapUtils.isNotEmpty(Warehouse.interceptorsIndex)) {
            InterceptorInitState.Result initResult;
            try {
                initResult = interceptorInitState.await(INTERCEPTOR_INIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                HandlerException interruption = new HandlerException("Interceptor initialization was interrupted.");
                interruption.initCause(ex);
                callback.onInterrupt(interruption);
                return;
            }

            if (initResult.getOutcome() == InterceptorInitState.Outcome.TIMEOUT) {
                callback.onInterrupt(new HandlerException("Interceptors initialization takes too much time."));
                return;
            } else if (initResult.getOutcome() == InterceptorInitState.Outcome.FAILURE) {
                callback.onInterrupt(initResult.getFailure());
                return;
            }

            InterceptorChain interceptorChain = new InterceptorChain(Warehouse.interceptors, postcard, callback);
            try {
                interceptorChain.scheduleTimeout(postcard.getTimeout(), TimeUnit.SECONDS);
                LogisticsCenter.executor.execute(interceptorChain);
            } catch (RuntimeException exception) {
                interceptorChain.interrupt(exception);
            }
        } else {
            callback.onContinue(postcard);
        }
    }

    @Override
    public void init(final Context context) {
        interceptorInitState.start();
        try {
            LogisticsCenter.executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (MapUtils.isNotEmpty(Warehouse.interceptorsIndex)) {
                        for (Map.Entry<Integer, Class<? extends IInterceptor>> entry : Warehouse.interceptorsIndex.entrySet()) {
                            Class<? extends IInterceptor> interceptorClass = entry.getValue();
                            try {
                                IInterceptor iInterceptor = interceptorClass.getConstructor().newInstance();
                                iInterceptor.init(context);
                                Warehouse.interceptors.add(iInterceptor);
                            } catch (Exception ex) {
                                HandlerException failure = interceptorInitFailure(interceptorClass, ex);
                                interceptorInitState.fail(failure);
                                logger.error(TAG, failure.getMessage(), ex);
                                return;
                            }
                        }

                        interceptorInitState.succeed();
                        logger.info(TAG, "ARouter interceptors init over.");
                    } else {
                        interceptorInitState.succeed();
                    }
                }
            });
        } catch (RuntimeException ex) {
            HandlerException failure = new HandlerException(TAG + "ARouter interceptor init task rejected! reason = [" + ex.getMessage() + "]");
            failure.initCause(ex);
            interceptorInitState.fail(failure);
            throw failure;
        }
    }

    private static HandlerException interceptorInitFailure(Class<? extends IInterceptor> interceptorClass, Exception cause) {
        HandlerException failure = new HandlerException(TAG + "ARouter init interceptor error! name = [" + interceptorClass.getName() + "], reason = [" + cause.getMessage() + "]");
        failure.initCause(cause);
        return failure;
    }
}

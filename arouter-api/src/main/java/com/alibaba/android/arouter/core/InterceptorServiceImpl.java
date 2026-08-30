package com.alibaba.android.arouter.core;

import android.content.Context;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.service.InterceptorService;
import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.alibaba.android.arouter.thread.CancelableCountDownLatch;
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

            LogisticsCenter.executor.execute(new Runnable() {
                @Override
                public void run() {
                    CancelableCountDownLatch interceptorCounter = new CancelableCountDownLatch(Warehouse.interceptors.size());
                    try {
                        _execute(0, interceptorCounter, postcard);
                        interceptorCounter.await(postcard.getTimeout(), TimeUnit.SECONDS);
                        if (interceptorCounter.getCount() > 0) {    // Cancel the navigation this time, if it hasn't return anythings.
                            callback.onInterrupt(new HandlerException("The interceptor processing timed out."));
                        } else if (null != postcard.getTag()) {    // Maybe some exception in the tag.
                            callback.onInterrupt((Throwable) postcard.getTag());
                        } else {
                            callback.onContinue(postcard);
                        }
                    } catch (Exception e) {
                        callback.onInterrupt(e);
                    }
                }
            });
        } else {
            callback.onContinue(postcard);
        }
    }

    /**
     * Excute interceptor
     *
     * @param index    current interceptor index
     * @param counter  interceptor counter
     * @param postcard routeMeta
     */
    private static void _execute(final int index, final CancelableCountDownLatch counter, final Postcard postcard) {
        if (index < Warehouse.interceptors.size()) {
            IInterceptor iInterceptor = Warehouse.interceptors.get(index);
            iInterceptor.process(postcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard postcard) {
                    // Last interceptor excute over with no exception.
                    counter.countDown();
                    _execute(index + 1, counter, postcard);  // When counter is down, it will be execute continue ,but index bigger than interceptors size, then U know.
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    // Last interceptor execute over with fatal exception.

                    postcard.setTag(null == exception ? new HandlerException("No message.") : exception);    // save the exception message for backup.
                    counter.cancel();
                    // Be attention, maybe the thread in callback has been changed,
                    // then the catch block(L207) will be invalid.
                    // The worst is the thread changed to main thread, then the app will be crash, if you throw this exception!
//                    if (!Looper.getMainLooper().equals(Looper.myLooper())) {    // You shouldn't throw the exception if the thread is main thread.
//                        throw new HandlerException(exception.getMessage());
//                    }
                }
            });
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

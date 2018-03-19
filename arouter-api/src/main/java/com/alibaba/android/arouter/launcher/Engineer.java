package com.alibaba.android.arouter.launcher;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.alibaba.android.arouter.core.LogisticsCenter;
import com.alibaba.android.arouter.exception.NoRouteFoundException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.callback.NavigationCallback;
import com.alibaba.android.arouter.facade.service.DegradeService;
import com.alibaba.android.arouter.utils.Consts;
import com.alibaba.android.arouter.utils.TextUtils;

import java.lang.ref.WeakReference;

import static com.alibaba.android.arouter.launcher.ARouter.debuggable;
import static com.alibaba.android.arouter.launcher._ARouter.interceptorService;
import static com.alibaba.android.arouter.launcher._ARouter.logger;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public abstract class Engineer<T> {
    private static final String TAG = Engineer.class.toString();
    private WeakReference<T> mReference;

    public Engineer(T t) {
        mReference = new WeakReference<T>(t);
    }

    @Nullable
    protected T getT() {
        return mReference != null ? mReference.get() : null;
    }

    public Object navigation(final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        try {
            LogisticsCenter.completion(postcard);
        } catch (NoRouteFoundException ex) {
            logger.warning(Consts.TAG, ex.getMessage());

            if (debuggable()) { // Show friendly tips for user.
                logger.error(TAG, "There's no route matched!\n" +
                        " Path = [" + postcard.getPath() + "]\n" +
                        " Group = [" + postcard.getGroup() + "]");
            }

            if (null != callback) {
                callback.onLost(postcard);
            } else {    // No callback for this invoke, then we use the global degrade service.
                DegradeService degradeService = ARouter.getInstance().navigation(DegradeService.class);
                if (null != degradeService) {
                    degradeService.onLost(getContext(), postcard);
                }
            }

            return null;
        }

        if (null != callback) {
            callback.onFound(postcard);
        }
        if (!postcard.isGreenChannel()) {   // It must be run in async thread, maybe interceptor cost too mush time made ANR.
            interceptorService.doInterceptions(postcard, new InterceptorCallback() {
                /**
                 * Continue process
                 *
                 * @param postcard route meta
                 */
                @Override
                public void onContinue(Postcard postcard) {
                    _navigation(getT(), postcard, requestCode, callback);
                }

                /**
                 * Interrupt process, pipeline will be destory when this method called.
                 *
                 * @param exception Reson of interrupt.
                 */
                @Override
                public void onInterrupt(Throwable exception) {
                    if (null != callback) {
                        callback.onInterrupt(postcard);
                    }

                    logger.info(Consts.TAG, "Navigation failed, termination by interceptor : " + exception.getMessage());
                }
            });
        } else {
            return _navigation(getT(), postcard, requestCode, callback);
        }
        return null;
    }

    private Object _navigation(final T caller, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        switch (postcard.getType()) {
            case ACTIVITY:
                // Build intent
                final Intent intent = new Intent(getContext(), postcard.getDestination());
                intent.putExtras(postcard.getExtras());

                // Set flags.
                int flags = postcard.getFlags();
                if (-1 != flags) {
                    intent.setFlags(flags);
                }

                // Navigation in main looper.
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        start(caller, requestCode, postcard, intent);
                        if (null != callback) { // Navigation over.
                            callback.onArrival(postcard);
                        }
                    }
                });

                break;
            case PROVIDER:
                return postcard.getProvider();
            case BOARDCAST:
            case CONTENT_PROVIDER:
            case FRAGMENT:
                Class fragmentMeta = postcard.getDestination();
                try {
                    Object instance = fragmentMeta.getConstructor().newInstance();
                    if (instance instanceof Fragment) {
                        ((Fragment) instance).setArguments(postcard.getExtras());
                    } else if (instance instanceof android.support.v4.app.Fragment) {
                        ((android.support.v4.app.Fragment) instance).setArguments(postcard.getExtras());
                    }

                    return instance;
                } catch (Exception ex) {
                    logger.error(Consts.TAG, "Fetch fragment instance error, " + TextUtils.formatStackTrace(ex.getStackTrace()));
                }
            case METHOD:
            case SERVICE:
            default:
                return null;
        }

        return null;
    }


    protected abstract void start(T caller, int requestCode, Postcard postcard, Intent intent);

    @Nullable
    protected abstract Context getContext();
}

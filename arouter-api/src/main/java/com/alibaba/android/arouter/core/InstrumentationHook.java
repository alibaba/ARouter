package com.alibaba.android.arouter.core;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.Consts;
import com.alibaba.android.arouter.utils.TextUtils;

import java.lang.reflect.Field;


/**
 * Use ARouter.getInstance().inject(this) now!
 *
 * Hook the instrumentation, inject values for activity's field.
 * Support normal activity only, not contain unit test.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2016/11/24 16:42
 */
@Deprecated
public class InstrumentationHook extends Instrumentation {
    /**
     * Hook the instrumentation's newActivity, inject
     * <p>
     * Perform instantiation of the process's {@link Activity} object.  The
     * default implementation provides the normal system behavior.
     *
     * @param cl        The ClassLoader with which to instantiate the object.
     * @param className The name of the class implementing the Activity
     *                  object.
     * @param intent    The Intent object that specified the activity class being
     *                  instantiated.
     * @return The newly instantiated Activity object.
     */
    public Activity newActivity(ClassLoader cl, String className,
                                Intent intent)
            throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {

//        return (Activity)cl.loadClass(className).newInstance();

        Class<?> targetActivity = cl.loadClass(className);
        Object instanceOfTarget = targetActivity.newInstance();

        if (ARouter.canAutoInject()) {
            try {
                String[] autoInjectParams = intent.getStringArrayExtra(ARouter.AUTO_INJECT);
                if (null != autoInjectParams && autoInjectParams.length > 0) {
                    Bundle extras = intent.getExtras();
                    for (String paramsName : autoInjectParams) {
                        String fieldName = TextUtils.getLeft(paramsName);
                        Object value = null == extras ? null : extras.get(fieldName);
                        if (null != value) {
                            try {
                                Field injectField = targetActivity.getDeclaredField(fieldName);
                                injectField.setAccessible(true);
                                injectField.set(instanceOfTarget, value);
                            } catch (Exception e) {
                                logError("Inject values for activity error! [" + e.getMessage() + "]");
                            }
                        }
                    }
                }
            } catch (RuntimeException malformedExtras) {
                // Activity creation must not be aborted by malformed or unparcelable Intent extras.
                logWarning("Skip legacy auto-inject for malformed activity extras. ["
                        + malformedExtras.getClass().getSimpleName() + "]");
            }
        }

        return (Activity) instanceOfTarget;
    }

    private static void logError(String message) {
        if (ARouter.logger == null) {
            Log.e(Consts.TAG, message);
        } else {
            ARouter.logger.error(Consts.TAG, message);
        }
    }

    private static void logWarning(String message) {
        if (ARouter.logger == null) {
            Log.w(Consts.TAG, message);
        } else {
            ARouter.logger.warning(Consts.TAG, message);
        }
    }
}

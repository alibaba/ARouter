package com.alibaba.android.arouter.core;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.Consts;

import java.lang.reflect.Field;


/**
 * Hook the instrumentation, inject values for activity's field.
 * Support normal activity only, not contain unit test.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2016/11/24 16:42
 */
public class InstrumentationHook extends Instrumentation {

    /**
     * Perform instantiation of an {@link Activity} object.  This method is intended for use with
     * unit tests, such as android.test.ActivityUnitTestCase.  The activity will be useable
     * locally but will be missing some of the linkages necessary for use within the sytem.
     *
     * @param clazz The Class of the desired Activity
     * @param context The base context for the activity to use
     * @param token The token for this activity to communicate with
     * @param application The application object (if any)
     * @param intent The intent that started this Activity
     * @param info ActivityInfo from the manifest
     * @param title The title, typically retrieved from the ActivityInfo record
     * @param parent The parent Activity (if any)
     * @param id The embedded Id (if any)
     * @param lastNonConfigurationInstance Arbitrary object that will be
     * available via {@link Activity#getLastNonConfigurationInstance()
     * Activity.getLastNonConfigurationInstance()}.
     * @return Returns the instantiated activity
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
//    public Activity newActivity(Class<?> clazz, Context context,
//                                IBinder token, Application application, Intent intent, ActivityInfo info,
//                                CharSequence title, Activity parent, String id,
//                                Object lastNonConfigurationInstance) throws InstantiationException,
//            IllegalAccessException {
//        Activity activity = (Activity)clazz.newInstance();
//        ActivityThread aThread = null;
//        activity.attach(context, aThread, this, token, 0, application, intent,
//                info, title, parent, id,
//                (Activity.NonConfigurationInstances)lastNonConfigurationInstance,
//                new Configuration(), null, null);
//        return activity;
//    }

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
            String[] autoInjectParams = intent.getStringArrayExtra(ARouter.AUTO_INJECT);
            if (null != autoInjectParams && autoInjectParams.length > 0) {
                for (String paramsName : autoInjectParams) {
                    Object value = intent.getExtras().get(LogisticsCenter.getLeft(paramsName));
                    if (null != value) {
                        try {
                            Field injectField = targetActivity.getDeclaredField(LogisticsCenter.getLeft(paramsName));
                            injectField.setAccessible(true);
                            injectField.set(instanceOfTarget, value);
                        } catch (Exception e) {
                            ARouter.logger.error(Consts.TAG, "Inject values for activity error! [" + e.getMessage() + "]");
                        }
                    }
                }
            }
        }

        return (Activity) instanceOfTarget;
    }
}

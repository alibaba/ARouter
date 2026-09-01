package com.alibaba.android.arouter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import static com.alibaba.android.arouter.launcher.ARouter.logger;
import static com.alibaba.android.arouter.utils.Consts.AROUTER_SP_CACHE_KEY;
import static com.alibaba.android.arouter.utils.Consts.LAST_UPDATE_TIME;
import static com.alibaba.android.arouter.utils.Consts.LAST_VERSION_CODE;
import static com.alibaba.android.arouter.utils.Consts.LAST_VERSION_NAME;

/**
 * Android package utils
 *
 * @author zhilong <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2017/8/8 下午8:19
 */
public class PackageUtils {
    private static String NEW_VERSION_NAME;
    private static int NEW_VERSION_CODE;
    private static long NEW_LAST_UPDATE_TIME;

    public static boolean isNewVersion(Context context) {
        clearPendingVersion();
        PackageInfo packageInfo = getPackageInfo(context);
        if (null != packageInfo) {
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            long lastUpdateTime = packageInfo.lastUpdateTime;

            SharedPreferences sp = context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE);
            if (!equals(versionName, sp.getString(LAST_VERSION_NAME, null))
                    || versionCode != sp.getInt(LAST_VERSION_CODE, -1)
                    || lastUpdateTime != sp.getLong(LAST_UPDATE_TIME, -1L)) {
                // New APK install/update or cache schema migration.
                NEW_VERSION_NAME = versionName;
                NEW_VERSION_CODE = versionCode;
                NEW_LAST_UPDATE_TIME = lastUpdateTime;

                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static void updateVersion(Context context) {
        if (NEW_VERSION_NAME != null && NEW_VERSION_NAME.length() > 0 && NEW_VERSION_CODE != 0) {
            SharedPreferences sp = context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE);
            sp.edit()
                    .putString(LAST_VERSION_NAME, NEW_VERSION_NAME)
                    .putInt(LAST_VERSION_CODE, NEW_VERSION_CODE)
                    .putLong(LAST_UPDATE_TIME, NEW_LAST_UPDATE_TIME)
                    .apply();
        }
        clearPendingVersion();
    }

    private static boolean equals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static void clearPendingVersion() {
        NEW_VERSION_NAME = null;
        NEW_VERSION_CODE = 0;
        NEW_LAST_UPDATE_TIME = 0L;
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
        } catch (Exception ex) {
            logger.error(Consts.TAG, "Get package info error.", ex);
        }

        return packageInfo;
    }
}

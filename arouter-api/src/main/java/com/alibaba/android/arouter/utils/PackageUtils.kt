package com.alibaba.android.arouter.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.alibaba.android.arouter.launcher.ARouter.logger
import com.alibaba.android.arouter.utils.Consts.*
import java.lang.Exception

/**
 * Android package utils
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/8/8 下午7:38
 */
object PackageUtils {
    fun isNewVersion(context: Context): Boolean {
        val packageInfo = getPackageInfo(context)
        if (null != packageInfo) {
            val versionName = packageInfo.versionName
            val versionCode = packageInfo.versionCode

            val sp = context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE)

            if (versionName != sp.getString(LAST_VERSION_NAME, null) || versionCode != sp.getInt(LAST_VERSION_CODE, -1)) {
                // new version.
                sp.edit().putString(LAST_VERSION_NAME, versionName).putInt(LAST_VERSION_CODE, versionCode).apply()
                return true
            } else {
                return false
            }
        } else {
            return true
        }
    }

    fun getPackageInfo(context: Context): PackageInfo? {
        var packageInfo: PackageInfo? = null

        try {
            packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_CONFIGURATIONS)
        } catch (e: Exception) {
            logger.error(Consts.TAG, "Get package info error.")
        }

        return packageInfo
    }
}
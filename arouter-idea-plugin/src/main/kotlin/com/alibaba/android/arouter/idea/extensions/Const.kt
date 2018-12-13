package com.alibaba.android.arouter.idea.extensions

import com.intellij.openapi.util.IconLoader

/**
 * Const of plugin
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2018/12/13 8:06 PM
 */
object Const {
    const val ROUTE_ANNOTATION_NAME = "com.alibaba.android.arouter.facade.annotation.Route"
    const val SDK_NAME = "ARouter"

    // Notify
    const val NOTIFY_SERVICE_NAME = "ARouter Plugin Tips"
    const val NOTIFY_TITLE = "Road Sign"
    const val NOTIFY_NO_TARGET_TIPS = "No destination found or unsupported type."

    val navigationOnIcon = IconLoader.getIcon("icon/outline-location_on-24px.svg")
    val navigationOffIcon = IconLoader.getIcon("icon/outline-location_off-24px.svg")
}
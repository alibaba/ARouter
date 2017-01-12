package com.alibaba.android.arouter.facade.enums;

/**
 * Type of route enum.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 22:33
 */
public enum RouteType {
    ACTIVITY(0, "android.app.Activity"),
    SERVICE(1, "android.app.Service"),
    PROVIDER(2, "com.alibaba.android.arouter.facade.template.IProvider"),
    CONTENT_PROVIDER(-1, "android.app.ContentProvider"),
    BOARDCAST(-1, ""),
    METHOD(-1, ""),
    FRAGMENT(-1, "android.app.Fragment"),
    UNKNOWN(-1, "Unknown route type");

    int id;
    String className;

    public int getId() {
        return id;
    }

    public RouteType setId(int id) {
        this.id = id;
        return this;
    }

    public String getClassName() {
        return className;
    }

    public RouteType setClassName(String className) {
        this.className = className;
        return this;
    }

    RouteType(int id, String className) {
        this.id = id;
        this.className = className;
    }

    public static RouteType parse(String name) {
        for (RouteType routeType : RouteType.values()) {
            if (routeType.getClassName().equals(name)) {
                return routeType;
            }
        }

        return UNKNOWN;
    }
}

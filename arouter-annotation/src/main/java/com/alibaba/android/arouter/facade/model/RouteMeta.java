package com.alibaba.android.arouter.facade.model;

import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.enums.RouteType;

import java.util.Map;

import javax.lang.model.element.Element;

/**
 * It contains basic route information.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/24 09:45
 */
public class RouteMeta {
    private RouteType type;         // Type of route
    private Element rawType;        // Raw type of route
    private Class<?> destination;   // Destination
    private String path;            // Path of route
    private String group;           // Group of route
    private int priority = -1;      // The smaller the number, the higher the priority
    private int extra;              // Extra data
    private Map<String, Integer> paramsType;  // Param type
    private String name;

    private Map<String, Autowired> injectConfig;  // Cache inject config.

    public RouteMeta() {
    }

    /**
     * For versions of 'compiler' less than 1.0.7, contain 1.0.7
     *
     * @param type        type
     * @param destination destination
     * @param path        path
     * @param group       group
     * @param priority    priority
     * @param extra       extra
     * @return this
     */
    public static RouteMeta build(RouteType type, Class<?> destination, String path, String group, int priority, int extra) {
        return new RouteMeta(type, null, destination, null, path, group, null, priority, extra);
    }

    /**
     * For versions of 'compiler' greater than 1.0.7
     *
     * @param type        type
     * @param destination destination
     * @param path        path
     * @param group       group
     * @param paramsType  paramsType
     * @param priority    priority
     * @param extra       extra
     * @return this
     */
    public static RouteMeta build(RouteType type, Class<?> destination, String path, String group, Map<String, Integer> paramsType, int priority, int extra) {
        return new RouteMeta(type, null, destination, null, path, group, paramsType, priority, extra);
    }

    /**
     * Type
     *
     * @param route       route
     * @param destination destination
     * @param type        type
     */
    public RouteMeta(Route route, Class<?> destination, RouteType type) {
        this(type, null, destination, route.name(), route.path(), route.group(), null, route.priority(), route.extras());
    }

    /**
     * Type
     *
     * @param route      route
     * @param rawType    rawType
     * @param type       type
     * @param paramsType paramsType
     */
    public RouteMeta(Route route, Element rawType, RouteType type, Map<String, Integer> paramsType) {
        this(type, rawType, null, route.name(), route.path(), route.group(), paramsType, route.priority(), route.extras());
    }

    /**
     * Type
     *
     * @param type        type
     * @param rawType     rawType
     * @param destination destination
     * @param path        path
     * @param group       group
     * @param paramsType  paramsType
     * @param priority    priority
     * @param extra       extra
     */
    public RouteMeta(RouteType type, Element rawType, Class<?> destination, String name, String path, String group, Map<String, Integer> paramsType, int priority, int extra) {
        this.type = type;
        this.name = name;
        this.destination = destination;
        this.rawType = rawType;
        this.path = path;
        this.group = group;
        this.paramsType = paramsType;
        this.priority = priority;
        this.extra = extra;
    }

    public Map<String, Integer> getParamsType() {
        return paramsType;
    }

    public RouteMeta setParamsType(Map<String, Integer> paramsType) {
        this.paramsType = paramsType;
        return this;
    }

    public Map<String, Autowired> getInjectConfig() {
        return injectConfig;
    }

    public void setInjectConfig(Map<String, Autowired> injectConfig) {
        this.injectConfig = injectConfig;
    }

    public Element getRawType() {
        return rawType;
    }

    public RouteMeta setRawType(Element rawType) {
        this.rawType = rawType;
        return this;
    }

    public RouteType getType() {
        return type;
    }

    public RouteMeta setType(RouteType type) {
        this.type = type;
        return this;
    }

    public Class<?> getDestination() {
        return destination;
    }

    public RouteMeta setDestination(Class<?> destination) {
        this.destination = destination;
        return this;
    }

    public String getPath() {
        return path;
    }

    public RouteMeta setPath(String path) {
        this.path = path;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public RouteMeta setGroup(String group) {
        this.group = group;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public RouteMeta setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getExtra() {
        return extra;
    }

    public RouteMeta setExtra(int extra) {
        this.extra = extra;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RouteMeta{" +
                "type=" + type +
                ", rawType=" + rawType +
                ", destination=" + destination +
                ", path='" + path + '\'' +
                ", group='" + group + '\'' +
                ", priority=" + priority +
                ", extra=" + extra +
                ", paramsType=" + paramsType +
                ", name='" + name + '\'' +
                '}';
    }
}
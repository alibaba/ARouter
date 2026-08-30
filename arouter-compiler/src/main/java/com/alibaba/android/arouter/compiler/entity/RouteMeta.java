package com.alibaba.android.arouter.compiler.entity;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.enums.RouteType;

import java.util.Map;

import javax.lang.model.element.Element;

/**
 * Compile-time route metadata.
 *
 * <p>The annotation-processing {@link Element} must remain in the compiler
 * artifact and must not leak into the runtime route model consumed by Android
 * applications.</p>
 */
public class RouteMeta extends com.alibaba.android.arouter.facade.model.RouteMeta {
    private Element rawType;

    public RouteMeta() {
    }

    public RouteMeta(Route route, Element rawType, RouteType type, Map<String, Integer> paramsType) {
        this(type, rawType, null, route.name(), route.path(), route.group(), paramsType, route.priority(), route.extras());
    }

    public RouteMeta(RouteType type, Element rawType, Class<?> destination, String name, String path, String group,
                     Map<String, Integer> paramsType, int priority, int extra) {
        super(type, destination, name, path, group, paramsType, priority, extra);
        this.rawType = rawType;
    }

    public Element getRawType() {
        return rawType;
    }

    public RouteMeta setRawType(Element rawType) {
        this.rawType = rawType;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + " rawType=" + rawType;
    }
}

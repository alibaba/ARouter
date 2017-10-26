package com.alibaba.android.arouter.facade.service;

import com.alibaba.android.arouter.facade.template.IProvider;

import java.lang.reflect.Type;

/**
 * Used for parse json string.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/4/10 下午1:43
 */
public interface SerializationService extends IProvider {

    /**
     * Parse json to object
     *
     * USE @parseObject PLEASE
     *
     * @param input json string
     * @param clazz object type
     * @return instance of object
     */
    @Deprecated
    <T> T json2Object(String input, Class<T> clazz);

    /**
     * Object to json
     *
     * @param instance obj
     * @return json string
     */
    String object2Json(Object instance);

    /**
     * Parse json to object
     *
     * @param input json string
     * @param clazz object type
     * @return instance of object
     */
    <T> T parseObject(String input, Type clazz);
}

package com.alibaba.android.arouter.facade.service;

import com.alibaba.android.arouter.facade.template.IProvider;

/**
 * Used for parse json string.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/4/10 下午1:43
 */
public interface SerializationService extends IProvider {
    /**
     * Parse json object.
     *
     * @param json  json str
     * @param clazz object type
     * @param <T>   type
     * @return instance
     */
    <T> T json2Object(String json, Class<T> clazz);

    /**
     * Object to json
     * @param instance obj
     * @return json string
     */
    String object2Json(Object instance);
}

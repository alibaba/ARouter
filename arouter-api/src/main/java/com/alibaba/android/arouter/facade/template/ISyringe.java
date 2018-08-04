package com.alibaba.android.arouter.facade.template;

import java.io.Serializable;

/**
 * Template of syringe
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/20 下午4:41
 */
public interface ISyringe {
    void inject(Object target);

    default <T extends Serializable> T primitiveParse(Serializable value){
        return (T) value;
    }
}

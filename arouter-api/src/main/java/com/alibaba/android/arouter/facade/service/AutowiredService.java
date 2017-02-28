package com.alibaba.android.arouter.facade.service;

import com.alibaba.android.arouter.facade.template.IProvider;

/**
 * Service for autowired.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/28 下午6:06
 */
public interface AutowiredService extends IProvider {

    /**
     * Autowired core.
     * @param instance the instance who need autowired.
     */
    void autowire(Object instance);
}

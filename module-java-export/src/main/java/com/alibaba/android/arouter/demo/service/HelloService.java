package com.alibaba.android.arouter.demo.service;

import com.alibaba.android.arouter.facade.template.IProvider;

/**
 * 通过 service module 提供给使用方依赖，使用方可以不依赖具体实现，只需要保证最终打包在 app 中即可
 *
 * @author Alex <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/1/3 10:26
 */
public interface HelloService extends IProvider {
    void sayHello(String name);
}

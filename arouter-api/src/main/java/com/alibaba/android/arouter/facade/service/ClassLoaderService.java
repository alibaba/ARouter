package com.alibaba.android.arouter.facade.service;

import com.alibaba.android.arouter.facade.template.IProvider;

/**
 * Get class by user, maybe someone use InstantRun and other tech will move dex files.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/23 下午12:16
 */
public interface ClassLoaderService extends IProvider {
    Class<?> forName();
}

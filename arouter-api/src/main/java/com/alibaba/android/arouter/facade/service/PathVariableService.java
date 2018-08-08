package com.alibaba.android.arouter.facade.service;

import android.net.Uri;
import com.alibaba.android.arouter.facade.template.IProvider;

/**
 * Created by tong on 2018/3/12.
 */
public interface PathVariableService extends IProvider {
    /**
     * For uri type.
     *
     * @param uri raw uri
     */
    Uri forUri(Uri uri);
}
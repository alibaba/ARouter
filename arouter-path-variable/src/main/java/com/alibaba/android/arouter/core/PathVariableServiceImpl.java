package com.alibaba.android.arouter.core;

import android.content.Context;
import android.net.Uri;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.service.PathVariableService;

/**
 * Created by tong on 2018/3/12.
 */
@Route(path = "/aroute_path_variable/service/pathvariable")
public class PathVariableServiceImpl implements PathVariableService {
    @Override
    public Uri forUri(Uri uri) {
        return ArouterPathVariableUtil.forUri(uri);
    }

    @Override
    public void init(Context context) {
        ArouterPathVariableUtil.init(context);
    }
}

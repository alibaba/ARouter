package com.alibaba.android.arouter.demo;

import android.content.Context;
import android.net.Uri;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.service.PathReplaceService;
import com.alibaba.android.arouter.pathvariable.ArouterPathVariableUtil;

/**
 * Created by tong on 2018/3/5.
 */
// 实现PathReplaceService接口，并加上一个Path内容任意的注解即可
@Route(path = "/xxx/xxx") // 必须标明注解
public class PathReplaceServiceImpl implements PathReplaceService {
    @Override
    public void init(Context context) {
        ArouterPathVariableUtil.init(context);
    }

    /**
     * For normal path.
     *
     * @param path raw path
     */
    @Override
    public String forString(String path) {
        return path;    // 按照一定的规则处理之后返回处理后的结果
    }

    /**
     * For uri type.
     *
     * @param uri raw uri
     */
    @Override
    public Uri forUri(Uri uri) {
        return ArouterPathVariableUtil.forUri(uri);    // 按照一定的规则处理之后返回处理后的结果
    }
}
package com.alibaba.android.arouter.demo.module1;

import android.content.Context;
import android.util.Log;

import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Interceptor;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.IInterceptor;

/**
 * TODO feature
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/9/9 14:34
 */
@Interceptor(priority = 90)
public class TestInterceptor90 implements IInterceptor {
    /**
     * The operation of this tollgate.
     *
     * @param postcard meta
     * @param callback cb
     */
    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
        callback.onContinue(postcard);
    }

    /**
     * Do your init work in this method, it well be call when processor has been load.
     *
     * @param context ctx
     */
    @Override
    public void init(Context context) {
        Log.e("test", "位于moudle1中的拦截器初始化了");
    }
}

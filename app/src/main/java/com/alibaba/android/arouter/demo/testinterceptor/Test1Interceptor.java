package com.alibaba.android.arouter.demo.testinterceptor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;

import com.alibaba.android.arouter.demo.MainActivity;
import com.alibaba.android.arouter.demo.MainLooper;
import com.alibaba.android.arouter.demo.testservice.HelloService;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.annotation.Interceptor;
import com.alibaba.android.arouter.facade.callback.InterceptorCallback;
import com.alibaba.android.arouter.facade.template.IInterceptor;

/**
 * TODO feature
 *
 * @author Alex <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/1/3 11:20
 */
@Interceptor(priority = 7)
public class Test1Interceptor implements IInterceptor {
    Context mContext;

    /**
     * The operation of this interceptor.
     *
     * @param postcard meta
     * @param callback cb
     */
    @Override
    public void process(final Postcard postcard, final InterceptorCallback callback) {
        if ("/test/activity4".equals(postcard.getPath())) {
            final AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.getThis());
            ab.setCancelable(false);
            ab.setTitle("温馨提醒");
            ab.setMessage("想要跳转到Test4Activity么？(触发了\"/inter/test1\"拦截器，拦截了本次跳转)");
            ab.setNegativeButton("继续", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callback.onContinue(postcard);
                }
            });
            ab.setNeutralButton("算了", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callback.onInterrupt(null);
                }
            });
            ab.setPositiveButton("加点料", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    postcard.withString("extra", "我是在拦截器中附加的参数");
                    callback.onContinue(postcard);
                }
            });

            MainLooper.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ab.create().show();
                }
            });
        } else {
            callback.onContinue(postcard);
        }
    }

    /**
     * Do your init work in this method, it well be call when processor has been load.
     *
     * @param context ctx
     */
    @Override
    public void init(Context context) {
        mContext = context;
        Log.e("testService", Test1Interceptor.class.getName() + " has init.");
    }
}

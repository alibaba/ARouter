package com.alibaba.android.arouter.demo.module1.testservice;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.android.arouter.demo.service.HelloService;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * TODO feature
 *
 * @author Alex <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/1/3 10:26
 */
@Route(path = "/yourservicegroupname/hello")
public class HelloServiceImpl implements HelloService {
    Context mContext;

    @Override
    public void sayHello(String name) {
        Toast.makeText(mContext, "Hello " + name, Toast.LENGTH_SHORT).show();
    }

    /**
     * Do your init work in this method, it well be call when processor has been load.
     *
     * @param context ctx
     */
    @Override
    public void init(Context context) {
        mContext = context;
        Log.e("testService", HelloService.class.getName() + " has init.");
    }
}

package com.alibaba.android.arouter.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.alibaba.android.arouter.demo.testservice.HelloService;
import com.alibaba.android.arouter.launcher.ARouter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;
    }

    public static Activity getThis() {
        return activity;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openLog:
                ARouter.openLog();
                break;
            case R.id.openDebug:
                ARouter.openDebug();
                break;
            case R.id.init:
                ARouter.init(AppContext.getInstance());
                break;
            case R.id.normalNavigation:
                ARouter.getInstance()
                        .build("/test/activity2")
                        .navigation();
                break;
            case R.id.normalNavigationWithParams:
                ARouter.getInstance()
                        .build("/test/activity2")
                        .withString("key1", "value1")
                        .navigation();
                break;
            case R.id.interceptor:
                ARouter.getInstance()
                        .build("/test/activity4")
                        .navigation();
                break;
            case R.id.navByUrl:
                ARouter.getInstance()
                        .build("/test/webview")
                        .withString("url", "file:///android_asset/schame-test.html")
                        .navigation();
                break;
            case R.id.autoInject:
                ARouter.enableAutoInject();
                break;
            case R.id.navByName:
                ((HelloService)ARouter.getInstance().build("/service/hello").navigation()).sayHello("mike");
                break;
            case R.id.navByType:
                ARouter.getInstance().navigation(HelloService.class).sayHello("mike");
                break;
            case R.id.navToMoudle1:
                // 这个页面主动指定了Group名
                ARouter.getInstance().build("/module/1", "m1").navigation();
                break;
            case R.id.navToMoudle2:
                // 这个页面主动指定了Group名
                ARouter.getInstance().build("/module/2", "m2").navigation();
                break;
            case R.id.destroy:
                ARouter.getInstance().destroy();
                break;
            default:
                break;
        }
    }
}

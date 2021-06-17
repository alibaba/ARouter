package com.alibaba.android.arouter.demo.service;

import android.content.Context;

import com.alibaba.android.arouter.launcher.ARouter;

public class Entrance {
    /**
     * 跳转到 Test1 Activity,
     *
     * @param name    姓名
     * @param age     年龄
     * @param context ctx
     */
    public static void redirect2Test1Activity(String name, int age, Context context) {
        ARouter.getInstance().build("/test/activity1")
                .withString("name", name)
                .withInt("age", age)
                .navigation(context);
    }
}

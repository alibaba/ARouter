package com.alibaba.android.arouter.demo.module1.testactivity;

import android.os.Bundle;

// 用于测试不标注 Route 的情况下，动态增加路由
//@Route(path="/dynamic/activity")
public class TestDynamicActivity extends Test1Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}

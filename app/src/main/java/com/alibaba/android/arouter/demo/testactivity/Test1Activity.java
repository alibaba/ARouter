package com.alibaba.android.arouter.demo.testactivity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.facade.annotation.Param;
import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * https://m.aliyun.com/test/activity1?name=老王&age=23&boy=true&high=180
 */
@Route(path = "/test/activity1")
public class Test1Activity extends AppCompatActivity {

    @Param
    private String name;

    @Param
    private int age;

    @Param(name = "boy")
    private boolean girl;

    private long high;

    @Param
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);

        name = getIntent().getStringExtra("name");
        age = getIntent().getIntExtra("age", 0);
        girl = getIntent().getBooleanExtra("girl", false);
        high = getIntent().getLongExtra("high", 0);
        url = getIntent().getStringExtra("url");

        String params = String.format("name=%s, age=%s, girl=%s, high=%s, url=%s", name, age, girl, high, url);

        ((TextView)findViewById(R.id.test)).setText("I am " + Test1Activity.class.getName());
        ((TextView)findViewById(R.id.test2)).setText(params);
    }
}

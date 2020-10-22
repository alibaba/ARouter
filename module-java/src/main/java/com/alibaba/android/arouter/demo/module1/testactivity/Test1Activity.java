package com.alibaba.android.arouter.demo.module1.testactivity;

import android.os.Bundle;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.module1.R;
import com.alibaba.android.arouter.demo.service.HelloService;
import com.alibaba.android.arouter.demo.service.model.TestObj;
import com.alibaba.android.arouter.demo.service.model.TestParcelable;
import com.alibaba.android.arouter.demo.service.model.TestSerializable;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

import java.util.List;
import java.util.Map;

/**
 * https://m.aliyun.com/test/activity1?name=老王&age=23&boy=true&high=180
 */
@Route(path = "/test/activity1", name = "测试用 Activity")
public class Test1Activity extends BaseActivity {
    @Autowired
    int age = 10;

    @Autowired
    int height = 175;

    @Autowired(name = "boy", required = true)
    boolean girl;

    @Autowired
    char ch = 'A';

    @Autowired
    float fl = 12.00f;

    @Autowired
    double dou = 12.01d;

    @Autowired
    TestSerializable ser;

    @Autowired
    TestParcelable pac;

    @Autowired
    TestObj obj;

    @Autowired
    List<TestObj> objList;

    @Autowired
    Map<String, List<TestObj>> map;

    private long high;

    @Autowired
    String url;

    @Autowired
    HelloService helloService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test1);

        ARouter.getInstance().inject(this);

        // No more getter ...
        // name = getIntent().getStringExtra("name");
        // age = getIntent().getIntExtra("age", 0);
        // girl = getIntent().getBooleanExtra("girl", false);
        // high = getIntent().getLongExtra("high", 0);
        // url = getIntent().getStringExtra("url");

        String params = String.format(
                "name=%s,\n age=%s, \n height=%s,\n girl=%s,\n high=%s,\n url=%s,\n ser=%s,\n pac=%s,\n obj=%s \n ch=%s \n fl = %s, \n dou = %s, \n objList=%s, \n map=%s",
                name,
                age,
                height,
                girl,
                high,
                url,
                ser,
                pac,
                obj,
                ch,
                fl,
                dou,
                objList,
                map
        );
        helloService.sayHello("Hello moto.");

        ((TextView) findViewById(R.id.test)).setText("I am " + Test1Activity.class.getName());
        ((TextView) findViewById(R.id.test2)).setText(params);
    }
}

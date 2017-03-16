package com.alibaba.android.arouter.demo.testactivity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.demo.testinject.TestObj;
import com.alibaba.android.arouter.demo.testinject.TestParcelable;
import com.alibaba.android.arouter.demo.testservice.HelloService;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

/**
 * https://m.aliyun.com/test/activity1?name=老王&age=23&boy=true&high=180
 */
@Route(path = "/test/activity1")
public class Test1Activity extends AppCompatActivity {

    @Autowired
    String name;

    @Autowired
    int age;

    @Autowired(name = "boy")
    boolean girl;

    @Autowired
    TestParcelable pac;

    @Autowired
    TestObj obj;

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
                "name=%s,\n age=%s,\n girl=%s,\n high=%s,\n url=%s,\n pac=%s,\n obj=%s",
                name,
                age,
                girl,
                high,
                url,
                pac,
                obj
        );
        helloService.sayHello("Hello moto.");

        ((TextView)findViewById(R.id.test)).setText("I am " + Test1Activity.class.getName());
        ((TextView)findViewById(R.id.test2)).setText(params);
    }
}

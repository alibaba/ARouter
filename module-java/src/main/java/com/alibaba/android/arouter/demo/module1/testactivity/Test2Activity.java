package com.alibaba.android.arouter.demo.module1.testactivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.android.arouter.demo.module1.R;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;

@Route(path = "/test/activity2", priority = 7)
public class Test2Activity extends AppCompatActivity {

    @Autowired
    String key1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test2);

        String value = getIntent().getStringExtra("key1");
        if (!TextUtils.isEmpty(value)) {
            Toast.makeText(this, "exist param :" + value, Toast.LENGTH_LONG).show();
        }

        setResult(999);
    }
}

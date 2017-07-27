package com.alibaba.android.arouter.demo.testactivity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

@Route(path = "/test/activity2")
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

        Log.e("TAG", "666 -- " + ((666 + 1) << 16) + (666 & 0xffff));

        Fragment fragment = (Fragment) ARouter.getInstance().build("/test/fragment").navigation();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frame, fragment);
        transaction.show(fragment);
        transaction.commit();
    }

}

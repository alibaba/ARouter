package com.alibaba.android.arouter.demo.testfragment;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.facade.annotation.Route;

@Route(path = "/test/target")
public class TestTargetActivity extends AppCompatActivity {
    private static final String TAG = "TestTargetActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_target);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.finish_result_ok:
                setResult(RESULT_OK);
                break;
            case R.id.finish_result_cancel:
                setResult(RESULT_CANCELED);
                break;
            case R.id.finish:
                break;
        }
        finish();
    }
}

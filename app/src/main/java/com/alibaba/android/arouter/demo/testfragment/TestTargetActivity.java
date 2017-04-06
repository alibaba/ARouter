package com.alibaba.android.arouter.demo.testfragment;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

@Route(path = "/test/target")
public class TestTargetActivity extends AppCompatActivity {
    private static final String TAG = "TestTargetActivity";

    @Autowired(name = "args")
    String args;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_target);

        ARouter.getInstance().inject(this);
        Toast.makeText(this, "路由参数：args=" + args, Toast.LENGTH_LONG).show();
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

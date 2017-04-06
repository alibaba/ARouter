package com.alibaba.android.arouter.demo.testfragment;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

@Route(path = "/test/fragment_container")
public class TestFragmentContainerActivity extends AppCompatActivity {
    private static final String TAG = "TestFragmentContainer";
    android.support.v4.app.Fragment v4Fragment;
    android.app.Fragment appFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_fragment_container);

        v4Fragment = (android.support.v4.app.Fragment) ARouter.getInstance().build("/test/activity_result/fragment_v4").navigation();
        appFragment = (android.app.Fragment) ARouter.getInstance().build("/test/activity_result/fragment_app").navigation();
    }

    public void onClick(View v) {
        if (v4Fragment == null || appFragment == null) {
            Toast.makeText(this, "Fragment 实例获取失败", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()) {
            case R.id.show_v4_fragment:
                getFragmentManager().beginTransaction().remove(appFragment).commit();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, v4Fragment).commit();
                break;
            case R.id.show_app_fragment:
                getSupportFragmentManager().beginTransaction().remove(v4Fragment).commit();
                getFragmentManager().beginTransaction().replace(R.id.fragment_container, appFragment).commit();
                break;
            default:
                break;
        }
    }
}

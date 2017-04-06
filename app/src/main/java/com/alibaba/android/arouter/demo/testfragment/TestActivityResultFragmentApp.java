package com.alibaba.android.arouter.demo.testfragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

/**
 * @author lishile  <a href="mailto:man.chester.lee.cn@gmail.com">Contact me.</a>
 * @version 1.0
 * @since 6/4/17
 */
@Route(path = "/test/activity_result/fragment_app")
public class TestActivityResultFragmentApp extends Fragment {
    private static final int REQUEST_CODE = 200;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_app_test_activity_result, container, false);
        root.findViewById(R.id.start_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ARouter.getInstance().build("/test/target")
                        .withString("args", "fragment_app")
                        .navigation(TestActivityResultFragmentApp.this.getActivity(),
                                TestActivityResultFragmentApp.this, REQUEST_CODE);
            }
        });
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE == requestCode) {
            Toast.makeText(getActivity(), "FragmentApp::onActivityResult::" + "resultCode = " + resultCode, Toast.LENGTH_LONG).show();
        }
    }
}

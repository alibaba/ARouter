package com.alibaba.android.arouter.demo.testcaller;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.R;
import com.alibaba.android.arouter.launcher.ARouter;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class NormalFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = NormalFragment.class.toString();
    private static final int REQ = 123;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_start, container, false);
        inflate.findViewById(R.id.navByFragment).setOnClickListener(this);
        inflate.findViewById(R.id.navByFragmentForResult).setOnClickListener(this);
        ((TextView) inflate.findViewById(R.id.tv_title)).setText("Fragment(请先初始化)");
        return inflate;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.navByFragment:
                ARouter.getInstance()
                        .build("/test/activity2")
                        .navigation();
                break;
            case R.id.navByFragmentForResult:
                ARouter.getInstance().build("/test/activity2").navigation(this, REQ);
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQ == requestCode) {
            Log.e(TAG, "onActivityResult: " + resultCode);
        }
    }
}

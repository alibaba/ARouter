package com.alibaba.android.arouter.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.launcher.engine.Caller;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class StartFragment extends Fragment implements View.OnClickListener, Caller {
    private static final String TAG = StartFragment.class.toString();
    private static final int REQ = 123;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_start, container, false);
        inflate.findViewById(R.id.navByFragment).setOnClickListener(this);
        inflate.findViewById(R.id.navByFragmentForResult).setOnClickListener(this);
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

    @Override
    public void overridePendingTransition(int enterAnim, int exitAnim) {
        //ignore
    }
}

package com.alibaba.android.arouter.demo.testcaller;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.R;

/**
 * auth zzc
 * date 2018/3/19
 * desc ${desc}
 */

public class CallerFragment extends Fragment {

    private CallerPresenter mPresenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.fragment_start, container, false);
        mPresenter = new CallerPresenter(this);
        inflate.findViewById(R.id.navByFragment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.start();
            }
        });
        inflate.findViewById(R.id.navByFragmentForResult).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.startForResult();
            }
        });
        ((TextView) inflate.findViewById(R.id.tv_title)).setText("Caller(请先初始化)");
        return inflate;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPresenter.onResult(requestCode, resultCode, data);
    }
}

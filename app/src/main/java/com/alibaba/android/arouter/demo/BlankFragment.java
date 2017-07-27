package com.alibaba.android.arouter.demo;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.alibaba.android.arouter.demo.testinject.TestObj;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;

/**
 * A simple {@link Fragment} subclass.
 */
@Route(path = "/test/fragment")
public class BlankFragment extends Fragment implements View.OnClickListener {

    @Autowired
    String name;

    @Autowired(required = true)
    TestObj obj;

    public BlankFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = new TextView(getActivity());
        textView.setText("I am Fragment\n点击通过Fragment.startActivityForResult()跳转");
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        textView.setOnClickListener(this);
        return textView;
    }

    @Override
    public void onClick(View v) {
        ARouter.getInstance().build("/test/activity1").navigation(this, 666);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("TAG", "BlankFragment onActivityResult " + requestCode);
    }

}

package com.alibaba.android.arouter.demo.extrasdex;


import android.content.Context;
import android.widget.Toast;

import com.alibaba.android.arouter.demo.extrasdex.ExternalDexProvider;
import com.alibaba.android.arouter.facade.annotation.Route;

@Route(path = "/extras/external/patch/route")
public class ExternalDexProviderImpl implements ExternalDexProvider {

    private Context mContext;

    @Override
    public void init(Context context) {
        mContext = context;
    }

    @Override
    public void doSomethings() {
        Toast.makeText(mContext, " Here will be used by hot swap code.", Toast.LENGTH_SHORT).show();
    }
}

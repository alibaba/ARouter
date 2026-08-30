package com.alibaba.android.arouter.demo.module1.testservice;

import android.content.Context;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.template.IProvider;

@Route(path = "/diagnostics/failing-provider")
public class FailingProvider implements IProvider {
    public static final String FAILURE_MESSAGE = "provider fixture failure";

    @Override
    public void init(Context context) {
        throw new IllegalStateException(FAILURE_MESSAGE);
    }
}

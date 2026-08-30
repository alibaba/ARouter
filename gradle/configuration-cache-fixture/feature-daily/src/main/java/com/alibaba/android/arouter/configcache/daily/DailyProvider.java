package com.alibaba.android.arouter.configcache.daily;

import android.content.Context;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.template.IProvider;

@Route(path = "/fixture/daily")
public final class DailyProvider implements IProvider {
    @Override
    public void init(Context context) {
    }
}

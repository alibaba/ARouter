package com.alibaba.android.arouter.configcache.online;

import android.content.Context;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.template.IProvider;

@Route(path = "/fixture/online")
public final class OnlineProvider implements IProvider {
    @Override
    public void init(Context context) {
    }
}

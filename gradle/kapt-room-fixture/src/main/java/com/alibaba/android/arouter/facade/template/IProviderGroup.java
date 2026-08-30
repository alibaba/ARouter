package com.alibaba.android.arouter.facade.template;

import com.alibaba.android.arouter.facade.model.RouteMeta;

import java.util.Map;

public interface IProviderGroup {
    void loadInto(Map<String, RouteMeta> providers);
}

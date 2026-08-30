package com.alibaba.android.arouter.facade.template;

import java.util.Map;

public interface IInterceptorGroup {
    void loadInto(Map<Integer, Class<? extends IInterceptor>> interceptors);
}

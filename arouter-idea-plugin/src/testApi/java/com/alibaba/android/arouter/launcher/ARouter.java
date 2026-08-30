package com.alibaba.android.arouter.launcher;

public final class ARouter {
    private static final ARouter INSTANCE = new ARouter();

    private ARouter() {
    }

    public static ARouter getInstance() {
        return INSTANCE;
    }

    public Object build(String path) {
        return null;
    }

    public Object build(String path, String group) {
        return null;
    }
}

package com.alibaba.android.arouter.utils;

/**
 * ARouter constants.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 9:38
 */
public final class Consts {
    public static final String SDK_NAME = "ARouter";
    public static final String TAG = SDK_NAME + "::";
    public static final String SEPARATOR = "$$";
    public static final String SUFFIX_ROOT = "Root";
    public static final String SUFFIX_INTERCEPTORS = "Interceptors";
    public static final String SUFFIX_PROVIDERS = "Providers";
    public static final String SUFFIX_AUTOWIRED = SEPARATOR + SDK_NAME + SEPARATOR + "Autowired";
    public static final String DOT = ".";
    public static final String ROUTE_ROOT_PAKCAGE = "com.alibaba.android.arouter.routes";

    public static final String AROUTER_SP_CACHE_KEY = "SP_AROUTER_CACHE";
    public static final String AROUTER_SP_KEY_MAP = "ROUTER_MAP";

    public static final String LAST_VERSION_NAME = "LAST_VERSION_NAME";
    public static final String LAST_VERSION_CODE = "LAST_VERSION_CODE";

    //modified by hanshengjian 2018/11/8
    public static final String PRE_BASE = ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR;
    public static final String PRE_ROUTE_ROOT = PRE_BASE + SUFFIX_ROOT;
    public static final String PRE_INTERCEPTOR_GROUP = PRE_BASE + SUFFIX_INTERCEPTORS;
    public static final String PRE_PROVIDER_GROUP = PRE_BASE + SUFFIX_PROVIDERS;
}

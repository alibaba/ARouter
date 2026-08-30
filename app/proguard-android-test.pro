# Instrumentation tests are compiled into a separate APK, so R8 cannot see
# their calls into the minified target APK. Keep only the public API surface
# exercised across that APK boundary and the dedicated result-test host.
-keep class com.alibaba.android.arouter.demo.ActivityResultHostActivity {
    *;
}

-keep class com.alibaba.android.arouter.facade.Postcard {
    public *;
}

-keep class com.alibaba.android.arouter.launcher.ARouter {
    public *;
}

-keep interface com.alibaba.android.arouter.facade.callback.NavigationCallback {
    *;
}

-keep interface com.alibaba.android.arouter.facade.callback.NavigationLauncher {
    *;
}

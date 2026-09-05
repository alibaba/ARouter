# Instrumentation tests are compiled into a separate APK, so R8 cannot see
# their calls into the minified target APK. Keep only the public API surface
# exercised across that APK boundary and the dedicated result-test host.
-keep class com.alibaba.android.arouter.demo.ActivityResultHostActivity {
    *;
}

-keep class com.alibaba.android.arouter.demo.module1.testactivity.ReorderProbeActivity {
    *;
}

-keep class com.alibaba.android.arouter.demo.module1.testinterceptor.LoginRedirectInterceptor {
    *;
}

-keep class com.alibaba.android.arouter.demo.kotlin.RecordingPretreatmentService {
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

# The target APK is minified separately from the instrumentation APK. Preserve
# the AndroidX factory invoked directly by the cross-APK public API test.
-keep class androidx.core.app.ActivityOptionsCompat {
    public static androidx.core.app.ActivityOptionsCompat makeCustomAnimation(android.content.Context, int, int);
}

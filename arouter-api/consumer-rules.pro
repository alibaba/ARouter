# The register plugin injects generated route-table class names as strings.
# Keep their names and public constructors for Class.forName/newInstance.
-keep,allowoptimization class com.alibaba.android.arouter.routes.** {
    public <init>();
}

# AutowiredServiceImpl resolves each generated injector from the target class name at runtime.
# Keep the names of classes that declare injected fields so R8 cannot break that relationship.
-keep,allowoptimization,allowshrinking class * {
    @com.alibaba.android.arouter.facade.annotation.Autowired <fields>;
}

# Generated injectors are loaded only through Class.forName(targetName + suffix).
-keep,allowoptimization class * implements com.alibaba.android.arouter.facade.template.ISyringe {
    public <init>();
    public void inject(java.lang.Object);
}

# Route metadata stores providers and interceptors as Class values, then creates them through
# public no-argument constructors at runtime. Their class names may still be obfuscated.
-keepclassmembers,allowoptimization,allowobfuscation class * implements com.alibaba.android.arouter.facade.template.IProvider {
    public <init>();
}
-keepclassmembers,allowoptimization,allowobfuscation class * implements com.alibaba.android.arouter.facade.template.IInterceptor {
    public <init>();
}

# Fragment routes use the same reflective public no-argument constructor contract.
# Older AndroidX releases do not supply their own Fragment constructor rules.
-keepclassmembers,allowoptimization,allowobfuscation class * extends androidx.fragment.app.Fragment {
    public <init>();
}
-keepclassmembers,allowoptimization,allowobfuscation class * extends android.app.Fragment {
    public <init>();
}

# by-type provider lookup uses the interface's canonical name as the generated map key.
-keep,allowoptimization interface * implements com.alibaba.android.arouter.facade.template.IProvider

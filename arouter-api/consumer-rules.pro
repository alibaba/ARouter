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

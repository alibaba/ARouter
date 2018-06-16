```
    A framework for assisting in the renovation of Android app componentization
```

[中文文档](https://raw.githubusercontent.com/alibaba/ARouter/master/README_CN.md)

##### [![Join the chat at https://gitter.im/alibaba/ARouter](https://badges.gitter.im/alibaba/ARouter.svg)](https://gitter.im/alibaba/ARouter?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

#### Lastest version

module|arouter-api|arouter-compiler|arouter-annotation|arouter-register
---|---|---|---|---
version|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-api/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-api/_latestVersion)|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-compiler/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-compiler/_latestVersion)|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-annotation/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-annotation/_latestVersion)|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-register/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-register/_latestVersion)

#### Demo

##### [Demo apk](https://github.com/alibaba/ARouter/blob/develop/demo/arouter-demo.apk)、[Demo Gif](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/arouter-demo.gif)

#### I. Feature
1. **Supports direct parsing of standard URLs for jumps and automatic injection of parameters into target pages**
2. **Support for multi-module**
3. **Support for interceptor**
4. **Support for dependency injection**
5. **InstantRun support**
6. **MultiDex support**
7. Mappings are grouped by group, multi-level management, on-demand initialization
8. Supports users to specify global demotion and local demotion strategies
9. Activity, interceptor and service can be automatically registered to the framework
10. Support multiple ways to configure transition animation
11. Support for fragement
12. Full kotlin support (Look at Other#2)

#### II. Classic Case
1. Forward from external URLs to internal pages, and parsing parameters
2. Jump and decoupling between multi-module
3. Intercept jump process, handle login, statistics and other logic
4. Cross-module communication, decouple components by IoC

#### III. Configuration
1. Adding dependencies and configurations
``` gradle
android {
    defaultConfig {
	...
	javaCompileOptions {
	    annotationProcessorOptions {
		arguments = [ moduleName : project.getName() ]
	    }
	}
    }
}

dependencies {
    // Replace with the latest version
    compile 'com.alibaba:arouter-api:?'
    annotationProcessor 'com.alibaba:arouter-compiler:?'
    ...
}
// Old version of gradle plugin (< 2.2), You can use apt plugin, look at 'Other#1'
// Kotlin configuration reference 'Other#2'
```

2. Add annotations
``` java
// Add annotations on pages that support routing (required)
// The path here needs to pay attention to need at least two levels : /xx/xx
@Route(path = "/test/activity")
public class YourActivity extend Activity {
    ...
}
```

3. Initialize the SDK
``` java
if (isDebug()) {           // These two lines must be written before init, otherwise these configurations will be invalid in the init process
    ARouter.openLog();     // Print log
    ARouter.openDebug();   // Turn on debugging mode (If you are running in InstantRun mode, you must turn on debug mode! Online version needs to be closed, otherwise there is a security risk)
}
ARouter.init(mApplication); // As early as possible, it is recommended to initialize in the Application
```

4. Initiate the routing
``` java
// 1. Simple jump within application (Jump via URL in 'Advanced usage')
ARouter.getInstance().build("/test/activity").navigation();

// 2. Jump with parameters
ARouter.getInstance().build("/test/1")
			.withLong("key1", 666L)
			.withString("key3", "888")
			.withObject("key4", new Test("Jack", "Rose"))
			.navigation();
```

5. Add confusing rules (If Proguard is turn on)
``` 
-keep public class com.alibaba.android.arouter.routes.**{*;}
-keep class * implements com.alibaba.android.arouter.facade.template.ISyringe{*;}

# If you use the byType method to obtain Service, add the following rules to protect the interface:
-keep interface * implements com.alibaba.android.arouter.facade.template.IProvider

# If single-type injection is used, that is, no interface is defined to implement IProvider, the following rules need to be added to protect the implementation
-keep class * implements com.alibaba.android.arouter.facade.template.IProvider

# If @Autowired is used for injection in non-Activity classes, add the following rules to prevent injection failures
-keepnames class * {
    @com.alibaba.android.arouter.facade.annotation.Autowired <fields>;
}
```

6. Using the custom gradle plugin to autoload the routing table
```gradle
apply plugin: 'com.alibaba.arouter'

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        // Replace with the latest version
        classpath "com.alibaba:arouter-register:?"
    }
}
```

Optional, use the registration plugin provided by the ARouter to automatically load the routing table. By default, the ARouter will scanned the dex files .
Performing an auto-registration via the gradle plugin can shorten the initialization time , it should be noted that the plugin must be used with api 1.3.0!

#### IV. Advanced usage
1. Jump via URL
``` java
// Create a new Activity for monitoring Schame events, and then directly pass url to ARouter
public class SchameFilterActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        ARouter.getInstance().build(uri).navigation();
        finish();
    }
}
```

AndroidManifest.xml
``` xml
<activity android:name=".activity.SchameFilterActivity">
	<!-- Schame -->
	<intent-filter>
	    <data
            android:host="m.aliyun.com"
            android:scheme="arouter"/>

	    <action android:name="android.intent.action.VIEW"/>

	    <category android:name="android.intent.category.DEFAULT"/>
	    <category android:name="android.intent.category.BROWSABLE"/>
	</intent-filter>
</activity>
```

2. Parse the parameters in the URL
``` java
// Declare a field for each parameter and annotate it with @Autowired
@Route(path = "/test/activity")
public class Test1Activity extends Activity {
    @Autowired
    public String name;
    @Autowired
    int age;
    @Autowired(name = "girl") // Map different parameters in the URL by name
    boolean boy;
    @Autowired
    TestObj obj;    // Support for parsing custom objects, using json pass in URL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ARouter.getInstance().inject(this);

        // ARouter will automatically set value of fields
        Log.d("param", name + age + boy);
    }
}

// If you need to pass a custom object, Need to implement the SerializationService, And use the @Route annotation annotation, E.g:
@Route(path = "/service/json")
public class JsonServiceImpl implements SerializationService {
    @Override
    public void init(Context context) {

    }

    @Override
    public <T> T json2Object(String text, Class<T> clazz) {
        return JSON.parseObject(text, clazz);
    }

    @Override
    public String object2Json(Object instance) {
        return JSON.toJSONString(instance);
    }
}
```

3. Declaration Interceptor (Intercept jump process, AOP)
``` java
// A more classic application is to handle login events during a jump so that there is no need to repeat the login check on the target page.
// Interceptors will be executed between jumps, multiple interceptors will be executed in order of priority
@Interceptor(priority = 8, name = "test interceptor")
public class TestInterceptor implements IInterceptor {
    @Override
    public void process(Postcard postcard, InterceptorCallback callback) {
        ...
        // No problem! hand over control to the framework
        callback.onContinue(postcard);  
        
        // Interrupt routing process
        // callback.onInterrupt(new RuntimeException("Something exception"));      

        // The above two types need to call at least one of them, otherwise it will not continue routing
    }

    @Override
    public void init(Context context) {
	    // Interceptor initialization, this method will be called when sdk is initialized, it will only be called once
    }
}
```

4. Processing jump results
``` java
// U can get the result of a single jump
ARouter.getInstance().build("/test/1").navigation(this, new NavigationCallback() {
    @Override
    public void onFound(Postcard postcard) {
      ...
    }

    @Override
    public void onLost(Postcard postcard) {
	...
    }
});
```

5. Custom global demotion strategy
``` java
// Implement the DegradeService interface
@Route(path = "/xxx/xxx")
public class DegradeServiceImpl implements DegradeService {
  @Override
  public void onLost(Context context, Postcard postcard) {
	// do something.
  }

  @Override
  public void init(Context context) {

  }
}
```

6. Decoupled by dependency injection : Service management -- Exposure services
``` java
// Declaration interface, other components get the service instance through the interface
public interface HelloService extends IProvider {
    String sayHello(String name);
}

@Route(path = "/service/hello", name = "test service")
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
	return "hello, " + name;
    }

    @Override
    public void init(Context context) {

    }
}
```

7. Decoupled by dependency injection : Service management -- Discovery service
``` java
public class Test {
    @Autowired
    HelloService helloService;

    @Autowired(name = "/service/hello")
    HelloService helloService2;

    HelloService helloService3;

    HelloService helloService4;

    public Test() {
	ARouter.getInstance().inject(this);
    }

    public void testService() {
        // 1. Use Dependency Injection to discover services, annotate fields with annotations
        helloService.sayHello("Vergil");
        helloService2.sayHello("Vergil");

        // 2. Discovering services using dependency lookup, the following two methods are byName and byType
        helloService3 = ARouter.getInstance().navigation(HelloService.class);
        helloService4 = (HelloService) ARouter.getInstance().build("/service/hello").navigation();
        helloService3.sayHello("Vergil");
        helloService4.sayHello("Vergil");
    }
}
```

#### V. More features

1. Other settings in initialization
``` java
ARouter.openLog(); // Open log
ARouter.openDebug(); // When using InstantRun, you need to open this switch and turn it off after going online. Otherwise, there is a security risk.
ARouter.printStackTrace(); // Print thread stack when printing logs
```

2. API description
``` java
// Build a standard route request
ARouter.getInstance().build("/home/main").navigation();

// Build a standard route request, via URI
Uri uri;
ARouter.getInstance().build(uri).navigation();

// Build a standard route request, startActivityForResult
// The first parameter must be Activity and the second parameter is RequestCode
ARouter.getInstance().build("/home/main", "ap").navigation(this, 5);

// Pass Bundle directly
Bundle params = new Bundle();
ARouter.getInstance()
	.build("/home/main")
	.with(params)
	.navigation();

// Set Flag
ARouter.getInstance()
	.build("/home/main")
	.withFlags();
	.navigation();

// For fragment
Fragment fragment = (Fragment) ARouter.getInstance().build("/test/fragment").navigation();
					
// transfer the object 
ARouter.getInstance()
	.withObject("key", new TestObj("Jack", "Rose"))
	.navigation();

// Think the interface is not enough, you can directly set parameter into Bundle
ARouter.getInstance()
	    .build("/home/main")
	    .getExtra();

// Transition animation (regular mode)
ARouter.getInstance()
    .build("/test/activity2")
    .withTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
    .navigation(this);

// Transition animation (API16+)
ActivityOptionsCompat compat = ActivityOptionsCompat.
    makeScaleUpAnimation(v, v.getWidth() / 2, v.getHeight() / 2, 0, 0);

// ps. makeSceneTransitionAnimation, When using shared elements, you need to pass in the current Activity in the navigation method

ARouter.getInstance()
	.build("/test/activity2")
	.withOptionsCompat(compat)
	.navigation();
        
// Use green channel (skip all interceptors)
ARouter.getInstance().build("/home/main").greenChannel().navigation();

// Use your own log tool to print logs
ARouter.setLogger();
```

3. Get the original URI
``` java
String uriStr = getIntent().getStringExtra(ARouter.RAW_URI);
```

4. Rewrite URL
``` java
// Implement the PathReplaceService interface
@Route(path = "/xxx/xxx")
public class PathReplaceServiceImpl implements PathReplaceService {
    /**
     * For normal path.
     *
     * @param path raw path
     */
    String forString(String path) {
        // Custom logic
    	return path;
    }

   /**
    * For uri type.
    *
    * @param uri raw uri
    */
    Uri forUri(Uri uri) {
        // Custom logic
    	return url;
    }
}
```

#### VI. Other

1. Old version of gradle plugin configuration
``` gradle
apply plugin: 'com.neenbedankt.android-apt'

buildscript {
    repositories {
    	jcenter()
    }

    dependencies {
    	classpath 'com.neenbedankt.gradle.plugins:android-apt:1.4'
    }
}

apt {
    arguments {
    	moduleName project.getName();
    }
}

dependencies {
    compile 'com.alibaba:arouter-api:x.x.x'
    apt 'com.alibaba:arouter-compiler:x.x.x'
    ...
}
```

2. Kotlin project configuration
```
// You can refer to the wording in the "module-kotlin" module
apply plugin: 'kotlin-kapt'

kapt {
    arguments {
        arg("moduleName", project.getName())
    }
}

dependencies {
    compile 'com.alibaba:arouter-api:x.x.x'
    kapt 'com.alibaba:arouter-compiler:x.x.x'
    ...
}
```

#### VII. Communication

1. Communication

    1. group1 (Full)
    
        ![qq](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/arouter-qq-addr.png)

    2. group2
        
        ![qq](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/qq-qrcode-2.JPG)

#### VIII. Contributions

1. [imknown](https://github.com/alibaba/ARouter/commits?author=imknown)

2. [crazy1235](https://github.com/crazy1235)

3. [luckybilly](https://github.com/luckybilly)

4. [LinXiaoTao](https://github.com/LinXiaoTao)

5. [tanglie1993](https://github.com/tanglie1993)
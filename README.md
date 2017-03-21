## English version is being re-translated, coming soon...

```
    Android平台中对页面、服务提供路由功能的中间件，我的目标是 —— 简单且够用。
```

##### [![Join the chat at https://gitter.im/alibaba/ARouter](https://badges.gitter.im/alibaba/ARouter.svg)](https://gitter.im/alibaba/ARouter?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)

---

#### 最新版本

模块|arouter-api|arouter-compiler|arouter-annotation
---|---|---|---
最新版本|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-api/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-api/_latestVersion)|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-compiler/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-compiler/_latestVersion)|[![Download](https://api.bintray.com/packages/zhi1ong/maven/arouter-annotation/images/download.svg)](https://bintray.com/zhi1ong/maven/arouter-annotation/_latestVersion)

#### Demo展示

##### [Demo apk下载](http://public.cdn.zhilong.me/app-debug.apk)、[Demo Gif](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/arouter-demo.gif)

#### 一、功能介绍
1. **支持直接解析标准URL进行跳转，并自动注入参数到目标页面中**
2. **支持多模块工程使用**
3. **支持添加多个拦截器，自定义拦截顺序**
4. **支持依赖注入，可单独作为依赖注入框架使用**
5. **支持InstantRun**
6. **支持MultiDex**(Google方案)
7. 映射关系按组分类、多级管理，按需初始化
8. 支持用户指定全局降级与局部降级策略
9. 页面、拦截器、服务等组件均自动注册到框架
10. 支持多种方式配置转场动画

#### 二、典型应用
1. 从外部URL映射到内部页面，以及参数传递与解析
2. 跨模块页面跳转，模块间解耦
3. 拦截跳转过程，处理登陆、埋点等逻辑
4. 跨模块API调用，通过控制反转来做组件解耦

#### 三、基础功能
1. 添加依赖和配置

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
            // 替换成最新版本, 需要注意的是api
            // 要与compiler匹配使用，均使用最新版可以保证兼容
            compile 'com.alibaba:arouter-api:x.x.x'
            annotationProcessor 'com.alibaba:arouter-compiler:x.x.x'
            ...
        }
        // 旧版本gradle插件(< 2.2)，可以使用apt插件，配置方法见文末'其他#4'

2. 添加注解

		// 在支持路由的页面上添加注解(必选)
		// 这里的路径需要注意的是至少需要有两级，/xx/xx
		@Route(path = "/test/activity")
		public class YourActivity extend Activity {
		    ...
		}

3. 初始化SDK

        if (isDebug()) {
            ARouter.openLog();     // 打印日志
            ARouter.openDebug();   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        ARouter.init(mApplication); // 尽可能早，推荐在Application中初始化

4. 发起路由操作

		// 1. 应用内简单的跳转(通过URL跳转在'进阶用法'中)
		ARouter.getInstance().build("/test/activity").navigation();

		// 2. 跳转并携带参数
		ARouter.getInstance().build("/test/1")
					.withLong("key1", 666L)
					.withString("key3", "888")
					.withObject("key4", new Test("Jack", "Rose"))
					.navigation();

5. 添加混淆规则(如果使用了Proguard)

        -keep public class com.alibaba.android.arouter.routes.**{*;}
        -keep class * implements com.alibaba.android.arouter.facade.template.ISyringe{*;}

#### 四、进阶用法
1. 通过URL跳转

        // 新建一个Activity用于监听Schame事件,之后直接把url传递给ARouter即可
        public class SchameFilterActivity extends Activity {
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                Uri uri = getIntent().getData();
                ARouter.getInstance().build(uri).navigation();
                finish();
            }
        }

        // AndroidManifest.xml
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

2. 解析URL中的参数

        // 为每一个参数声明一个字段，并使用 @Autowired 标注
        // URL中不能传递Parcelable类型数据，通过ARouter api可以传递Parcelable对象
        @Route(path = "/test/activity")
        public class Test1Activity extends Activity {
            @Autowired
            public String name;
            @Autowired
            int age;
            @Autowired(name = "girl") // 通过name来映射URL中的不同参数
            boolean boy;
            @Autowired
            TestObj obj;    // 支持解析自定义对象，URL中使用json传递

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                ARouter.getInstance().inject(this);

                // ARouter会自动对字段进行赋值，无需主动获取
                Log.d("param", name + age + boy);
            }
        }

3. 声明拦截器(拦截跳转过程，面向切面编程)

        // 比较经典的应用就是在跳转过程中处理登陆事件，这样就不需要在目标页重复做登陆检查
        // 拦截器会在跳转之间执行，多个拦截器会按优先级顺序依次执行
        @Interceptor(priority = 8, name = "测试用拦截器")
        public class TestInterceptor implements IInterceptor {
            @Override
            public void process(Postcard postcard, InterceptorCallback callback) {
                ...
                callback.onContinue(postcard);  // 处理完成，交还控制权
                // callback.onInterrupt(new RuntimeException("我觉得有点异常"));      // 觉得有问题，中断路由流程

                // 以上两种至少需要调用其中一种，否则不会继续路由
            }

            @Override
            public void init(Context context) {
            	// 拦截器的初始化，会在sdk初始化的时候调用该方法，仅会调用一次
            }
        }

4. 处理跳转结果

		// 使用两个参数的navigation方法，可以获取单次跳转的结果
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

5. 自定义全局降级策略

			// 实现DegradeService接口，并加上一个Path内容任意的注解即可
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

6. 为目标页面声明更多信息

		// 我们经常需要在目标页面中配置一些属性，比方说"是否需要登陆"之类的
		// 可以通过 Route 注解中的 extras 属性进行扩展，这个属性是一个 int值，换句话说，单个int有4字节，也就是32位，可以配置32个开关
		// 剩下的可以自行发挥，通过字节操作可以标识32个开关，通过开关标记目标页面的一些属性，在拦截器中可以拿到这个标记进行业务逻辑判断
		@Route(path = "/test/activity", extras = Consts.XXXX)

7. 通过依赖注入解耦:服务管理(一) 暴露服务

        // 声明接口,其他组件通过接口来调用服务
        public interface HelloService extends IProvider {
            String sayHello(String name);
        }

        // 实现接口
        @Route(path = "/service/hello", name = "测试服务")
        public class HelloServiceImpl implements HelloService {

            @Override
            public String sayHello(String name) {
                return "hello, " + name;
            }

            @Override
            public void init(Context context) {

            }
        }

9. 通过依赖注入解耦:服务管理(二) 发现服务


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
		    	 // 1. (推荐)使用依赖注入的方式发现服务,通过注解标注字段,即可使用，无需主动获取
		    	 // Autowired注解中标注name之后，将会使用byName的方式注入对应的字段，不设置name属性，会默认使用byType的方式发现服务(当同一接口有多个实现的时候，必须使用byName的方式发现服务)
		        helloService.sayHello("Vergil");
		        helloService2.sayHello("Vergil");

		        // 2. 使用依赖查找的方式发现服务，主动去发现服务并使用，下面两种方式分别是byName和byType
		        helloService3 = ARouter.getInstance().navigation(HelloService.class);
		        helloService4 = (HelloService) ARouter.getInstance().build("/service/hello").navigation();
		        helloService3.sayHello("Vergil");
		        helloService4.sayHello("Vergil");
		    }
		}

#### 五、更多功能

1. 初始化中的其他设置

		ARouter.openLog();		// 开启日志
		ARouter.openDebug();	// 使用InstantRun的时候，需要打开该开关，上线之后关闭，否则有安全风险
		ARouter.printStackTrace(); // 打印日志的时候打印线程堆栈

2. 详细的API说明

		// 构建标准的路由请求
		ARouter.getInstance().build("/home/main").navigation();

		// 构建标准的路由请求，并指定分组
		ARouter.getInstance().build("/home/main", "ap").navigation();

		// 构建标准的路由请求，通过Uri直接解析
		Uri uri;
		ARouter.getInstance().build(uri).navigation();

		// 构建标准的路由请求，startActivityForResult
		// navigation的第一个参数必须是Activity，第二个参数则是RequestCode
		ARouter.getInstance().build("/home/main", "ap").navigation(this, 5);

		// 直接传递Bundle
		Bundle params = new Bundle();
		ARouter.getInstance()
					.build("/home/main")
					.with(params)
					.navigation();

		// 指定Flag
		ARouter.getInstance()
					.build("/home/main")
					.withFlags();
					.navigation();
					
	    // 对象传递
	    ARouter.getInstance()
	                .withObject("key", new TestObj("Jack", "Rose"))
	                .navigation();

		// 觉得接口不够多，可以直接拿出Bundle赋值
		ARouter.getInstance()
		            .build("/home/main")
		            .getExtra();

        // 转场动画(常规方式)
        ARouter.getInstance()
            .build("/test/activity2")
            .withTransition(R.anim.slide_in_bottom, R.anim.slide_out_bottom)
            .navigation(this);
        
        // 转场动画(API16+)
        ActivityOptionsCompat compat = ActivityOptionsCompat.
            makeScaleUpAnimation(v, v.getWidth() / 2, v.getHeight() / 2, 0, 0);

        ARouter.getInstance()
                .build("/test/activity2")
                .withOptionsCompat(compat)
                .navigation();
        
	    // 使用绿色通道(跳过所有的拦截器)
	    ARouter.getInstance().build("/home/main").greenChannel().navigation();

	    // 使用自己的日志工具打印日志
	    ARouter.setLogger();

3. 获取原始的URI

        String uriStr = getIntent().getStringExtra(ARouter.RAW_URI);

4. 重写跳转URL

			// 实现PathReplaceService接口，并加上一个Path内容任意的注解即可
        @Route(path = "/xxx/xxx") // 必须标明注解
        public class PathReplaceServiceImpl implements DegradeService {
            /**
             * For normal path.
             *
             * @param path raw path
             */
            String forString(String path) {
                return path;    // 按照一定的规则处理之后返回处理后的结果
            }

            /**
             * For uri type.
             *
             * @param uri raw uri
            */
            Uri forUri(Uri uri) {
                return url;    // 按照一定的规则处理之后返回处理后的结果
            }
         }

#### 六、其他

1. 路由中的分组概念

	- SDK中针对所有的路径(/test/1 /test/2)进行分组，分组只有在分组中的某一个路径第一次被访问的时候，该分组才会被初始化
	- 可以通过 @Route 注解主动指定分组，否则使用路径中第一段字符串(/*/)作为分组
	- 注意：一旦主动指定分组之后，应用内路由需要使用 ARouter.getInstance().build(path, group) 进行跳转，手动指定分组，否则无法找到

	        @Route(path = "/test/1", group = "app")

2. 拦截器和服务的异同

	- 拦截器和服务所需要实现的接口不同，但是结构类似，都存在 init(Context context) 方法，但是两者的调用时机不同
	- 拦截器因为其特殊性，会被任何一次路由所触发，拦截器会在ARouter初始化的时候异步初始化，如果第一次路由的时候拦截器还没有初始化结束，路由会等待，直到初始化完成。
	- 服务没有该限制，某一服务可能在App整个生命周期中都不会用到，所以服务只有被调用的时候才会触发初始化操作

3. Jack 编译链的支持

	- ~~因为不想让用户主动设置一堆乱七八糟的参数，在获取模块名的时候使用javac的api，使用了Jack之后没有了javac，只能让用户稍稍动动手了~~
	- 现在任何情况下都需要在build.gradle中配置moduleName了。。。。

4. 旧版本gradle插件的配置方式

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

#### 七、Q&A

1. "W/ARouter::: ARouter::No postcard![ ]"

    这个Log正常的情况下也会打印出来，如果您的代码中没有实现DegradeService和PathReplaceService的话，因为ARouter本身的一些功能也依赖
    自己提供的Service管理功能，ARouter在跳转的时候会尝试寻找用户实现的PathReplaceService，用于对路径进行重写(可选功能)，所以如果您没有
    实现这个服务的话，也会抛出这个日志

    推荐在app中实现DegradeService、PathReplaceService

2. "W/ARouter::: ARouter::There is no route match the path [/xxx/xxx], in group [xxx][ ]"

    - 通常来说这种情况是没有找到目标页面，目标不存在
    - 如果这个页面是存在的，那么您可以按照下面的步骤进行排查
        1. 检查目标页面的注解是否配置正确，正确的注解形式应该是 (@Route(path="/test/test"), 如没有特殊需求，请勿指定group字段，废弃功能)
        2. 检查目标页面所在的模块的gradle脚本中是否依赖了 arouter-compiler sdk (需要注意的是，要使用apt依赖，而不是compile关键字依赖)
        3. 检查编译打包日志，是否出现了形如 ARouter::Compiler >>> xxxxx 的日志，日志中会打印出发现的路由目标
        4. 启动App的时候，开启debug、log(openDebug/openLog), 查看映射表是否已经被扫描出来，形如 D/ARouter::: LogisticsCenter has already been loaded, GroupIndex[4]，GroupIndex > 0

3. 沟通和交流

    ![qq](https://raw.githubusercontent.com/alibaba/ARouter/master/demo/arouter-qq-addr.png)
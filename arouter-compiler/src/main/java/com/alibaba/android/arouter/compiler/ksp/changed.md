## 关于 KSP
加速kotlin文件的注解处理速度， 用来替换kapt， 可参考官方文档：[KSP](https://kotlinlang.org/docs/ksp-overview.html#symbolprocessorprovider-the-entry-point)

> Kotlin Symbol Processing (*KSP*) is an API that you can use to develop lightweight compiler plugins. KSP provides a simplified compiler plugin API that leverages the power of Kotlin while keeping the learning curve at a minimum. Compared to [kapt](https://kotlinlang.org/docs/kapt.html), annotation processors that use KSP can run up to 2 times faster.
>
> To run Java annotation processors unmodified, kapt compiles Kotlin code into Java stubs that retain information that Java annotation processors care about. To create these stubs, kapt needs to resolve all symbols in the Kotlin program. The stub generation costs roughly 1/3 of a full `kotlinc` analysis and the same order of `kotlinc` code-generation. For many annotation processors, this is much longer than the time spent in the processors themselves. For example, Glide looks at a very limited number of classes with a predefined annotation, and its code generation is fairly quick. Almost all of the build overhead resides in the stub generation phase. Switching to KSP would immediately reduce the time spent in the compiler by 25%.



## 主要变动



### 1、 升级kotlin版本为1.6.10， 更好的直接支持[kotlin-poet](https://github.com/square/kotlinpoet)和 [ksp](https://github.com/google/ksp) 插件版本, 后续升级kotlin版本时应该参考上述2个插件的kotlin匹配版本号



### 2、 修改部分compileOptions为JAVA_1_8
```groovy
	compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_7
        targetCompatibility JavaVersion.VERSION_1_7
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
```


### 3、 gradle配置中增加三个选项分别支持ksp增编编译、ksp日志、[track classpath](https://github.com/google/ksp/issues/511)三个功能

```groovy
ksp.incremental=true
ksp.incremental.log=true
# track classpath
ksp.incremental.intermodule=true
```


### 4、 版本号变更, annotation、compiler、api 分别变更为1.0.7、1.5.3、1.5.3 

注意，未正式合并前， 在其他项目中测试时需要使用 Arouter提供的installLocally打出本地aar才能使用



### 5、 annotation模块的RouteMeta类增加 kspRawType成员，存储ksp的KSClassDeclaration (对标AbstractProcessor中使用的`Element rawType;`) 

```groovy
public class RouteMeta {
    private RouteType type;         // Type of route
    private Element rawType;        // Raw type of route
    private Object kspRawType;      // Raw type of ksp : KSClassDeclaration
    private Class<?> destination;   // Destination
```
因此， 要使用ksp, 必须使用1.0.7版本的annotation， 此外， 为避免在annotation中将ksp的包导入， 这里使用 Object类型， 实际使用时强转类型为KSClassDeclaration



### 6、 添加`com.alibaba.android.arouter.compiler.ksp`模块， 分别实现 `@Route @Autowired @Interceptor`



### 7、 ksp导入`@AutoService`有点麻烦， 为避免和AbstractProcessor中的`@AutoService`版本号等冲突， 直接弃用AbstractProcessor的AutoSrvice， 直接使用`resources\META-INF\services` 导入插件

AbstractProcessor无其他变化， 因此在纯Java项目直接使用老方法导入即可（不需要使用ksp）



### 8 、 关于测试

1、 官方demo， 对比其代码生成，生成代码无误， 可以直接运行（module-java）
2、 纯java项目 [https://github.com/JailedBird/WanAndroid](https://github.com/JailedBird/WanAndroid)   对比其代码生成，生成代码无误， 可以直接运行 (注：开源项目，Arouter使用不是很深入，测试覆盖率过小)
3、 纯kotlin项目[https://github.com/JailedBird/DevComponent](https://github.com/JailedBird/DevComponent)  对比其代码生成，生成代码无误， 可以直接运行 (注：开源项目，Arouter使用不是很深入，测试覆盖率过小)



### 9、 关于算法， 完全参照AbstractProcessor的解析、生成算法实现注解处理器



### 10、 关于增量编译， @Route @Interceptor 采用aggregating的规则生成， @Autowired 根据其中是否存在Provider区分是aggregating还是isolating模式（无Provider时可以根据自身的AST生成代码， 存在Provider时需要根据外部Provider区分代码生成规则）

关于ksp的增编编译处理可以参考： [https://kotlinlang.org/docs/ksp-incremental.html#symbolprocessorprovider-the-entry-point](https://kotlinlang.org/docs/ksp-incremental.html#symbolprocessorprovider-the-entry-point)



### 11、 参考项目和文献

-  [https://kotlinlang.org/docs/ksp-overview.html#symbolprocessorprovider-the-entry-point](https://kotlinlang.org/docs/ksp-overview.html#symbolprocessorprovider-the-entry-point)
- [https://github.com/google/ksp](https://github.com/google/ksp)
- [https://github.com/square/moshi](https://github.com/square/moshi)
- [https://github.com/adrielcafe/lyricist](https://github.com/adrielcafe/lyricist)



## 接入方式 (参考module-java)



### 1、 root build.gradle

根 build.gradle 配置 ksp插件版本号， 最好匹配kotlin版本号， 避免兼容问题， 关于ksp兼容问题可参考：[https://kotlinlang.org/docs/ksp-faq.html#besides-kotlin-are-there-other-version-requirements-to-libraries](https://kotlinlang.org/docs/ksp-faq.html#besides-kotlin-are-there-other-version-requirements-to-libraries)

```groovy
plugins {
    id 'com.google.devtools.ksp' version '1.6.10-1.0.4' apply false
}
```



### 2、 module build.gradle

在模块中使用1的ksp插件， 并用ksp指定注解处理器, 添加ksp注释.... 

```groovy
apply plugin: 'kotlin-android'
apply plugin: 'com.google.devtools.ksp'

ksp {
    arg("AROUTER_MODULE_NAME", project.getName())
    arg("AROUTER_GENERATE_DOC", "enable")
}

dependencies {
    // annotationProcessor project(':arouter-compiler')
    // ksp 'com.alibaba:arouter-compiler:1.5.3'
    ksp project(':arouter-compiler')
}
```
注意， 纯java中直接使用annotationProcessor导入AbstractProcessor即可， 但是如果现在纯java中导入ksp， 应该是需要导入'kotlin-android', 否则貌似是不会生成代码的



### 3、 output

1、 日志默认使用KSPLogger.info 打印， 默认不会输出到控制台， 可以使用 `--info`开启（如：`:app:assembleDebug -- info`）， 或者测试阶段使用KSPLogger.warn输出， 关于这点参考：[https://github.com/google/ksp/issues/1111](https://github.com/google/ksp/issues/1111)

2、 生成代码：模块内的`build\generated\ksp` 

3、 开启doc配置还可得到`arouter-map-of-xxx.json`观察生成的内容

4、 gradle.property开启增量编译和编译日志，观测增量编译、脏文件等，关于增量编译请参考：[https://kotlinlang.org/docs/ksp-incremental.html](https://kotlinlang.org/docs/ksp-incremental.html)

```groovy
# KSP Incremental processing
# https://kotlinlang.org/docs/ksp-incremental.html#program-elements
ksp.incremental=true
ksp.incremental.log=true
# track classpath  https://github.com/google/ksp/issues/511
ksp.incremental.intermodule=true
```
开启日志可得到：`build\kspCaches\debug\logs`， kspDirtySet.log： 增量编译脏文件集合

```groovy
=== Build 1666430430393 ===
All Files
  src\main\java\com\alibaba\android\arouter\demo\module1\BlankFragment.java
  src\main\java\com\alibaba\android\arouter\demo\module1\MainLooper.java
  ......
  src\main\java\com\alibaba\android\arouter\demo\module1\testservice\HelloServiceImpl.java
  src\main\java\com\alibaba\android\arouter\demo\module1\testservice\JsonServiceImpl.java
  src\main\java\com\alibaba\android\arouter\demo\module1\testservice\SingleService.java
  src\main\java\com\alibaba\android\arouter\demo\module1\TestWebview.java
Modified
  build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\demo\module1\BlankFragment$$ARouter$$Autowired.kt
  build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\demo\module1\testactivity\BaseActivity$$ARouter$$Autowired.kt
  build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\demo\module1\testactivity\Test1Activity$$ARouter$$Autowired.kt
  ......
Removed
Disappeared Outputs
Affected By CP
Affected By new syms
Affected By sealed
CP changes
Dirty:
  src\main\java\com\alibaba\android\arouter\demo\module1\BlankFragment.java
  src\main\java\com\alibaba\android\arouter\demo\module1\testactivity\BaseActivity.java
  ......
  src\main\java\com\alibaba\android\arouter\demo\module1\testservice\JsonServiceImpl.java
  src\main\java\com\alibaba\android\arouter\demo\module1\testservice\SingleService.java
  src\main\java\com\alibaba\android\arouter\demo\module1\TestWebview.java

Dirty / All: 87.50%


```

kspSourceToOutputs.log：和kspDirtySet对应， 查看 ksp特有的输入和输出依赖关系， 以此推断代码增量编译依赖关系、是否有效  
```groovy
=== Build 1666430430393 ===
Accumulated source to outputs map
  src\main\java\com\alibaba\android\arouter\demo\module1\TestModule2Activity.java:
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\routes\ARouter$$Root$$modulejava.kt
  ......
  src\main\java\com\alibaba\android\arouter\demo\module1\testactivity\Test1Activity.java:
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\demo\module1\testactivity\Test1Activity$$ARouter$$Autowired.kt
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\routes\ARouter$$Group$$test.kt
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\routes\ARouter$$Root$$modulejava.kt
    build\generated\ksp\debug\resources\com\alibaba\android\arouter\docs\arouter-map-of-modulejava.json

Reprocessed sources and their outputs
  src\main\java\com\alibaba\android\arouter\demo\module1\testactivity\Test1Activity.java:
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\routes\ARouter$$Group$$test.kt
    build\generated\ksp\debug\resources\com\alibaba\android\arouter\docs\arouter-map-of-modulejava.json
  ......
  src\main\java\com\alibaba\android\arouter\demo\module1\testinterceptor\Test1Interceptor.java:
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\routes\ARouter$$Interceptors$$modulejava.kt
  src\main\java\com\alibaba\android\arouter\demo\module1\TestInterceptor90.java:
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\routes\ARouter$$Interceptors$$modulejava.kt
  src\main\java\com\alibaba\android\arouter\demo\module1\testactivity\BaseActivity.java:
    build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\demo\module1\testactivity\BaseActivity$$ARouter$$Autowired.kt

All reprocessed outputs
  build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\routes\ARouter$$Group$$test.kt
  ......
  build\generated\ksp\debug\kotlin\com\alibaba\android\arouter\demo\module1\testactivity\Test3Activity$$ARouter$$Autowired.kt


```


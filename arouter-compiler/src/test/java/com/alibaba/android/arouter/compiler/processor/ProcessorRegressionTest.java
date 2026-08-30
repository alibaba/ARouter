package com.alibaba.android.arouter.compiler.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaFileObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProcessorRegressionTest {

    @Test
    public void routeProcessorRejectsDuplicatePaths() {
        List<JavaFileObject> sources = commonSources();
        sources.add(JavaFileObjects.forSourceLines(
                "test.FirstActivity",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Route;",
                "@Route(path = \"/duplicate/page\")",
                "public class FirstActivity extends android.app.Activity {}"
        ));
        sources.add(JavaFileObjects.forSourceLines(
                "test.SecondActivity",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Route;",
                "@Route(path = \"/duplicate/page\")",
                "public class SecondActivity extends android.app.Activity {}"
        ));

        Compilation compilation = Compiler.javac()
                .withOptions("-AAROUTER_MODULE_NAME=test")
                .withProcessors(new RouteProcessor())
                .compile(sources);

        assertEquals(compilation.toString(), Compilation.Status.FAILURE, compilation.status());
        assertTrue(compilation.errors().toString(), compilation.errors().toString()
                .contains("Duplicate route path [/duplicate/page]"));
    }

    @Test
    public void routeProcessorRejectsDuplicateProviderPathsRegardlessOfPriority() {
        List<JavaFileObject> sources = commonSources();
        sources.add(JavaFileObjects.forSourceLines(
                "test.DuplicateService",
                "package test;",
                "public interface DuplicateService extends "
                        + "com.alibaba.android.arouter.facade.template.IProvider {}"
        ));
        sources.add(JavaFileObjects.forSourceLines(
                "test.FirstProvider",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Route;",
                "@Route(path = \"/duplicate/provider\", priority = 1)",
                "public class FirstProvider implements DuplicateService {}"
        ));
        sources.add(JavaFileObjects.forSourceLines(
                "test.SecondProvider",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Route;",
                "@Route(path = \"/duplicate/provider\", priority = 2)",
                "public class SecondProvider implements DuplicateService {}"
        ));

        Compilation compilation = Compiler.javac()
                .withOptions("-AAROUTER_MODULE_NAME=test")
                .withProcessors(new RouteProcessor())
                .compile(sources);

        assertEquals(compilation.toString(), Compilation.Status.FAILURE, compilation.status());
        String errors = compilation.errors().toString();
        assertTrue(errors, errors.contains("Duplicate route path [/duplicate/provider]"));
        assertTrue(errors, errors.contains(
                "Route priority is metadata and does not select between duplicate paths"));
    }

    @Test
    public void routeProcessorSupportsAndroidXWithoutLegacySupportFragment() throws Exception {
        List<JavaFileObject> sources = commonSources();
        sources.add(JavaFileObjects.forSourceLines(
                "test.AndroidXRoute",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Route;",
                "@Route(path = \"/test/androidx\")",
                "public class AndroidXRoute extends androidx.fragment.app.Fragment {}"
        ));

        Compilation compilation = Compiler.javac()
                .withOptions("-AAROUTER_MODULE_NAME=test")
                .withProcessors(new RouteProcessor())
                .compile(sources);

        assertEquals(compilation.toString(), Compilation.Status.SUCCESS, compilation.status());
        assertTrue(generatedSource(compilation, "ARouter$$Group$$test.java")
                .contains("RouteType.FRAGMENT"));
    }

    @Test
    public void autowiredProcessorGuardsMissingContainersAndNullableValues() throws Exception {
        List<JavaFileObject> sources = commonSources();
        sources.add(JavaFileObjects.forSourceLines(
                "test.InjectionTarget",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Autowired;",
                "public class InjectionTarget extends android.app.Activity {",
                "  @Autowired public int count = 7;",
                "  @Autowired public Integer nullableCount = null;",
                "  @Autowired public String title = \"default\";",
                "  @Autowired public Payload payload = new Payload();",
                "}",
                "class Payload {}"
        ));
        sources.add(JavaFileObjects.forSourceLines(
                "test.AndroidXInjectionTarget",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Autowired;",
                "public class AndroidXInjectionTarget extends androidx.fragment.app.Fragment {",
                "  @Autowired public String title = \"default\";",
                "}"
        ));

        Compilation compilation = Compiler.javac()
                .withOptions("-AAROUTER_MODULE_NAME=test")
                .withProcessors(new AutowiredProcessor())
                .compile(sources);

        assertEquals(compilation.toString(), Compilation.Status.SUCCESS, compilation.status());

        String activityHelper = generatedSource(compilation, "InjectionTarget$$ARouter$$Autowired.java");
        assertTrue(activityHelper.contains(
                "Bundle bundle = substitute.getIntent() == null ? null : substitute.getIntent().getExtras();"));
        assertTrue(activityHelper.contains("if (null != bundle && bundle.containsKey(\"count\"))"));
        assertTrue(activityHelper.contains("bundle.getInt(\"count\", substitute.count)"));
        assertTrue(activityHelper.contains("substitute.nullableCount = (Integer) bundle.get(\"nullableCount\")"));
        assertTrue(activityHelper.contains("Payload payloadValue ="));
        assertTrue(activityHelper.contains("if (null != payloadValue)"));

        String fragmentHelper = generatedSource(compilation, "AndroidXInjectionTarget$$ARouter$$Autowired.java");
        assertTrue(fragmentHelper.contains("Bundle bundle = substitute.getArguments();"));
        assertTrue(fragmentHelper.contains("if (null != bundle && bundle.containsKey(\"title\"))"));
    }

    @Test
    public void interceptorProcessorUsesSemanticInterfaceComparison() throws Exception {
        List<JavaFileObject> sources = commonSources();
        sources.add(JavaFileObjects.forSourceLines(
                "test.DirectInterceptor",
                "package test;",
                "import com.alibaba.android.arouter.facade.annotation.Interceptor;",
                "import com.alibaba.android.arouter.facade.template.IInterceptor;",
                "@Interceptor(priority = 1)",
                "public final class DirectInterceptor implements IInterceptor {}"
        ));

        Compilation compilation = Compiler.javac()
                .withOptions("-AAROUTER_MODULE_NAME=test")
                .withProcessors(new InterceptorProcessor())
                .compile(sources);

        assertEquals(compilation.toString(), Compilation.Status.SUCCESS, compilation.status());
        assertTrue(generatedSource(compilation, "ARouter$$Interceptors$$test.java")
                .contains("DirectInterceptor.class"));
    }

    private static String generatedSource(Compilation compilation, String fileName) throws IOException {
        for (JavaFileObject source : compilation.generatedSourceFiles()) {
            if (source.getName().endsWith(fileName)) {
                return source.getCharContent(false).toString();
            }
        }
        throw new AssertionError("Generated source not found: " + fileName + "\n" + compilation);
    }

    private static List<JavaFileObject> commonSources() {
        return new ArrayList<>(Arrays.asList(
                source("android.content.Context", "public class Context {}"),
                source("android.content.Intent",
                        "public class Intent { public android.os.Bundle getExtras() { return null; } }"),
                source("android.os.Parcelable", "public interface Parcelable {}"),
                source("android.os.Bundle",
                        "public class Bundle {",
                        "  public boolean containsKey(String key) { return false; }",
                        "  public Object get(String key) { return null; }",
                        "  public String getString(String key) { return null; }",
                        "  public boolean getBoolean(String key, boolean value) { return value; }",
                        "  public byte getByte(String key, byte value) { return value; }",
                        "  public short getShort(String key, short value) { return value; }",
                        "  public int getInt(String key, int value) { return value; }",
                        "  public long getLong(String key, long value) { return value; }",
                        "  public char getChar(String key, char value) { return value; }",
                        "  public float getFloat(String key, float value) { return value; }",
                        "  public double getDouble(String key, double value) { return value; }",
                        "}"),
                source("android.app.Activity",
                        "public class Activity extends android.content.Context {",
                        "  public android.content.Intent getIntent() { return null; }",
                        "}"),
                source("android.app.Service", "public class Service extends android.content.Context {}"),
                source("android.app.Fragment",
                        "public class Fragment { public android.os.Bundle getArguments() { return null; } }"),
                source("androidx.fragment.app.Fragment",
                        "public class Fragment { public android.os.Bundle getArguments() { return null; } }"),
                source("android.util.Log", "public class Log { public static int e(String tag, String message) { return 0; } }"),
                source("com.alibaba.android.arouter.facade.template.IProvider", "public interface IProvider {}"),
                source("com.alibaba.android.arouter.facade.template.IInterceptor", "public interface IInterceptor {}"),
                source("com.alibaba.android.arouter.facade.template.IInterceptorGroup",
                        "public interface IInterceptorGroup {",
                        "  void loadInto(java.util.Map<Integer, Class<? extends IInterceptor>> interceptors);",
                        "}"),
                source("com.alibaba.android.arouter.facade.template.ISyringe",
                        "public interface ISyringe { void inject(Object target); }"),
                source("com.alibaba.android.arouter.facade.template.IRouteGroup",
                        "public interface IRouteGroup {",
                        "  void loadInto(java.util.Map<String, com.alibaba.android.arouter.facade.model.RouteMeta> routes);",
                        "}"),
                source("com.alibaba.android.arouter.facade.template.IRouteRoot",
                        "public interface IRouteRoot {",
                        "  void loadInto(java.util.Map<String, Class<? extends IRouteGroup>> routes);",
                        "}"),
                source("com.alibaba.android.arouter.facade.template.IProviderGroup",
                        "public interface IProviderGroup {",
                        "  void loadInto(java.util.Map<String, com.alibaba.android.arouter.facade.model.RouteMeta> providers);",
                        "}"),
                source("com.alibaba.android.arouter.facade.service.SerializationService",
                        "public interface SerializationService extends com.alibaba.android.arouter.facade.template.IProvider {",
                        "  <T> T parseObject(String input, java.lang.reflect.Type type);",
                        "}"),
                source("com.alibaba.android.arouter.launcher.ARouter",
                        "public final class ARouter {",
                        "  private static final ARouter INSTANCE = new ARouter();",
                        "  public static ARouter getInstance() { return INSTANCE; }",
                        "  public <T> T navigation(Class<? extends T> service) { return null; }",
                        "}")
        ));
    }

    private static JavaFileObject source(String name, String... body) {
        List<String> lines = new ArrayList<>();
        int packageSeparator = name.lastIndexOf('.');
        lines.add("package " + name.substring(0, packageSeparator) + ";");
        lines.addAll(Arrays.asList(body));
        return JavaFileObjects.forSourceLines(name, lines);
    }
}

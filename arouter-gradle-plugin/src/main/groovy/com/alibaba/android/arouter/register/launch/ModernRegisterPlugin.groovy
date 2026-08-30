package com.alibaba.android.arouter.register.launch

import com.alibaba.android.arouter.register.core.ScopedRegisterTask
import com.alibaba.android.arouter.register.utils.Logger
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Wires the registration transform through the public Scoped Artifacts API
 * available in Android Gradle Plugin 7.4 and newer.
 *
 * <p>The AGP types are intentionally resolved at runtime. This keeps the same
 * plugin artifact loadable on the legacy AGP versions that still use the
 * Transform API.</p>
 */
class ModernRegisterPlugin {
    private static final String SCOPED_ARTIFACTS_SCOPE =
            'com.android.build.api.variant.ScopedArtifacts$Scope'
    private static final String SCOPED_ARTIFACT =
            'com.android.build.api.artifact.ScopedArtifact'
    private static final String SCOPED_ARTIFACT_CLASSES =
            'com.android.build.api.artifact.ScopedArtifact$CLASSES'
    private static final String SCOPED_ARTIFACTS =
            'com.android.build.api.variant.ScopedArtifacts'
    private static final String SCOPED_ARTIFACTS_OPERATION =
            'com.android.build.api.variant.ScopedArtifactsOperation'
    private static final String ARTIFACTS =
            'com.android.build.api.artifact.Artifacts'
    private static final String KOTLIN_FUNCTION_1 = 'kotlin.jvm.functions.Function1'

    static boolean isSupported(Project project) {
        def androidComponents = project.extensions.findByName('androidComponents')
        if (androidComponents == null) {
            return false
        }

        try {
            Class.forName(SCOPED_ARTIFACT_CLASSES, false, androidComponents.class.classLoader)
            return true
        } catch (ClassNotFoundException ignored) {
            return false
        }
    }

    static void configure(Project project) {
        def androidComponents = project.extensions.getByName('androidComponents')
        def selector = androidComponents.selector().all()

        androidComponents.onVariants(selector, { variant ->
            String taskName = 'transform' + capitalize(variant.name) + 'ClassesWithARouter'
            TaskProvider<ScopedRegisterTask> taskProvider =
                    project.tasks.register(taskName, ScopedRegisterTask)
            wireTransform(variant.artifacts, taskProvider)
        } as Action)

        Logger.i('Use Android Scoped Artifacts registration pipeline')
    }

    private static void wireTransform(Object artifacts, TaskProvider<ScopedRegisterTask> taskProvider) {
        ClassLoader loader = artifacts.class.classLoader
        Class scopeType = Class.forName(SCOPED_ARTIFACTS_SCOPE, true, loader)
        Class scopedArtifactType = Class.forName(SCOPED_ARTIFACT, true, loader)
        Class functionType = Class.forName(KOTLIN_FUNCTION_1, true, loader)

        Object allScope = Enum.valueOf(scopeType, 'ALL')
        Object classesArtifact = Class.forName(SCOPED_ARTIFACT_CLASSES, true, loader)
                .getField('INSTANCE')
                .get(null)

        Class artifactsType = Class.forName(ARTIFACTS, true, loader)
        Object scopedArtifacts = artifactsType.getMethod('forScope', scopeType)
                .invoke(artifacts, allScope)

        Class scopedArtifactsType = Class.forName(SCOPED_ARTIFACTS, true, loader)
        Object operation = scopedArtifactsType.getMethod('use', TaskProvider)
                .invoke(scopedArtifacts, taskProvider)

        Object jars = function1(functionType) { ScopedRegisterTask task -> task.allJars }
        Object directories = function1(functionType) { ScopedRegisterTask task -> task.allDirectories }
        Object output = function1(functionType) { ScopedRegisterTask task -> task.output }

        Class operationType = Class.forName(SCOPED_ARTIFACTS_OPERATION, true, loader)
        Method transform = operationType.getMethod(
                'toTransform',
                scopedArtifactType,
                functionType,
                functionType,
                functionType
        )
        transform.invoke(operation, classesArtifact, jars, directories, output)
    }

    private static Object function1(Class functionType, Closure body) {
        InvocationHandler handler = { Object proxy, Method method, Object[] args ->
            switch (method.name) {
                case 'invoke':
                    return body.call(args[0])
                case 'toString':
                    return 'ARouter scoped artifact property selector'
                case 'hashCode':
                    return System.identityHashCode(proxy)
                case 'equals':
                    return proxy.is(args[0])
                default:
                    return null
            }
        } as InvocationHandler

        return Proxy.newProxyInstance(
                functionType.classLoader,
                [functionType] as Class[],
                handler
        )
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value
        }
        return String.valueOf(Character.toUpperCase(value.charAt(0))) + value.substring(1)
    }
}

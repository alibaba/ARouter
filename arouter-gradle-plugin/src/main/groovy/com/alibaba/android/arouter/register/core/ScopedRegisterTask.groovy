package com.alibaba.android.arouter.register.core

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Transforms the complete class scope supplied by AGP 7.4+ into a single jar
 * with ARouter's registration calls injected into LogisticsCenter.
 */
@CacheableTask
abstract class ScopedRegisterTask extends DefaultTask {

    @Classpath
    abstract ListProperty<RegularFile> getAllJars()

    @Classpath
    abstract ListProperty<Directory> getAllDirectories()

    @OutputFile
    abstract RegularFileProperty getOutput()

    @TaskAction
    void registerRoutes() {
        List<File> jars = allJars.get().collect { it.asFile }
        List<File> directories = allDirectories.get().collect { it.asFile }

        ScopedClassTransformer.Result result = ScopedClassTransformer.transform(
                jars,
                directories,
                output.get().asFile
        )

        if (result.logisticsCenterFound) {
            logger.info('ARouter::Register >>> Injected ' + result.registrationCount +
                    ' route registration classes')
        } else {
            logger.warn('ARouter::Register >>> LogisticsCenter.class was not found; ' +
                    'automatic registration was not injected')
        }
    }
}

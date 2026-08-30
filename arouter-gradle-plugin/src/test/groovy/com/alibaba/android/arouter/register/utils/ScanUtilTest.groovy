package com.alibaba.android.arouter.register.utils

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class ScanUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void ignoresDirectoriesAndResourcesInsideTheGeneratedRoutePackage() {
        File input = temporaryFolder.newFile('routes.jar')
        JarOutputStream output = new JarOutputStream(new FileOutputStream(input))
        try {
            output.putNextEntry(new JarEntry('com/alibaba/android/arouter/routes/'))
            output.closeEntry()

            output.putNextEntry(new JarEntry('com/alibaba/android/arouter/routes/README.txt'))
            output.write('not bytecode'.getBytes('UTF-8'))
            output.closeEntry()
        } finally {
            output.close()
        }

        ScanUtil.scanJar(input, temporaryFolder.newFile('destination.jar'))
    }

    @Test
    void onlyProcessesClassFilesInsideTheGeneratedRoutePackage() {
        assertTrue(ScanUtil.shouldProcessClass('com/alibaba/android/arouter/routes/ARouter$$Root$$app.class'))
        assertFalse(ScanUtil.shouldProcessClass('com/alibaba/android/arouter/routes/'))
        assertFalse(ScanUtil.shouldProcessClass('com/alibaba/android/arouter/routes/metadata.json'))
        assertFalse(ScanUtil.shouldProcessClass('other/package/Route.class'))
    }

    @Test
    void acceptsModernJavaClassFiles() {
        ClassWriter writer = new ClassWriter(0)
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC,
                'com/alibaba/android/arouter/routes/ModernRoute$Nested',
                null,
                'java/lang/Object',
                null
        )
        writer.visitNestHost('com/alibaba/android/arouter/routes/ModernRoute')
        writer.visitEnd()

        ScanUtil.scanClass(new ByteArrayInputStream(writer.toByteArray()))
    }
}

package com.alibaba.android.arouter.register.utils

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

import static org.junit.Assert.assertEquals
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

        assertFalse(ScanUtil.scanJar(input, []))
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

        ScanUtil.scanClass(new ByteArrayInputStream(writer.toByteArray()), [])
    }

    @Test
    void keepsImplementationsInTheCallingTransformState() {
        String dailyClass = 'com/alibaba/android/arouter/routes/ARouter$$Root$$featuredaily'
        String onlineClass = 'com/alibaba/android/arouter/routes/ARouter$$Root$$featureonline'
        ScanSetting dailyRoot = new ScanSetting('IRouteRoot')
        ScanSetting onlineRoot = new ScanSetting('IRouteRoot')

        ScanUtil.scanClass(
                new ByteArrayInputStream(routeClass(dailyClass, dailyRoot.interfaceName)),
                [dailyRoot]
        )
        ScanUtil.scanClass(
                new ByteArrayInputStream(routeClass(onlineClass, onlineRoot.interfaceName)),
                [onlineRoot]
        )

        assertEquals([dailyClass], dailyRoot.classList)
        assertEquals([onlineClass], onlineRoot.classList)
    }

    private static byte[] routeClass(String className, String interfaceName) {
        ClassWriter writer = new ClassWriter(0)
        writer.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                className,
                null,
                'java/lang/Object',
                [interfaceName] as String[]
        )
        writer.visitEnd()
        return writer.toByteArray()
    }
}

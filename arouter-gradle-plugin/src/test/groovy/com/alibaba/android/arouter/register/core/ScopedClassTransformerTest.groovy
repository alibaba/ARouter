package com.alibaba.android.arouter.register.core

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

import static org.junit.Assert.assertArrayEquals
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ScopedClassTransformerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Test
    void scansAllScopesAndInjectsDeterministicRegistrationCalls() {
        String rootClass = 'com/alibaba/android/arouter/routes/ARouter$$Root$$dependency'
        String providerClass = 'com/alibaba/android/arouter/routes/ARouter$$Providers$$app'

        File inputJar = temporaryFolder.newFile('dependency.jar')
        writeJar(inputJar, [
                'com/alibaba/android/arouter/core/LogisticsCenter.class': logisticsCenterClass(),
                (rootClass + '.class'): routeClass(
                        rootClass,
                        'com/alibaba/android/arouter/facade/template/IRouteRoot'
                )
        ])

        File classes = temporaryFolder.newFolder('classes')
        writeClassFile(
                classes,
                providerClass,
                routeClass(
                        providerClass,
                        'com/alibaba/android/arouter/facade/template/IProviderGroup'
                )
        )

        File firstOutput = temporaryFolder.newFile('first.jar')
        File secondOutput = temporaryFolder.newFile('second.jar')

        ScopedClassTransformer.Result result = ScopedClassTransformer.transform(
                [inputJar],
                [classes],
                firstOutput
        )
        ScopedClassTransformer.transform([inputJar], [classes], secondOutput)

        assertTrue(result.logisticsCenterFound)
        assertEquals(2, result.registrationCount)
        assertEquals(
                [
                        'com.alibaba.android.arouter.routes.ARouter$$Providers$$app',
                        'com.alibaba.android.arouter.routes.ARouter$$Root$$dependency'
                ],
                registeredClasses(readEntry(
                        firstOutput,
                        'com/alibaba/android/arouter/core/LogisticsCenter.class'
                ))
        )
        assertArrayEquals(firstOutput.bytes, secondOutput.bytes)
    }

    private static byte[] routeClass(String className, String interfaceName) {
        ClassWriter writer = new ClassWriter(0)
        writer.visit(
                Opcodes.V17,
                Opcodes.ACC_PUBLIC,
                className,
                null,
                'java/lang/Object',
                [interfaceName] as String[]
        )
        writer.visitEnd()
        return writer.toByteArray()
    }

    private static byte[] logisticsCenterClass() {
        ClassWriter writer = new ClassWriter(0)
        writer.visit(
                Opcodes.V1_8,
                Opcodes.ACC_PUBLIC,
                'com/alibaba/android/arouter/core/LogisticsCenter',
                null,
                'java/lang/Object',
                null
        )

        MethodVisitor load = writer.visitMethod(
                Opcodes.ACC_STATIC,
                'loadRouterMap',
                '()V',
                null,
                null
        )
        load.visitCode()
        load.visitInsn(Opcodes.RETURN)
        load.visitMaxs(0, 0)
        load.visitEnd()

        MethodVisitor register = writer.visitMethod(
                Opcodes.ACC_STATIC,
                'register',
                '(Ljava/lang/String;)V',
                null,
                null
        )
        register.visitCode()
        register.visitInsn(Opcodes.RETURN)
        register.visitMaxs(0, 1)
        register.visitEnd()

        writer.visitEnd()
        return writer.toByteArray()
    }

    private static List<String> registeredClasses(byte[] logisticsCenter) {
        List<String> classes = []
        new ClassReader(logisticsCenter).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            MethodVisitor visitMethod(int access, String name, String descriptor,
                                      String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions)
                if (name != 'loadRouterMap') {
                    return method
                }

                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override
                    void visitLdcInsn(Object value) {
                        if (value instanceof String) {
                            classes.add((String) value)
                        }
                        super.visitLdcInsn(value)
                    }
                }
            }
        }, 0)
        return classes
    }

    private static void writeJar(File jar, Map<String, byte[]> entries) {
        JarOutputStream output = new JarOutputStream(new FileOutputStream(jar))
        try {
            entries.each { String name, byte[] bytes ->
                output.putNextEntry(new JarEntry(name))
                output.write(bytes)
                output.closeEntry()
            }
        } finally {
            output.close()
        }
    }

    private static void writeClassFile(File root, String className, byte[] bytes) {
        File output = new File(root, className + '.class')
        if (!output.parentFile.mkdirs() && !output.parentFile.isDirectory()) {
            throw new IOException('Cannot create test class directory: ' + output.parentFile)
        }
        output.bytes = bytes
    }

    private static byte[] readEntry(File jar, String entryName) {
        JarFile jarFile = new JarFile(jar)
        try {
            JarEntry entry = jarFile.getJarEntry(entryName)
            assertTrue('Missing jar entry ' + entryName, entry != null)
            InputStream input = jarFile.getInputStream(entry)
            try {
                return input.bytes
            } finally {
                input.close()
            }
        } finally {
            jarFile.close()
        }
    }
}

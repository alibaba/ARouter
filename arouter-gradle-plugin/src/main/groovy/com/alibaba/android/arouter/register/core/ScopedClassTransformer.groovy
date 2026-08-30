package com.alibaba.android.arouter.register.core

import com.alibaba.android.arouter.register.utils.ScanSetting
import groovy.transform.CompileStatic
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Stateless class scanner and jar transformer used by the AGP 7.4+ task.
 */
class ScopedClassTransformer {
    private static final List<String> REGISTER_INTERFACES = [
            'com/alibaba/android/arouter/facade/template/IRouteRoot',
            'com/alibaba/android/arouter/facade/template/IInterceptorGroup',
            'com/alibaba/android/arouter/facade/template/IProviderGroup'
    ].asImmutable()

    static Result transform(Collection<File> jars, Collection<File> directories, File output) {
        Map<String, Set<String>> implementations = new LinkedHashMap<>()
        REGISTER_INTERFACES.each { implementations.put(it, new TreeSet<String>()) }

        jars.each { scanJar(it, implementations) }
        directories.each { scanDirectory(it, implementations) }

        Set<String> registrations = new TreeSet<>()
        implementations.values().each { registrations.addAll(it) }

        File parent = output.parentFile
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException('Cannot create ARouter transform output directory: ' + parent)
        }

        File temporary = new File(parent, output.name + '.tmp')
        if (temporary.exists() && !temporary.delete()) {
            throw new IOException('Cannot replace temporary ARouter transform output: ' + temporary)
        }

        boolean logisticsCenterFound = false
        Set<String> writtenEntries = new HashSet<>()

        try {
            JarOutputStream jarOutput = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(temporary)))
            try {
                jars.each { File jar ->
                    logisticsCenterFound |= copyJar(jar, jarOutput, writtenEntries, registrations)
                }
                directories.each { File directory ->
                    logisticsCenterFound |= copyDirectory(directory, jarOutput, writtenEntries, registrations)
                }
            } finally {
                jarOutput.close()
            }

            Files.move(temporary.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (Throwable failure) {
            temporary.delete()
            throw failure
        }

        return new Result(logisticsCenterFound, registrations.size())
    }

    private static void scanJar(File jar, Map<String, Set<String>> implementations) {
        if (!jar.isFile()) {
            return
        }

        JarFile jarFile = new JarFile(jar)
        try {
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement()
                if (!entry.isDirectory() && shouldProcessClass(entry.name)) {
                    InputStream input = jarFile.getInputStream(entry)
                    try {
                        scanClass(input, implementations)
                    } finally {
                        input.close()
                    }
                }
            }
        } finally {
            jarFile.close()
        }
    }

    private static void scanDirectory(File directory, Map<String, Set<String>> implementations) {
        if (!directory.isDirectory()) {
            return
        }

        directory.eachFileRecurse { File file ->
            if (file.isFile()) {
                String relativePath = relativePath(directory, file)
                if (shouldProcessClass(relativePath)) {
                    InputStream input = new FileInputStream(file)
                    try {
                        scanClass(input, implementations)
                    } finally {
                        input.close()
                    }
                }
            }
        }
    }

    private static void scanClass(InputStream input, Map<String, Set<String>> implementations) {
        ClassReader reader = new ClassReader(input)
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
            @Override
            void visit(int version, int access, String name, String signature,
                       String superName, String[] interfaces) {
                interfaces?.each { String interfaceName ->
                    implementations.get(interfaceName)?.add(name)
                }
            }
        }
        reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
    }

    private static boolean copyJar(File jar, JarOutputStream output, Set<String> written,
                                   Collection<String> registrations) {
        if (!jar.isFile()) {
            return false
        }

        boolean logisticsCenterFound = false
        JarFile jarFile = new JarFile(jar)
        try {
            Enumeration<JarEntry> entries = jarFile.entries()
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement()
                if (!entry.isDirectory() && !isSignatureFile(entry.name)) {
                    InputStream input = jarFile.getInputStream(entry)
                    try {
                        logisticsCenterFound |= writeEntry(
                                entry.name,
                                input,
                                output,
                                written,
                                registrations
                        )
                    } finally {
                        input.close()
                    }
                }
            }
        } finally {
            jarFile.close()
        }
        return logisticsCenterFound
    }

    private static boolean copyDirectory(File directory, JarOutputStream output, Set<String> written,
                                         Collection<String> registrations) {
        if (!directory.isDirectory()) {
            return false
        }

        boolean logisticsCenterFound = false
        List<File> files = []
        directory.eachFileRecurse { File file ->
            if (file.isFile()) {
                files.add(file)
            }
        }
        files.sort { File left, File right ->
            relativePath(directory, left) <=> relativePath(directory, right)
        }

        files.each { File file ->
            String path = relativePath(directory, file)
            if (!isSignatureFile(path)) {
                InputStream input = new FileInputStream(file)
                try {
                    logisticsCenterFound |= writeEntry(path, input, output, written, registrations)
                } finally {
                    input.close()
                }
            }
        }
        return logisticsCenterFound
    }

    private static boolean writeEntry(String name, InputStream input, JarOutputStream output,
                                      Set<String> written, Collection<String> registrations) {
        if (!written.add(name)) {
            return false
        }

        byte[] bytes = input.bytes
        boolean logisticsCenter = ScanSetting.GENERATE_TO_CLASS_FILE_NAME == name
        if (logisticsCenter && !registrations.isEmpty()) {
            bytes = injectRegistrationCode(bytes, registrations)
        }

        ZipEntry outputEntry = new ZipEntry(name)
        outputEntry.time = 0L
        output.putNextEntry(outputEntry)
        output.write(bytes)
        output.closeEntry()
        return logisticsCenter
    }

    private static byte[] injectRegistrationCode(byte[] original, Collection<String> registrations) {
        ClassReader reader = new ClassReader(original)
        ClassWriter writer = new ClassWriter(reader, 0)
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            @CompileStatic
            MethodVisitor visitMethod(int access, String name, String descriptor,
                                      String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions)
                if (ScanSetting.GENERATE_TO_METHOD_NAME != name || descriptor != '()V') {
                    return method
                }

                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override
                    @CompileStatic
                    void visitInsn(int opcode) {
                        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                            registrations.each { String className ->
                                mv.visitLdcInsn(className.replace('/', '.'))
                                mv.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        ScanSetting.GENERATE_TO_CLASS_NAME,
                                        ScanSetting.REGISTER_METHOD_NAME,
                                        '(Ljava/lang/String;)V',
                                        false
                                )
                            }
                        }
                        super.visitInsn(opcode)
                    }

                    @Override
                    @CompileStatic
                    void visitMaxs(int maxStack, int maxLocals) {
                        super.visitMaxs(maxStack + 4, maxLocals)
                    }
                }
            }
        }
        reader.accept(visitor, ClassReader.EXPAND_FRAMES)
        return writer.toByteArray()
    }

    private static boolean shouldProcessClass(String entryName) {
        return entryName != null &&
                entryName.startsWith(ScanSetting.ROUTER_CLASS_PACKAGE_NAME) &&
                entryName.endsWith('.class')
    }

    private static boolean isSignatureFile(String entryName) {
        String upper = entryName.toUpperCase(Locale.ROOT)
        return upper.startsWith('META-INF/') &&
                (upper.endsWith('.SF') || upper.endsWith('.RSA') ||
                        upper.endsWith('.DSA') || upper.endsWith('.EC'))
    }

    private static String relativePath(File root, File file) {
        return root.toPath().relativize(file.toPath()).toString()
                .replace(File.separatorChar, '/' as char)
    }

    static class Result {
        final boolean logisticsCenterFound
        final int registrationCount

        Result(boolean logisticsCenterFound, int registrationCount) {
            this.logisticsCenterFound = logisticsCenterFound
            this.registrationCount = registrationCount
        }
    }
}

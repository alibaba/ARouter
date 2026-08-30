package com.alibaba.android.arouter.register.utils

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile

/**
 * Scan all class in the package: com/alibaba/android/arouter/
 * find out all routers,interceptors and providers
 * @author billy.qi email: qiyilike@163.com
 * @since 17/3/20 11:48
 */
class ScanUtil {

    /**
     * scan jar file
     * @param jarFile All jar files that are compiled into apk
     * @param registerList interfaces and implementations for this transform invocation
     * @return true when the jar contains LogisticsCenter.class
     */
    static boolean scanJar(File jarFile, Collection<ScanSetting> registerList) {
        if (!jarFile) {
            return false
        }

        boolean containsInitClass = false
        def file = new JarFile(jarFile)
        try {
            Enumeration enumeration = file.entries()
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement()
                String entryName = jarEntry.getName()
                if (!jarEntry.isDirectory() && shouldProcessClass(entryName)) {
                    InputStream inputStream = file.getInputStream(jarEntry)
                    try {
                        scanClass(inputStream, registerList)
                    } finally {
                        inputStream.close()
                    }
                } else if (ScanSetting.GENERATE_TO_CLASS_FILE_NAME == entryName) {
                    containsInitClass = true
                }
            }
        } finally {
            file.close()
        }
        return containsInitClass
    }

    static boolean shouldProcessPreDexJar(String path) {
        return !path.contains("com.android.support") && !path.contains("/android/m2repository")
    }

    static boolean shouldProcessClass(String entryName) {
        return entryName != null &&
                entryName.startsWith(ScanSetting.ROUTER_CLASS_PACKAGE_NAME) &&
                entryName.endsWith('.class')
    }

    /**
     * scan class file
     * @param class file
     */
    static void scanClass(File file, Collection<ScanSetting> registerList) {
        InputStream input = new FileInputStream(file)
        try {
            scanClass(input, registerList)
        } finally {
            input.close()
        }
    }

    static void scanClass(InputStream inputStream, Collection<ScanSetting> registerList) {
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ScanClassVisitor cv = new ScanClassVisitor(ScanSetting.ASM_API, cw, registerList)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
    }

    static class ScanClassVisitor extends ClassVisitor {
        private final Collection<ScanSetting> registerList

        ScanClassVisitor(int api, ClassVisitor cv, Collection<ScanSetting> registerList) {
            super(api, cv)
            this.registerList = registerList
        }

        void visit(int version, int access, String name, String signature,
                   String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces)
            registerList.each { ext ->
                if (ext.interfaceName && interfaces != null) {
                    interfaces.each { itName ->
                        if (itName == ext.interfaceName) {
                            //fix repeated inject init code when Multi-channel packaging
                            if (!ext.classList.contains(name)) {
                                ext.classList.add(name)
                            }
                        }
                    }
                }
            }
        }
    }

}

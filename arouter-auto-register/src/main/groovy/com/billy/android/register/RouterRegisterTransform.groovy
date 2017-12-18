package com.billy.android.register

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * transform api
 * <p>
 *     1. Scan all classes to find which classes implement the specified interface
 *     2. Generate register code into class file: {@link RouterRegisterSetting#GENERATE_TO_CLASS_FILE_NAME}
 * @author billy.qi email: qiyilike@163.com
 * @since 17/3/21 11:48
 */
class RouterRegisterTransform extends Transform {

    Project project
    static ArrayList<RouterRegisterSetting> registerList
    static File fileContainsInitClass;

    RouterRegisterTransform(Project project) {
        this.project = project
    }

    /**
     * name of this transform
     * @return
     */
    @Override
    String getName() {
        return "arouter-auto-register"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * The plugin will scan all classes in the project
     * @return
     */
    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }


    @Override
    void transform(Context context, Collection<TransformInput> inputs
                   , Collection<TransformInput> referencedInputs
                   , TransformOutputProvider outputProvider
                   , boolean isIncremental) throws IOException, TransformException, InterruptedException {
        println("\n\n============================ start arouter-auto-register ============================\n")
        long time = System.currentTimeMillis()
        inputs.each { TransformInput input ->

            // scan all jars
            input.jarInputs.each { JarInput jarInput ->
                String destName = jarInput.name
                // rename jar files
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath)
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4)
                }
                // input file
                File src = jarInput.file
                // output file
                File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

                //scan jar file to find classes
                if (RouterScanUtil.shouldProcessPreDexJar(src.absolutePath)) {
                    RouterScanUtil.scanJar(src, dest)
                }
                FileUtils.copyFile(src, dest)

            }
            // scan class files
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                String root = directoryInput.file.absolutePath
                if (!root.endsWith(File.separator))
                    root += File.separator
                directoryInput.file.eachFileRecurse { File file ->
                    def path = file.absolutePath.replace(root, '')
                    if(file.isFile() && RouterScanUtil.shouldProcessClass(path)){
                        RouterScanUtil.scanClass(file)
                    }
                }

                // copy to dest
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
        def scanFinishTime = System.currentTimeMillis()
        println(">>>>>> register scan all class cost time: " + (scanFinishTime - time) + " ms")

        if (fileContainsInitClass) {
            registerList.each { ext ->
                println("\ninsert register code to file:" + fileContainsInitClass.absolutePath)
                if (ext.classList.isEmpty()) {
                    project.logger.error("No class implements found for interface:" + ext.interfaceName)
                } else {
                    ext.classList.each {
                        println(it)
                    }
                    RouterRegisterCodeGenerator.insertInitCodeTo(ext)
                }
            }
        }
        def finishTime = System.currentTimeMillis()
        println("\n>>>>>> register insert code cost time: " + (finishTime - scanFinishTime) + " ms")
        println(">>>>>> register cost time: " + (finishTime - time) + " ms")
        println("\n============================ finish arouter-auto-register ============================\n\n")
    }

}

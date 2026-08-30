package com.alibaba.android.arouter.register.launch

import com.alibaba.android.arouter.register.utils.Logger
import com.alibaba.android.arouter.register.utils.ScanSetting
import com.alibaba.android.arouter.register.core.RegisterTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * Simple version of AutoRegister plugin for ARouter
 * @author billy.qi email: qiyilike@163.com
 * @since 17/12/06 15:35
 */
public class PluginLaunch implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Only application modules generate the final registration table. Using
        // withPlugin also supports applying ARouter before the Android plugin.
        project.pluginManager.withPlugin('com.android.application') {
            Logger.make(project)
            Logger.i('Project enable arouter-register plugin')

            if (ModernRegisterPlugin.isSupported(project)) {
                ModernRegisterPlugin.configure(project)
            } else {
                configureLegacyTransform(project)
            }
        }
    }

    private static void configureLegacyTransform(Project project) {
        def android = project.extensions.getByName('android')
        def transformImpl = new RegisterTransform(project)

        ArrayList<ScanSetting> list = new ArrayList<>(3)
        list.add(new ScanSetting('IRouteRoot'))
        list.add(new ScanSetting('IInterceptorGroup'))
        list.add(new ScanSetting('IProviderGroup'))
        RegisterTransform.registerList = list
        android.registerTransform(transformImpl)
    }
}

package com.billy.android.register

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * Simple version of AutoRegister plugin for ARouter
 * @author billy.qi email: qiyilike@163.com
 * @since 17/12/06 15:35
 */
public class RouterRegisterPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        def isApp = project.plugins.hasPlugin(AppPlugin)
        //only application module needs this plugin to generate register code
        if (isApp) {
            println 'project(' + project.name + ') apply arouter-auto-register plugin'
            def android = project.extensions.getByType(AppExtension)
            def transformImpl = new RouterRegisterTransform(project)

            //init arouter-auto-register settings
            ArrayList<RouterRegisterSetting> list = new ArrayList<>(3)
            list.add(new RouterRegisterSetting('IRouteRoot', 'registerRouteRoot'))
            list.add(new RouterRegisterSetting('IInterceptorGroup', 'registerInterceptor'))
            list.add(new RouterRegisterSetting('IProviderGroup', 'registerProvider'))
            RouterRegisterTransform.registerList = list
            //register this plugin
            android.registerTransform(transformImpl)
        }
    }

}

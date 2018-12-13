package com.alibaba.android.arouter.idea.extensions

import com.alibaba.android.arouter.idea.extensions.Const.NOTIFY_NO_TARGET_TIPS
import com.alibaba.android.arouter.idea.extensions.Const.NOTIFY_SERVICE_NAME
import com.alibaba.android.arouter.idea.extensions.Const.NOTIFY_TITLE
import com.alibaba.android.arouter.idea.extensions.Const.ROUTE_ANNOTATION_NAME
import com.alibaba.android.arouter.idea.extensions.Const.SDK_NAME
import com.alibaba.android.arouter.idea.extensions.Const.navigationOnIcon
import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.navigation.NavigationItem
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import java.awt.event.MouseEvent

/**
 * Mark navigation target.
 *
 * @author zhilong <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2018/12/13 12:30 PM
 */
class NavigationLineMarker : LineMarkerProviderDescriptor(), GutterIconNavigationHandler<PsiElement> {
    override fun getName(): String? {
        return "ARouter Location"
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return if (isNavigationCall(element)) {
            LineMarkerInfo<PsiElement>(element, element.textRange, navigationOnIcon,
                    Pass.UPDATE_ALL, null, this,
                    GutterIconRenderer.Alignment.LEFT)
        } else {
            null
        }
    }

    override fun navigate(e: MouseEvent?, psiElement: PsiElement?) {
        if (psiElement is PsiMethodCallExpression) {
            val psiExpressionList = (psiElement as PsiMethodCallExpressionImpl).argumentList
            if (psiExpressionList.expressionCount == 1) {
                // Support `build(path)` only now.

                val targetPath = psiExpressionList.children[1].text.replace("\"", "")
                val fullScope = GlobalSearchScope.allScope(psiElement.project)
                val routeAnnotationWrapper = AnnotatedMembersSearch.search(getAnnotationWrapper(psiElement, fullScope)
                        ?: return, fullScope).findAll()
                val target = routeAnnotationWrapper.find {
                    it.annotations.map { it.findAttributeValue("path")?.text?.replace("\"", "") }.contains(targetPath)
                }

                if (null == target) {
                    Notifications.Bus.notify(Notification(NOTIFY_SERVICE_NAME, NOTIFY_TITLE, NOTIFY_NO_TARGET_TIPS, NotificationType.WARNING))
                } else {
                    // Redirect to target.
                    NavigationItem::class.java.cast(target).navigate(true)
                }
            }
        }
    }

    private fun getAnnotationWrapper(psiElement: PsiElement?, scope: GlobalSearchScope): PsiClass? {
        if (null == routeAnnotationWrapper) {
            routeAnnotationWrapper = JavaPsiFacade.getInstance(psiElement?.project).findClass(ROUTE_ANNOTATION_NAME, scope)
        }

        return routeAnnotationWrapper
    }

    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {}

    /**
     * Judge whether the code used for navigation.
     */
    private fun isNavigationCall(psiElement: PsiElement): Boolean {
        if (psiElement is PsiCallExpression) {
            val method = psiElement.resolveMethod() ?: return false
            val parent = method.parent

            if (method.name == "build" && parent is PsiClass) {
                if (isClassOfARouter(parent)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Judge whether the caller was ARouter
     */
    private fun isClassOfARouter(psiClass: PsiClass): Boolean {
        // It was ARouter
        if (psiClass.name.equals(SDK_NAME)) {
            return true
        }

        // It super class was ARouter
        psiClass.supers.find { it.name == SDK_NAME } ?: return false

        return true
    }

    // I'm 100% sure this point can not made memory leak.
    private var routeAnnotationWrapper: PsiClass? = null
}
package com.alibaba.android.arouter.idea.extensions

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiVariable
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.toUElementOfType

/** Adds route destination gutter navigation to Java and Kotlin ARouter calls. */
class NavigationLineMarker : LineMarkerProviderDescriptor() {
    override fun getName(): String = "ARouter Location"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        elements.forEach { element ->
            createMarker(element)?.let(result::add)
        }
    }

    internal fun createMarker(element: PsiElement): LineMarkerInfo<*>? {
        val call = navigationCall(element) ?: return null
        val path = call.valueArguments.firstOrNull()?.constantString() ?: return null
        val targets = RouteTargetResolver.findTargets(element.project, path)
        if (targets.isEmpty()) {
            return null
        }

        return NavigationGutterIconBuilder.create(NavigationIcons.route)
            .setTargets(targets)
            .setTooltipText("Navigate to ARouter destination")
            .createLineMarkerInfo(element)
    }

    internal fun navigationPath(element: PsiElement): String? {
        return navigationCall(element)
            ?.valueArguments
            ?.firstOrNull()
            ?.constantString()
    }

    private fun navigationCall(element: PsiElement): UCallExpression? {
        val call = generateSequence(element) { it.parent }
            .take(MAX_CALL_ANCESTOR_DEPTH)
            .mapNotNull { it.toUElementOfType<UCallExpression>() }
            .firstOrNull { it.isAnchoredAt(element) }
            ?: return null

        val method = call.resolve() ?: return null
        if (method.name != BUILD_METHOD_NAME || !isARouterClass(method.containingClass)) {
            return null
        }
        return call
    }

    private fun UCallExpression.isAnchoredAt(element: PsiElement): Boolean {
        val sourceIdentifier = methodIdentifier?.sourcePsi ?: return false
        return (sourceIdentifier.firstChild ?: sourceIdentifier) == element
    }

    private fun isARouterClass(psiClass: PsiClass?): Boolean {
        return isARouterClass(psiClass, mutableSetOf())
    }

    private fun isARouterClass(psiClass: PsiClass?, visited: MutableSet<PsiClass>): Boolean {
        if (psiClass == null || !visited.add(psiClass)) {
            return false
        }
        if (psiClass.qualifiedName == AROUTER_CLASS_NAME) {
            return true
        }
        return psiClass.supers.any { isARouterClass(it, visited) }
    }

    private fun UExpression.constantString(): String? {
        val uastValue = evaluate() as? String
        if (uastValue != null) {
            return uastValue
        }
        val helper = JavaPsiFacade.getInstance(sourcePsi?.project ?: return null)
            .constantEvaluationHelper
        val javaValue = (sourcePsi as? PsiExpression)
            ?.let(helper::computeConstantExpression) as? String
        if (javaValue != null) {
            return javaValue
        }
        val initializer = ((this as? UReferenceExpression)?.resolve() as? PsiVariable)
            ?.initializer
            ?: return null
        return helper.computeConstantExpression(initializer) as? String
    }

    companion object {
        internal const val ROUTE_ANNOTATION_NAME =
            "com.alibaba.android.arouter.facade.annotation.Route"
        internal const val AROUTER_CLASS_NAME =
            "com.alibaba.android.arouter.launcher.ARouter"
        private const val BUILD_METHOD_NAME = "build"
        private const val MAX_CALL_ANCESTOR_DEPTH = 12
    }
}

private object NavigationIcons {
    val route = IconLoader.getIcon(
        "/icon/outline_my_location_black_18dp.png",
        NavigationLineMarker::class.java.classLoader
    )
}

internal object RouteTargetResolver {
    fun findTargets(project: Project, path: String): List<PsiElement> {
        val targetScope = GlobalSearchScope.projectScope(project)
        val dependencyScope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        val annotation = facade.findClass(
            NavigationLineMarker.ROUTE_ANNOTATION_NAME,
            dependencyScope
        ) ?: return emptyList()
        return AnnotatedMembersSearch.search(annotation, targetScope)
            .findAll()
            .asSequence()
            .filter { path in routePaths(it) }
            .map { it.navigationElement }
            .distinct()
            .toList()
    }

    internal fun routePaths(owner: PsiModifierListOwner): Set<String> {
        return owner.modifierList
            ?.annotations
            ?.flatMapTo(linkedSetOf()) { routePaths(it) }
            ?: emptySet()
    }

    private fun routePaths(annotation: PsiAnnotation): Set<String> {
        if (annotation.qualifiedName != NavigationLineMarker.ROUTE_ANNOTATION_NAME) {
            return emptySet()
        }
        return annotation.findAttributeValue("path")
            ?.constantString(annotation.project)
            ?.let(::setOf)
            ?: emptySet()
    }

    private fun PsiAnnotationMemberValue.constantString(project: Project): String? {
        val javaValue = JavaPsiFacade.getInstance(project)
            .constantEvaluationHelper
            .computeConstantExpression(this) as? String
        return javaValue ?: toUElementOfType<UExpression>()?.evaluate() as? String
    }
}

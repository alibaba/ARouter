package com.alibaba.android.arouter.idea.extensions

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.formatting.blocks.prev
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.PsiTreeUtil

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.isNullOrEmpty
import java.util.regex.Pattern
import javax.swing.Icon

/**
 * 支持 kotlin 的Arouter行解读器
 * @author lckj686
 * @version 1.0
 * @since 2021-08-01
 */
class KtNavigationLineMarker : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    private var filePath: String? = ""
    private var projectPath: String? = ""
    private var time: Long = 0

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        if (elements.isNullOrEmpty()) {
            result
        }
        time = System.currentTimeMillis()
        val project = elements[0].project
        projectPath = project.basePath
        filePath = elements[0].containingFile.viewProvider.virtualFile.canonicalPath
        elements.forEachIndexed { index, psiElement ->

            var target: TargetContent? = null

            //目标注解
            target = getAnnotationContent(psiElement, listOf("Route", "Autowired"))

            if (target == null) {
                target = getAnnotationContent2(psiElement)
            }
            target?.let {
                val reva = findProperties(project, target, PsiElement::class.java, listOf("Route"))
                if (reva.isNotEmpty()) {
                    val builder = NavigationGutterIconBuilder.create(getIcon(target))
                    builder.setAlignment(GutterIconRenderer.Alignment.CENTER)
                    builder.setTargets(reva)
                    builder.setTooltipTitle("Navigation to target Protocol")
                    result.add(builder.createLineMarkerInfo(psiElement))
                    return@forEachIndexed
                } else {
                    val builder = NavigationGutterIconBuilder.create(getIcon(target))
                            .setAlignment(GutterIconRenderer.Alignment.CENTER)
                            .setTargets(listOf())
                            .setTooltipTitle("Navigation to none")
                    result.add(builder.createLineMarkerInfo(psiElement))
                }
                logTime("3")
                return
            }
        }
    }

    private fun getAnnotationContent2(psiElement: PsiElement): TargetContent? {
        if (psiElement.javaClass.name == "org.jetbrains.kotlin.psi.KtDotQualifiedExpression") {
            if (psiElement.text.contains("ARouter.getInstance()") && psiElement.text.contains("navigation")) {
                var anoContent = matchBuild(psiElement.text)
                return TargetContent().apply {
                    this.type = 2
                    this.content = anoContent
                }
            }
        }
        return null
    }

    /**
     * 找到 注解
     */
    private fun getAnnotationContent(element: PsiElement, targetAno: List<String>): TargetContent? {
        if (element.text == "(") {
            if (element is LeafPsiElement) {
                val pre = (element as LeafPsiElement).treeParent.prev()
                if (pre is CompositeElement) {
                    val text = recursionCompositeElementText(pre)
                    //确实是注解
                    if (targetAno.contains(text)) {
                        //接着去找注解的内容
                        val next = element.treeNext
                        if (next is CompositeElement) {
                            val ano = recursionCompositeElement(next)
                            return TargetContent().apply {
                                this.content = ano
                                this.type = 1
                                //如果注解是 Autowired 和导航一样处理
                                if (text == "Autowired") {
                                    this.type = 2
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * 找注解内容
     */
    private fun recursionCompositeElement(element: CompositeElement): String {
        //接着去找注解的内容
        var result = ""
        if (element is CompositeElement && (element.firstChildNode.text == "path" || element.firstChildNode.text == "name")) {
            val ano = element.text
            result = ano.split("=")[1].trim()
        }
        return result
    }

    private fun recursionCompositeElementText(element: CompositeElement): String {

        val e = element.firstChildNode
        if (e is CompositeElement) {
            return recursionCompositeElementText(e)
        } else if (e is LeafPsiElement) {
            val cotent = e.text
            return cotent
        } else {
            return ""
        }
    }

    private fun <T : PsiElement> findProperties(project: Project, target: TargetContent, clazz: Class<T>, key: List<String>): List<T> {
        var result: MutableList<T> = mutableListOf()
        val scopes = GlobalSearchScope.projectScope(project)
        val virtualFiles = FilenameIndex.getAllFilesByExt(project, "kt", scopes)
        println("virtualFiles-size= ${virtualFiles.size}")
        if (virtualFiles.isNullOrEmpty()) {
            return listOf()
        }
        for (virtualFile in virtualFiles) {
            //当前的这个,不加自己
            if (virtualFile.canonicalPath == filePath) {
                continue
            }
            val psiFile: PsiFile? = PsiManager.getInstance(project).findFile(virtualFile)
            psiFile ?: return listOf()

            val properties = PsiTreeUtil.findChildrenOfType(psiFile, clazz) ?: listOf()
            val list = properties.toMutableList()

            //src 是导航器->来查找
            if (target.type == 2) {
                list.forEach {
                    if (it.javaClass.simpleName == "KtAnnotationEntry" && it.text.contains(target.content
                                    ?: "") && it.text.contains("Route")) {
                        result.add(it)
                    }
                }
            } else if (target.content?.startsWith("\"") == true) {
                list.forEachIndexed { index, property ->
                    println("2级查找=${property.javaClass.simpleName}  ${property.text}")
                    //KtDotQualifiedExpression
                    if (target.content?.startsWith("\"") == true && property.javaClass.simpleName == "KtStringTemplateExpression") {
                        if (property.text == target.content) {
                            result.add(property)
                        }
                    }
                }
            } else {
                list.forEachIndexed { index, property ->
                    if (property.javaClass.simpleName == "KtDotQualifiedExpression") {
                        if (property.text == target.content) {
                            println("--property=${property.javaClass.simpleName} ${property.text} ")
                            result.add(property)
                        }
                    }
                }
            }
        }
        return result
    }

    fun getIconGoto(): Icon {
        return IconLoader.getIcon("/icon/icon_direct_to.png")
    }

    fun getIconFrom(): Icon {
        return IconLoader.getIcon("/icon/icon_direct_from.png")
    }

    fun getIcon(target: TargetContent): Icon {
        if (target.type == 2) {
            return getIconGoto()
        }
        return getIconFrom()
    }

    fun getMarkWarnIcon(): Icon {
        return IconLoader.getIcon("/icon/icon_firect_warn.png")
    }

    fun logTime(tag: String = "") {
        println("$tag timeOff = ${System.currentTimeMillis() - time}")
    }


    fun matchBuild(srcStr: String): String {

        // 匹配规则
        val reg = "build\\((.*?)\\)"
        val pattern = Pattern.compile(reg)

        // 内容 与 匹配规则 的测试
        val matcher = pattern.matcher(srcStr)
        if (matcher.find()) {
            // 不包含前后的两个字符
            return matcher.group(1)
        } else {
            return "";
        }
    }

    data class TargetContent(
            var content: String? = "",
            /**
             * 类文件头部的注解是1
             * 导航器的是2
             */
            var type: Int = 0
    )
}

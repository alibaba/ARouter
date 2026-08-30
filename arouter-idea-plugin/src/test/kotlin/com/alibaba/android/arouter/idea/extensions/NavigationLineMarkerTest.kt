package com.alibaba.android.arouter.idea.extensions

import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import java.nio.file.Path

class NavigationLineMarkerTest : JavaCodeInsightFixtureTestCase() {
    private val provider = NavigationLineMarker()

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        super.tuneFixture(moduleBuilder)
        val libraryJar = testApiJar()
        moduleBuilder.addLibraryJars(
            "ARouter API",
            libraryJar.parent.toString(),
            libraryJar.fileName.toString()
        )
    }

    fun testJavaBuildCallsResolveLiteralAndConstantPaths() {
        addJavaRoute("JavaTarget", "/java/target")
        val file = myFixture.configureByText(
            "JavaCaller.java",
            """
            import com.alibaba.android.arouter.launcher.ARouter;

            final class JavaCaller {
                static final String TARGET = "/java/target";

                void navigate() {
                    ARouter.getInstance().build("/java/target");
                    ARouter.getInstance().build(TARGET, "java");
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("/java/target", "/java/target"), navigationPaths(file))
        assertEquals(2, markers(file).size)
    }

    fun testARouterApiStubsAreResolvedFromTheDependencyScope() {
        val routeAnnotation = myFixture.findClass(NavigationLineMarker.ROUTE_ANNOTATION_NAME)
        val routeFile = routeAnnotation.containingFile.virtualFile

        assertFalse(GlobalSearchScope.projectScope(project).contains(routeFile))
        assertTrue(GlobalSearchScope.allScope(project).contains(routeFile))
    }

    fun testKotlinBuildCallsResolveLiteralAndConstPaths() {
        addKotlinRoute("KotlinTarget", "/kotlin/target")
        val file = myFixture.configureByText(
            "KotlinCaller.kt",
            """
            import com.alibaba.android.arouter.launcher.ARouter

            private const val TARGET = "/kotlin/target"

            fun navigate() {
                ARouter.getInstance().build("/kotlin/target")
                ARouter.getInstance().build(TARGET, "kotlin")
            }
            """.trimIndent()
        )

        assertEquals(listOf("/kotlin/target", "/kotlin/target"), navigationPaths(file))
        assertEquals(2, markers(file).size)
    }

    fun testRouteTargetSearchSupportsJavaAndKotlinDestinations() {
        addJavaRoute("JavaTarget", "/target/java")
        addKotlinRoute("KotlinTarget", "/target/kotlin")

        assertEquals("JavaTarget.java", targetFile("/target/java"))
        assertEquals("KotlinTarget.kt", targetFile("/target/kotlin"))
    }

    fun testUnrelatedARouterClassAndDynamicPathDoNotProduceMarkers() {
        val file = myFixture.configureByText(
            "Unrelated.kt",
            """
            package unrelated

            class ARouter {
                fun build(path: String) = path
            }

            fun navigate(router: ARouter, path: String) {
                router.build("/not/arouter")
                com.alibaba.android.arouter.launcher.ARouter.getInstance().build(path)
            }
            """.trimIndent()
        )

        assertEmpty(navigationPaths(file))
        assertEmpty(markers(file))
    }

    fun testRegisteredProviderCreatesClickableJavaAndKotlinGutters() {
        addJavaRoute("RegisteredJavaTarget", "/registered/java")
        addKotlinRoute("RegisteredKotlinTarget", "/registered/kotlin")

        val javaGutter = registeredRouteGutter(
            "RegisteredJavaCaller.java",
            """
            import com.alibaba.android.arouter.launcher.ARouter;

            final class RegisteredJavaCaller {
                void navigate() {
                    ARouter.getInstance().build("/registered/java");
                }
            }
            """.trimIndent()
        )
        assertEquals("RegisteredJavaTarget.java", targetFile("/registered/java"))
        assertTrue(javaGutter.isNavigateAction)
        assertNotNull(javaGutter.clickAction)

        val kotlinGutter = registeredRouteGutter(
            "RegisteredKotlinCaller.kt",
            """
            import com.alibaba.android.arouter.launcher.ARouter

            fun navigate() {
                ARouter.getInstance().build("/registered/kotlin")
            }
            """.trimIndent()
        )
        assertEquals("RegisteredKotlinTarget.kt", targetFile("/registered/kotlin"))
        assertTrue(kotlinGutter.isNavigateAction)
        assertNotNull(kotlinGutter.clickAction)
    }

    private fun addJavaRoute(className: String, path: String) {
        myFixture.addFileToProject(
            "fixture/$className.java",
            """
            package fixture;

            import com.alibaba.android.arouter.facade.annotation.Route;

            @Route(path = "$path")
            public final class $className {}
            """.trimIndent()
        )
    }

    private fun addKotlinRoute(className: String, path: String) {
        myFixture.addFileToProject(
            "$className.kt",
            """
            package fixture

            import com.alibaba.android.arouter.facade.annotation.Route

            @Route(path = "$path")
            class $className
            """.trimIndent()
        )
    }

    private fun navigationPaths(file: PsiFile): List<String> {
        return elements(file).mapNotNull(provider::navigationPath)
    }

    private fun markers(file: PsiFile): List<PsiElement> {
        return elements(file).filter { provider.createMarker(it) != null }
    }

    private fun targetFile(path: String): String {
        val targets = RouteTargetResolver.findTargets(project, path)
        assertSize(1, targets)
        return targets.single().containingFile.name
    }

    private fun registeredRouteGutter(
        fileName: String,
        source: String
    ): GutterIconRenderer {
        myFixture.configureByText(fileName, source)
        myFixture.doHighlighting()
        val routeGutters = myFixture.findAllGutters()
            .filter { it.tooltipText == "Navigate to ARouter destination" }
        assertSize(1, routeGutters)
        return routeGutters.single() as GutterIconRenderer
    }

    private fun elements(file: PsiFile): List<PsiElement> {
        return PsiTreeUtil.collectElements(file) { true }.toList()
    }

    private fun testApiJar(): Path {
        return Path.of(
            requireNotNull(System.getProperty("arouter.test.api.jar")) {
                "Missing arouter.test.api.jar test property"
            }
        )
    }
}

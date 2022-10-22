package com.alibaba.android.arouter.compiler.ksp

import com.alibaba.android.arouter.compiler.ksp.utils.*
import com.alibaba.android.arouter.compiler.utils.Consts
import com.alibaba.android.arouter.facade.annotation.Interceptor
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import java.util.*

@KotlinPoetKspPreview
class InterceptorSymbolProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return InterceptorSymbolProcessor(
            KSPLoggerWrapper(environment.logger),
            environment.codeGenerator, environment.options
        )
    }

    class InterceptorSymbolProcessor(
        private val logger: KSPLoggerWrapper,
        private val codeGenerator: CodeGenerator,
        options: Map<String, String>
    ) : SymbolProcessor {
        @Suppress("SpellCheckingInspection")
        companion object {
            val INTERCEPTOR_CLASS_NAME = Interceptor::class.qualifiedName!!

            private val IINTERCEPTOR_GROUP_CLASSNAME =
                Consts.IINTERCEPTOR_GROUP.quantifyNameToClassName()
        }

        private val moduleName = options.findModuleName(logger)


        override fun process(resolver: Resolver): List<KSAnnotated> {
            val symbol = resolver.getSymbolsWithAnnotation(INTERCEPTOR_CLASS_NAME)

            val elements = symbol
                .filterIsInstance<KSClassDeclaration>()
                .toList()

            if (elements.isNotEmpty()) {
                logger.info(">>> InterceptorSymbolProcessor init. <<<")
                try {
                    parseInterceptor(elements)
                } catch (e: Exception) {
                    logger.exception(e)
                }
            }
            return emptyList()
        }

        private fun parseInterceptor(elements: List<KSClassDeclaration>) {
            logger.info(">>> Found interceptors, size is " + elements.size + " <<<")

            val interceptors: TreeMap<Int, KSClassDeclaration> = TreeMap()
            for (element in elements) {
                if (verify(element)) {
                    // Check the interceptor meta
                    logger.info("A interceptor verify over, its ${element.qualifiedName?.asString()}")

                    val interceptor = element.findAnnotationWithType<Interceptor>()!!
                    // Avoid has same priority @Interceptor
                    interceptors[interceptor.priority]?.let {
                        throw IllegalArgumentException(
                            String.format(
                                Locale.getDefault(),
                                "More than one interceptors use same priority [%d], They are [%s] and [%s].",
                                interceptor.priority,
                                it.simpleName.asString(),
                                element.simpleName.asString()
                            )
                        )
                    }
                    interceptors[interceptor.priority] = element
                } else {
                    logger.error("A interceptor verify failed, its " + element.qualifiedName?.asString())
                }
            }
            generateInterceptorFile(interceptors)
        }

        private fun generateInterceptorFile(interceptors: TreeMap<Int, KSClassDeclaration>) {
            val parameterName = "interceptor"

            /** interceptor: MutableMap<Int, Class<out IInterceptor>>? */
            val parameterSpec = ParameterSpec.builder(
                parameterName,
                MUTABLE_MAP.parameterizedBy(
                    INT,
                    Class::class.asClassName().parameterizedBy(
                        WildcardTypeName.producerOf(
                            Consts.IINTERCEPTOR.quantifyNameToClassName()
                        )
                    )
                ).copy(nullable = true)
            ).build()

            /** override fun loadInto(providers: MutableMap<String, RouteMeta>?) */
            val loadInfoFunSpecBuilder: FunSpec.Builder = FunSpec
                .builder(Consts.METHOD_LOAD_INTO)
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(parameterSpec)
            /** if (interceptor == null) { return } **/
            loadInfoFunSpecBuilder.addStatement("if($parameterName == null) { return }")

            val dependencies = mutableSetOf<KSFile>()

            for (entry in interceptors) {
                val priority: Int = entry.key
                val interceptor: KSClassDeclaration = entry.value

                interceptor.containingFile?.let {
                    dependencies.add(it)
                }
                /** interceptor.put(priority, XxxInterceptor::class.java) */
                loadInfoFunSpecBuilder.addStatement(
                    "$parameterName.put(%L, %T::class.java)", priority, interceptor.toClassName()
                )
            }

            val interceptorClassName = Consts.NAME_OF_INTERCEPTOR + Consts.SEPARATOR + moduleName
            val file =
                FileSpec.builder(Consts.PACKAGE_OF_GENERATE_FILE, interceptorClassName)
                    .addType(
                        TypeSpec.classBuilder(
                            ClassName(Consts.PACKAGE_OF_GENERATE_FILE, interceptorClassName)
                        )
                            .addKdoc(Consts.WARNING_TIPS)
                            .addSuperinterface(IINTERCEPTOR_GROUP_CLASSNAME)
                            .addFunction(loadInfoFunSpecBuilder.build())
                            .build()
                    )
                    .build()

            file.writeTo(codeGenerator, true, dependencies)
            logger.info(">>> Interceptor group write over. <<<")
        }

        private fun verify(element: KSClassDeclaration): Boolean {
            // It must be implement the interface IInterceptor and marked with annotation Interceptor.
            return element.findAnnotationWithType<Interceptor>() != null
                    && element.isSubclassOf(Consts.IINTERCEPTOR)
        }
    }

}


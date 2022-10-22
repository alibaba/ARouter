package com.alibaba.android.arouter.compiler.ksp

import com.alibaba.android.arouter.compiler.ksp.utils.*
import com.alibaba.android.arouter.compiler.utils.Consts
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.enums.RouteType
import com.alibaba.android.arouter.facade.enums.TypeKind
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

@KotlinPoetKspPreview
class AutowiredSymbolProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AutowiredSymbolProcessor(
            KSPLoggerWrapper(environment.logger), environment.codeGenerator
        )
    }

    class AutowiredSymbolProcessor(
        private val logger: KSPLoggerWrapper,
        private val codeGenerator: CodeGenerator,
    ) : SymbolProcessor {
        @Suppress("SpellCheckingInspection")
        companion object {
            val AUTOWIRED_CLASS_NAME = Autowired::class.qualifiedName!!
            private val ISYRINGE_CLASS_NAME = Consts.ISYRINGE.quantifyNameToClassName()
            private val JSON_SERVICE_CLASS_NAME = Consts.JSON_SERVICE.quantifyNameToClassName()
            private val AROUTER_CLASS_NAME =
                ClassName("com.alibaba.android.arouter.launcher", "ARouter")
        }


        override fun process(resolver: Resolver): List<KSAnnotated> {
            val symbol = resolver.getSymbolsWithAnnotation(AUTOWIRED_CLASS_NAME)

            val elements = symbol
                .filterIsInstance<KSPropertyDeclaration>()
                .toList()

            if (elements.isNotEmpty()) {
                logger.info(">>> AutowiredSymbolProcessor init. <<<")
                try {
                    parseAutowired(elements)
                } catch (e: Exception) {
                    logger.exception(e)
                }
            }
            return emptyList()
        }

        private fun parseAutowired(elements: List<KSPropertyDeclaration>) {
            logger.info(">>> Found autowired field, start... <<<")
            generateAutowiredFiles(categories(elements))
        }

        private fun categories(elements: List<KSPropertyDeclaration>): MutableMap<KSClassDeclaration, MutableList<KSPropertyDeclaration>> {
            val parentAndChildren =
                mutableMapOf<KSClassDeclaration, MutableList<KSPropertyDeclaration>>()
            for (element in elements) {
                // Class of the member
                logger.check(element.parentDeclaration is KSClassDeclaration) {
                    "Property annotated with @Autowired 's enclosingElement(property's class) must be non-null!"
                }
                val parent = element.parentDeclaration as KSClassDeclaration

                if (element.modifiers.contains(Modifier.PRIVATE)) {
                    throw  IllegalAccessException(
                        "The inject fields CAN NOT BE 'private'!!! please check field ["
                                + element.simpleName.asString() + "] in class [" + parent.qualifiedName?.asString() + "]"
                    )
                }
                if (parentAndChildren.containsKey(parent)) {
                    parentAndChildren[parent]!!.add(element)
                } else {
                    parentAndChildren[parent] = mutableListOf(element)
                }
            }
            logger.info("@Autowired categories finished.")
            return parentAndChildren
        }

        @Suppress("SpellCheckingInspection")
        private fun generateAutowiredFiles(parentAndChildren: MutableMap<KSClassDeclaration, MutableList<KSPropertyDeclaration>>) {
            /** private var serializationService: SerializationService? = null */
            val serializationServiceProperty = PropertySpec.builder(
                "serializationService",
                JSON_SERVICE_CLASS_NAME.copy(nullable = true),
                KModifier.PRIVATE,
            ).mutable(true)
                .initializer("null")
                .build()

            /** target: Any? */
            val parameterName = "target"
            val parameterSpec = ParameterSpec.builder(
                parameterName,
                Any::class.asTypeName().copy(nullable = true)
            ).build()

            /** if(target == null) { return } */
            val returnStatement = "if($parameterName == null) { return }"

            for (entry in parentAndChildren) {
                val parent: KSClassDeclaration = entry.key
                val children: List<KSPropertyDeclaration> = entry.value
                if (children.isEmpty()) continue
                logger.info(">>> Start process " + children.size + " field in " + parent.simpleName.asString() + " ... <<<")
                /** override fun inject(target: Any?) */
                val injectMethodBuilder: FunSpec.Builder = FunSpec
                    .builder(Consts.METHOD_INJECT)
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(parameterSpec)

                injectMethodBuilder.addStatement(returnStatement)
                /** serializationService = ARouter.getInstance().navigation(SerializationService::class.java) */
                injectMethodBuilder.addStatement(
                    "serializationService = %T.getInstance().navigation(%T::class.java)",
                    AROUTER_CLASS_NAME,
                    JSON_SERVICE_CLASS_NAME
                )
                val parentClassName = parent.toClassName()
                injectMethodBuilder.addStatement(
                    "val substitute = target as %T",
                    parentClassName
                )

                val parentRouteType = parent.routeType

                /**
                 *  Judge this file generate with isolating or aggregating mode
                 *  More detail: https://kotlinlang.org/docs/ksp-incremental.html#symbolprocessorprovider-the-entry-point
                 *  */
                var aggregating = false
                // Generate method body, start inject.
                for (child in children) {
                    if (child.isSubclassOf(Consts.IPROVIDER)) { // It's provider, inject by IProvider
                        aggregating = true
                        addProviderStatement(child, injectMethodBuilder, parentClassName)
                    } else { // It's normal intent value (activity or fragment)
                        addActivityOrFragmentStatement(
                            child, injectMethodBuilder, TypeKind.values()[child.typeExchange()],
                            parentRouteType, parentClassName
                        )
                    }
                }

                val autowiredClassName = parent.simpleName.asString() + Consts.NAME_OF_AUTOWIRED
                val qualifiedName = parent.qualifiedName!!.asString()
                val packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."))

                val file =
                    FileSpec.builder(packageName, autowiredClassName)
                        .addImport("android.util", "Log") // manual import (without %T)
                        .addImport("com.alibaba.android.arouter.facade.model", "TypeWrapper")
                        .addType(
                            TypeSpec.classBuilder(ClassName(packageName, autowiredClassName))
                                .addKdoc(Consts.WARNING_TIPS)
                                .addSuperinterface(ISYRINGE_CLASS_NAME)
                                .addProperty(serializationServiceProperty)
                                .addFunction(injectMethodBuilder.build())
                                .build()
                        )
                        .build()

                // Get input source (@Autowired) which gene the output file
                val dependency = mutableSetOf<KSFile>()
                parent.containingFile?.let {
                    dependency.add(it)
                }
                file.writeTo(codeGenerator, aggregating, dependency)
                logger.info(">>> " + parent.simpleName.asString() + " has been processed, " + autowiredClassName + " has been generated. <<<")
            }
            logger.info(">>> Autowired processor stop. <<<")
        }

        /**
         * Inject Provider field, such as
         * [substitute.helloService = ARouter.getInstance().navigation(HelloService::class.java)]
         * */
        private fun addProviderStatement(
            ksPropertyDeclaration: KSPropertyDeclaration,
            injectMethodBuilder: FunSpec.Builder,
            parentClassName: ClassName
        ) {
            val annotation = ksPropertyDeclaration.findAnnotationWithType<Autowired>()!!
            val fieldName = ksPropertyDeclaration.simpleName.asString()
            val propertyType = ksPropertyDeclaration.getKotlinPoetTTypeGeneric()
            if (annotation.name.isEmpty()) { // User has not set service path, then use byType.
                injectMethodBuilder.addStatement(
                    "substitute.$fieldName = %T.getInstance().navigation(%T::class.java)",
                    AROUTER_CLASS_NAME, propertyType
                )
            } else { // use byName
                injectMethodBuilder.addStatement(
                    "substitute.$fieldName = %T.getInstance().build(%S).navigation() as %T",
                    AROUTER_CLASS_NAME, annotation.name, propertyType
                )
            }
            // Validator
            if (annotation.required) {
                injectMethodBuilder.beginControlFlow("if (substitute.$fieldName == null)")
                    .addStatement(
                        "throw RuntimeException(\"\"\"The field '$fieldName' is null, in class '%L' !\"\"\")",
                        parentClassName.simpleName
                    )
                    .endControlFlow()
            }
        }

        /**
         * Inject field for activity and fragment
         * */
        private fun addActivityOrFragmentStatement(
            ksPropertyDeclaration: KSPropertyDeclaration,
            injectMethodBuilder: FunSpec.Builder,
            type: TypeKind,
            parentRouteType: RouteType,
            parentClassName: ClassName
        ) {
            val fieldName = ksPropertyDeclaration.simpleName.asString()
            val isActivity = when (parentRouteType) {
                RouteType.ACTIVITY -> true
                RouteType.FRAGMENT -> false
                else -> {
                    throw IllegalAccessException("The field [$fieldName] need autowired from intent, its parent must be activity or fragment!")
                }
            }
            val annotation = ksPropertyDeclaration.findAnnotationWithType<Autowired>()!!
            val bundleName = annotation.name.ifEmpty { fieldName }

            val getObj: String = when (type) {
                TypeKind.BOOLEAN -> "getBoolean"
                TypeKind.BYTE -> "getByte"
                TypeKind.SHORT -> "getShort"
                TypeKind.INT -> "getInt"
                TypeKind.LONG -> "getLong"
                TypeKind.CHAR -> "getChar"
                TypeKind.FLOAT -> "getFloat"
                TypeKind.DOUBLE -> "getDouble"
                else -> ""
            }
            if (getObj.isNotEmpty()) {
                val intent = if (isActivity) "intent?.extras" else "arguments"
                val statementPrimaryFormat =
                    "substitute.%L = if (substitute.${intent} != null) {  substitute.${intent}!!.${getObj}(%S, substitute.%L) } else  {  substitute.%L }"
                injectMethodBuilder.addStatement(
                    statementPrimaryFormat, fieldName, bundleName, fieldName, fieldName
                )
            } else {
                // such as: val param = List<JailedBird> ==> %T ==> List<JailedBird>
                val parameterClassName = ksPropertyDeclaration.getKotlinPoetTTypeGeneric()

                when (type) {
                    TypeKind.STRING -> {
                        val intent = if (isActivity) "intent?.extras" else "arguments"
                        val statementPrimaryFormat =
                            "substitute.%L = if (substitute.${intent} != null) {  substitute.${intent}!!.getString(%S, substitute.%L) } else  {  substitute.%L }"
                        injectMethodBuilder.addStatement(
                            statementPrimaryFormat, fieldName, bundleName, fieldName, fieldName
                        )
                    }
                    TypeKind.SERIALIZABLE -> {
                        val statement = if (isActivity) {
                            "(substitute.intent?.getSerializableExtra(%S) as? %T)?.let { substitute.%L = it }"
                        } else {
                            "(substitute.arguments?.getSerializable(%S) as? %T)?.let { substitute.%L = it }"
                        }
                        injectMethodBuilder.addStatement(
                            statement, bundleName, parameterClassName, fieldName
                        )
                    }
                    TypeKind.PARCELABLE -> {
                        val statement = if (isActivity) {
                            "substitute.intent?.getParcelableExtra<%T>(%S)?.let { substitute.%L = it }"
                        } else {
                            "substitute.arguments?.getParcelable<%T>(%S)?.let { substitute.%L = it }"
                        }
                        injectMethodBuilder.addStatement(
                            statement, parameterClassName, bundleName, fieldName
                        )
                    }
                    TypeKind.OBJECT -> {
                        val intent = if (isActivity) "intent?.extras" else "arguments"
                        injectMethodBuilder.beginControlFlow("if(serializationService != null && substitute.${intent} != null)")
                            .addStatement(
                                "substitute.%L = serializationService!!.parseObject(substitute.${intent}!!.getString(%S), (object : TypeWrapper<%T>(){}).type)",
                                fieldName, bundleName, parameterClassName
                            )
                            .nextControlFlow("else")
                            // Kotlin-poet Notice: Long lists line wrapping makes code not compile
                            // https://github.com/square/kotlinpoet/issues/1346 , temp using """  """ to wrap long string (perhaps can optimize it)
                            .addStatement(
                                "Log.e(\"${Consts.TAG}\" , \"\"\"You want automatic inject the field '%L' in class '%L', then you should implement 'SerializationService' to support object auto inject!\"\"\")",
                                fieldName, parentClassName.simpleName
                            )
                            .endControlFlow()
                    }
                    else -> {
                        // This branch will not be reach
                        error("This branch will not be reach")
                    }
                }
                // Validator, Primitive type wont be check.
                if (annotation.required) {
                    injectMethodBuilder.beginControlFlow("if (substitute.$fieldName == null)")
                        .addStatement(
                            "Log.e(\"${Consts.TAG}\" , \"\"\"The field '%L' in class '%L' is null!\"\"\")",
                            fieldName, parentClassName.simpleName
                        )
                    injectMethodBuilder.endControlFlow()
                }
            }

        }
    }

}


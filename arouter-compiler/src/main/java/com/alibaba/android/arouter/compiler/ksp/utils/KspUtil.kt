/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(KotlinPoetKspPreview::class, KspExperimental::class)

package com.alibaba.android.arouter.compiler.ksp.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.Origin.KOTLIN
import com.google.devtools.ksp.symbol.Origin.KOTLIN_LIB
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import java.io.OutputStream

@Suppress("unused")
internal fun KSClassDeclaration.isKotlinClass(): Boolean {
    return origin == KOTLIN ||
            origin == KOTLIN_LIB ||
            isAnnotationPresent(Metadata::class)
}

internal inline fun <reified T : Annotation> KSAnnotated.findAnnotationWithType(): T? {
    return getAnnotationsByType(T::class).firstOrNull()
}

internal inline fun KSPLogger.check(condition: Boolean, message: () -> String) {
    check(condition, null, message)
}

internal inline fun KSPLogger.check(condition: Boolean, element: KSNode?, message: () -> String) {
    if (!condition) {
        error(message(), element)
    }
}

/**
 * Judge whether a class [KSClassDeclaration] is a subclass of another class [superClassName]
 * https://www.raywenderlich.com/33148161-write-a-symbol-processor-with-kotlin-symbol-processing
 * */
internal fun KSClassDeclaration.isSubclassOf(
    superClassName: String,
): Boolean {
    val superClasses = superTypes.toMutableList()
    while (superClasses.isNotEmpty()) {
        val current: KSTypeReference = superClasses.first()
        val declaration: KSDeclaration = current.resolve().declaration
        when {
            declaration is KSClassDeclaration && declaration.qualifiedName?.asString() == superClassName -> {
                return true
            }
            declaration is KSClassDeclaration -> {
                superClasses.removeAt(0)
                superClasses.addAll(0, declaration.superTypes.toList())
            }
            else -> {
                superClasses.removeAt(0)
            }
        }
    }
    return false
}

internal fun KSClassDeclaration.isSubclassOf(superClassNames: List<String>): Int {
    val superClasses = superTypes.toMutableList()
    while (superClasses.isNotEmpty()) {
        val current: KSTypeReference = superClasses.first()
        val declaration: KSDeclaration = current.resolve().declaration
        when {
            declaration is KSClassDeclaration && (superClassNames.indexOf(declaration.qualifiedName?.asString())) != -1 -> {
                return superClassNames.indexOf(declaration.qualifiedName?.asString())
            }
            declaration is KSClassDeclaration -> {
                superClasses.removeAt(0)
                superClasses.addAll(0, declaration.superTypes.toList())
            }
            else -> {
                superClasses.removeAt(0)
            }
        }
    }
    return -1
}

internal fun KSPropertyDeclaration.isSubclassOf(superClassName: String): Boolean {
    val propertyType = type.resolve().declaration
    return if (propertyType is KSClassDeclaration) {
        propertyType.isSubclassOf(superClassName)
    } else {
        false
    }
}

internal fun KSPropertyDeclaration.isSubclassOf(superClassNames: List<String>): Int {
    val propertyType = type.resolve().declaration
    return if (propertyType is KSClassDeclaration) {
        propertyType.isSubclassOf(superClassNames)
    } else {
        -1
    }
}

internal fun String.quantifyNameToClassName(): ClassName {
    val index = lastIndexOf(".")
    return ClassName(substring(0, index), substring(index + 1, length))
}

/**
 *  such: val map = Map<String, String> ==> Map<String, String> (used for kotlinpoet for %T)
 * */
internal fun KSPropertyDeclaration.getKotlinPoetTTypeGeneric(): TypeName {
    val classTypeParams = this.typeParameters.toTypeParameterResolver()
    return this.type.toTypeName(classTypeParams)
}

/**
 *  such: val map = Map<String, String> ==> Map (used for kotlinpoet for %T)
 * */
@Suppress("unused")
internal fun KSPropertyDeclaration.getKotlinPoetTType(): TypeName {
    return this.type.resolve().toTypeName()
}

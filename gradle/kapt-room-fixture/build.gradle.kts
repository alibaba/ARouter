import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.7.10"
    kotlin("kapt") version "1.7.10"
}

val annotationVersion = providers.gradleProperty("arouter.annotation.version").get()
val compilerVersion = providers.gradleProperty("arouter.compiler.version").get()

dependencies {
    compileOnly("com.alibaba:arouter-annotation:$annotationVersion")
    kapt("com.alibaba:arouter-compiler:$compilerVersion")
    kapt("androidx.room:room-compiler:2.4.3")
}

kapt {
    arguments {
        arg("AROUTER_MODULE_NAME", "kaptroomfixture")
        arg("AROUTER_GENERATE_DOC", "enable")
    }
    includeCompileClasspath = false
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

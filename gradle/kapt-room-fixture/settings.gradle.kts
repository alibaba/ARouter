import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val arouterRepository = providers.gradleProperty("arouter.repository").orNull
    ?: throw GradleException("Pass -Parouter.repository=<repo>/build/localMaven.")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri(arouterRepository) }
        google()
        mavenCentral()
    }
}

rootProject.name = "arouter-kapt-room-fixture"

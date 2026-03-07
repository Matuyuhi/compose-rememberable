pluginManagement {
    plugins {
        id("com.android.application") version "9.1.0"
        id("org.jetbrains.kotlin.jvm") version "2.2.10"
        id("org.jetbrains.kotlin.kapt") version "2.2.10"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
        id("org.jetbrains.kotlin.android") version "2.2.10"
        id("com.vanniktech.maven.publish") version "0.29.0"
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "compose-rememberable"

include(":rememberable-annotations")
include(":rememberable-runtime")
include(":rememberable-compiler")
include(":rememberable-gradle-plugin")

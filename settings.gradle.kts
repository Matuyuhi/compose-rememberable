pluginManagement {
    plugins {
        id("com.matuyuhi.rememberable") version "0.1.6" apply false
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
include(":rememberable-compiler")
include(":rememberable-gradle-plugin")
if (System.getenv("CI") == null) {
    include(":sample-app")
    includeBuild("rememberable-intellij")
}

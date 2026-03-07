pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "compose-rememberable"

include(":rememberable-annotations")
include(":rememberable-runtime")
include(":rememberable-compiler")
include(":rememberable-gradle-plugin")

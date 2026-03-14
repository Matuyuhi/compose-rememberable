plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.maven.publish)
    `java-gradle-plugin`
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.kotlin.gradle.plugin)
}

buildConfig {
    packageName("com.matuyuhi.rememberable.gradle")

    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}.rememberable\"")

    val pluginProject = project(":rememberable-compiler")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${pluginProject.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${pluginProject.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${pluginProject.version}\"")

    val annotationsProject = project(":rememberable-annotations")
    buildConfigField(
        type = "String",
        name = "ANNOTATIONS_LIBRARY_COORDINATES",
        expression = "\"${annotationsProject.group}:${annotationsProject.name}:${annotationsProject.version}\"",
    )
}

gradlePlugin {
    plugins {
        create("rememberable") {
            id = "${rootProject.group}.rememberable"
            displayName = "Rememberable"
            description = "Kotlin compiler plugin for Compose state persistence"
            implementationClass = "com.matuyuhi.rememberable.gradle.RememberableGradlePlugin"
        }
    }
}

mavenPublishing {
    pom {
        url.set("https://github.com/matuyuhi/compose-rememberable")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("matuyuhi")
                name.set("matuyuhi")
            }
        }
        scm {
            url.set("https://github.com/matuyuhi/compose-rememberable")
            connection.set("scm:git:git://github.com/matuyuhi/compose-rememberable.git")
            developerConnection.set("scm:git:ssh://github.com:matuyuhi/compose-rememberable.git")
        }
    }
}

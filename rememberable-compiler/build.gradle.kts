import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.maven.publish)
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
            "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI",
        )
    }
}

buildConfig {
    useKotlinOutput {
        internalVisibility = true
    }
    packageName("com.matuyuhi.rememberable.compiler")
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.group}.rememberable\"")
}

dependencies {
    compileOnly(libs.kotlin.compiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.compiler)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.junit)
    testImplementation(project(":rememberable-annotations"))
}

tasks.test {
    useJUnit()
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set("Rememberable Compiler")
        description.set("Kotlin compiler plugin that generates Saver for rememberSaveable from @Rememberable annotated classes")
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

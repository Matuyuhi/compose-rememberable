plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI",
            "org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi",
        )
    }
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)

    kapt(libs.auto.service)
    compileOnly(libs.auto.service.annotations)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.kctfork.core)
    testImplementation(libs.junit)

    testImplementation(project(":rememberable-annotations"))
}

tasks.test {
    useJUnit()
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

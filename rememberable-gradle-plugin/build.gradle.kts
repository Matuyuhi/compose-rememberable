plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.29.0"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        create("rememberable") {
            id = "com.matuyuhi.rememberable"
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

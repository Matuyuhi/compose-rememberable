plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
}

gradlePlugin {
    plugins {
        create("rememberable") {
            id = "io.github.matuyuhi.rememberable"
            implementationClass = "io.github.matuyuhi.rememberable.gradle.RememberableGradlePlugin"
        }
    }
}

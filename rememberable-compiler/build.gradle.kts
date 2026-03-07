plugins {
    kotlin("jvm")
    kotlin("kapt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")

    kapt("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")
    testImplementation("dev.zacsweers.kctfork:core:0.7.0")
    testImplementation("junit:junit:4.13.2")

    testImplementation(project(":rememberable-annotations"))
}

tasks.test {
    useJUnit()
}

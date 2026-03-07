plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

// Android and Compose dependencies are provided by the consuming project.
// This module has no external dependencies; it uses compileOnly stubs.

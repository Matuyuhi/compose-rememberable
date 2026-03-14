plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.intellij.platform") version "2.13.0"
}

val rememberableVersion = "0.1.6"

group = "com.matuyuhi"
version = rememberableVersion

repositories {
    mavenCentral()
    mavenLocal()
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.3")
        bundledPlugin("org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
    }

    implementation("com.matuyuhi:rememberable-compiler:$rememberableVersion")
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChainFile = file("certificate/chain.crt").takeIf { it.exists() }
        privateKeyFile = file("certificate/private.pem").takeIf { it.exists() }
        password = providers.environmentVariable("INTELLIJ_PLUGIN_SIGNING_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("INTELLIJ_MARKETPLACE_TOKEN")
    }
}

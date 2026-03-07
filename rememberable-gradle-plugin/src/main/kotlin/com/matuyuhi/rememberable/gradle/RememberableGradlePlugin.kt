package com.matuyuhi.rememberable.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RememberableGradlePlugin : Plugin<Project>, KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        // Add annotation dependency
        target.dependencies.add("implementation", "com.matuyuhi:rememberable-annotations:0.1.0")
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "com.matuyuhi.rememberable"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.matuyuhi",
        artifactId = "rememberable-compiler",
        version = "0.1.0",
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }
}

package com.matuyuhi.rememberable.compiler

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class RememberableFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::RememberableFirDeclarationGenerationExtension
    }
}

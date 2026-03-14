package com.matuyuhi.rememberable.compiler

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class RememberableFirDeclarationGenerationExtension(session: FirSession) :
    FirDeclarationGenerationExtension(session) {

    companion object {
        private val REMEMBERABLE_FQ_NAME = FqName("com.matuyuhi.rememberable.Rememberable")
        private val SAVER_NAME = Name.identifier("Saver")
        private val SAVER_CLASS_ID = ClassId.topLevel(FqName("androidx.compose.runtime.saveable.Saver"))
        private val BUNDLE_CLASS_ID = ClassId.topLevel(FqName("android.os.Bundle"))
    }

    private val predicate = LookupPredicate.create { annotated(REMEMBERABLE_FQ_NAME) }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    private fun isRememberableClass(classSymbol: FirClassSymbol<*>): Boolean {
        return session.predicateBasedProvider.matches(predicate, classSymbol)
    }

    private fun isCompanionOfRememberable(classSymbol: FirClassSymbol<*>): Boolean {
        if (!classSymbol.rawStatus.isCompanion) return false
        val outerClassId = classSymbol.classId.outerClassId ?: return false
        val outerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
            as? FirRegularClassSymbol ?: return false
        return isRememberableClass(outerSymbol)
    }

    @OptIn(SymbolInternals::class)
    private fun hasSourceCompanion(classSymbol: FirClassSymbol<*>): Boolean {
        val firClass = (classSymbol as? FirRegularClassSymbol)?.fir ?: return false
        return firClass.companionObjectSymbol != null
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
        if (!isRememberableClass(classSymbol)) return emptySet()
        if (hasSourceCompanion(classSymbol)) return emptySet()
        return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> {
        if (!isCompanionOfRememberable(classSymbol)) return emptySet()
        return setOf(SAVER_NAME, SpecialNames.INIT)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        if (!isRememberableClass(owner)) return null
        return createCompanionObject(owner, RememberablePluginKey).symbol
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val owner = context.owner
        if (!isCompanionOfRememberable(owner)) return emptyList()
        return listOf(createConstructor(owner, RememberablePluginKey, isPrimary = true).symbol)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> {
        val owner = context?.owner ?: return emptyList()
        if (callableId.callableName != SAVER_NAME) return emptyList()
        if (!isCompanionOfRememberable(owner)) return emptyList()

        val outerClassId = owner.classId.outerClassId ?: return emptyList()

        val outerType = outerClassId.createConeType(session)
        val bundleType = BUNDLE_CLASS_ID.createConeType(session)
        val saverType = SAVER_CLASS_ID.createConeType(session, arrayOf(outerType, bundleType))

        val property = createMemberProperty(
            owner,
            RememberablePluginKey,
            SAVER_NAME,
            saverType,
        )
        return listOf(property.symbol)
    }
}

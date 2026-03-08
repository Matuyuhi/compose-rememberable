package com.matuyuhi.rememberable.compiler

import org.jetbrains.kotlin.DeprecatedForRemovalCompilerApi
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class RememberableIrTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

    private val rememberableAnnotationFqName = FqName("com.matuyuhi.rememberable.Rememberable")
    private val bundleClassId = ClassId.topLevel(FqName("android.os.Bundle"))

    override fun visitClass(declaration: IrClass): IrStatement {
        val result = super.visitClass(declaration)

        if (!declaration.hasAnnotation(rememberableAnnotationFqName)) {
            return result
        }

        val companion = declaration.companionObject() ?: return result

        // Fill constructor body for FIR-generated companion objects
        fillCompanionConstructorIfNeeded(companion)

        val saverProperty = companion.properties.find { it.name.asString() == "Saver" } ?: return result

        val backingField = saverProperty.backingField ?: return result
        if (backingField.initializer != null) return result

        fillSaverBody(declaration, companion, saverProperty, backingField)

        return result
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun fillCompanionConstructorIfNeeded(companion: IrClass) {
        val constructor = companion.constructors.firstOrNull { it.isPrimary } ?: return
        if (constructor.body != null) return

        constructor.body = DeclarationIrBuilder(pluginContext, constructor.symbol).irBlockBody {
            +irDelegatingConstructorCall(
                pluginContext.irBuiltIns.anyClass.owner.constructors.single()
            )
            +IrInstanceInitializerCallImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                companion.symbol,
                pluginContext.irBuiltIns.unitType,
            )
        }
    }

    private fun fillSaverBody(
        targetClass: IrClass,
        companion: IrClass,
        saverProperty: IrProperty,
        backingField: IrField,
    ) {
        val bundleClassSymbol = pluginContext.referenceClass(bundleClassId) ?: return

        val saverFactoryFunction = pluginContext.referenceFunctions(
            CallableId(FqName("androidx.compose.runtime.saveable"), Name.identifier("Saver"))
        ).firstOrNull() ?: return

        backingField.initializer = pluginContext.irFactory.createExpressionBody(
            buildSaverExpression(
                backingField,
                targetClass,
                bundleClassSymbol,
                saverFactoryFunction.owner,
            )
        )

        saverProperty.getter?.let { getter ->
            getter.body = DeclarationIrBuilder(pluginContext, getter.symbol).irBlockBody {
                +irReturn(irGetField(irGet(getter.dispatchReceiverParameter!!), backingField))
            }
        }
    }

    @OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
    private fun buildSaverExpression(
        field: IrField,
        targetClass: IrClass,
        bundleClassSymbol: IrClassSymbol,
        saverFunction: IrSimpleFunction,
    ): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, field.symbol)

        val bundleConstructor = bundleClassSymbol.owner.constructors
            .firstOrNull { it.valueParameters.isEmpty() }
            ?: error("Bundle() constructor not found")

        val primaryConstructor = targetClass.primaryConstructor
            ?: error("Primary constructor not found for ${targetClass.name}")
        val constructorParamNames = primaryConstructor.valueParameters.map { it.name }.toSet()
        val properties = targetClass.properties
            .filter { it.name in constructorParamNames && !it.isExternal }
            .toList()

        return builder.irCall(saverFunction.symbol).apply {
            putTypeArgument(0, targetClass.defaultType)
            putTypeArgument(1, bundleClassSymbol.owner.defaultType)

            putValueArgument(0, builder.buildSaveLambda(
                targetClass, bundleClassSymbol, bundleConstructor, properties
            ))

            putValueArgument(1, builder.buildRestoreLambda(
                targetClass, bundleClassSymbol, primaryConstructor, properties
            ))
        }
    }

    private fun createValueParameter(
        parent: IrFunction,
        name: String,
        index: Int,
        type: IrType,
    ): IrValueParameter {
        return pluginContext.irFactory.createValueParameter(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = IrDeclarationOrigin.DEFINED,
            kind = IrParameterKind.Regular,
            name = Name.identifier(name),
            type = type,
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(),
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        ).apply {
            this.parent = parent
        }
    }

    private data class BundleMethodNames(val put: String, val get: String)

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun bundleMethodsForType(type: IrType): BundleMethodNames {
        val builtIns = pluginContext.irBuiltIns
        return when {
            type.isInt() -> BundleMethodNames("putInt", "getInt")
            type.isLong() -> BundleMethodNames("putLong", "getLong")
            type.isFloat() -> BundleMethodNames("putFloat", "getFloat")
            type.isDouble() -> BundleMethodNames("putDouble", "getDouble")
            type.isBoolean() -> BundleMethodNames("putBoolean", "getBoolean")
            type.isString() || type.isNullableString() -> BundleMethodNames("putString", "getString")
            else -> BundleMethodNames("putString", "getString") // fallback: toString
        }
    }

    private fun IrType.isString(): Boolean {
        return isClassType(pluginContext.irBuiltIns.stringType.classFqName!!, false)
    }

    private fun IrType.isNullableString(): Boolean {
        return isClassType(pluginContext.irBuiltIns.stringType.classFqName!!, true)
    }

    private fun IrType.isClassType(fqName: FqName, nullable: Boolean): Boolean {
        if (this !is IrSimpleType) return false
        if (this.isMarkedNullable() != nullable) return false
        return this.classFqName == fqName
    }

    private fun needsToStringConversion(type: IrType): Boolean {
        return !type.isInt() && !type.isLong() && !type.isFloat() &&
            !type.isDouble() && !type.isBoolean() && !type.isString() &&
            !type.isNullableString()
    }

    @OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
    private fun DeclarationIrBuilder.buildSaveLambda(
        targetClass: IrClass,
        bundleClassSymbol: IrClassSymbol,
        bundleConstructor: IrConstructor,
        properties: List<IrProperty>,
    ): IrExpression {
        val irFactory = pluginContext.irFactory
        val localParent = this.scope.getLocalDeclarationParent()

        val saveLambda = irFactory.buildFun {
            name = Name.special("<anonymous>")
            returnType = bundleClassSymbol.owner.defaultType.makeNullable()
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }

        saveLambda.parent = localParent

        val saveScopeParam = createValueParameter(saveLambda, "saveScopeReceiver", 0, pluginContext.irBuiltIns.anyNType)
        val valueParam = createValueParameter(saveLambda, "value", 1, targetClass.defaultType)

        saveLambda.valueParameters = listOf(saveScopeParam, valueParam)

        saveLambda.body = DeclarationIrBuilder(pluginContext, saveLambda.symbol).irBlockBody {
            val bundleVar = irTemporary(irCallConstructor(bundleConstructor.symbol, emptyList()))

            properties.forEach { property ->
                val propertyType = property.backingField!!.type
                val methods = bundleMethodsForType(propertyType)

                val putFunction = bundleClassSymbol.owner.functions
                    .firstOrNull { func ->
                        func.name.asString() == methods.put &&
                            func.valueParameters.size == 2
                    } ?: error("Bundle.${methods.put} not found")

                val fieldValue = irGetField(irGet(valueParam), property.backingField!!)

                val valueArg = if (needsToStringConversion(propertyType)) {
                    // Call toString() for unsupported types
                    val toStringFunc = propertyType.classOrNull!!.owner.functions
                        .firstOrNull { it.name.asString() == "toString" && it.valueParameters.isEmpty() }
                        ?: pluginContext.irBuiltIns.anyClass.owner.functions
                            .first { it.name.asString() == "toString" && it.valueParameters.isEmpty() }
                    irCall(toStringFunc.symbol).apply {
                        dispatchReceiver = fieldValue
                    }
                } else {
                    fieldValue
                }

                +irCall(putFunction.symbol).apply {
                    dispatchReceiver = irGet(bundleVar)
                    putValueArgument(0, irString(property.name.asString()))
                    putValueArgument(1, valueArg)
                }
            }

            +irReturn(irGet(bundleVar))
        }

        val saveLambdaType = pluginContext.irBuiltIns.functionN(2).typeWith(
            pluginContext.irBuiltIns.anyNType,
            targetClass.defaultType,
            bundleClassSymbol.owner.defaultType.makeNullable(),
        )

        return IrFunctionExpressionImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = saveLambdaType,
            function = saveLambda,
            origin = IrStatementOrigin.LAMBDA,
        )
    }

    @OptIn(DeprecatedForRemovalCompilerApi::class, UnsafeDuringIrConstructionAPI::class)
    private fun DeclarationIrBuilder.buildRestoreLambda(
        targetClass: IrClass,
        bundleClassSymbol: IrClassSymbol,
        primaryConstructor: IrConstructor,
        properties: List<IrProperty>,
    ): IrExpression {
        val irFactory = pluginContext.irFactory
        val localParent = this.scope.getLocalDeclarationParent()

        val restoreLambda = irFactory.buildFun {
            name = Name.special("<anonymous>")
            returnType = targetClass.defaultType.makeNullable()
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }

        restoreLambda.parent = localParent

        val bundleParam = createValueParameter(restoreLambda, "bundle", 0, bundleClassSymbol.owner.defaultType)

        restoreLambda.valueParameters = listOf(bundleParam)

        restoreLambda.body = DeclarationIrBuilder(pluginContext, restoreLambda.symbol).irBlockBody {
            val constructorArgs = properties.map { property ->
                val propertyType = property.backingField!!.type
                val methods = bundleMethodsForType(propertyType)

                val getFunction = bundleClassSymbol.owner.functions
                    .firstOrNull { func ->
                        func.name.asString() == methods.get &&
                            func.valueParameters.size == 2
                    } ?: error("Bundle.${methods.get} not found")

                val defaultValue: IrExpression = when {
                    propertyType.isInt() -> irInt(0)
                    propertyType.isLong() -> irLong(0L)
                    propertyType.isFloat() -> IrConstImpl.float(UNDEFINED_OFFSET, UNDEFINED_OFFSET, pluginContext.irBuiltIns.floatType, 0f)
                    propertyType.isDouble() -> IrConstImpl.double(UNDEFINED_OFFSET, UNDEFINED_OFFSET, pluginContext.irBuiltIns.doubleType, 0.0)
                    propertyType.isBoolean() -> irFalse()
                    else -> irString("")
                }

                irCall(getFunction.symbol).apply {
                    dispatchReceiver = irGet(bundleParam)
                    putValueArgument(0, irString(property.name.asString()))
                    putValueArgument(1, defaultValue)
                }
            }

            +irReturn(irCallConstructor(primaryConstructor.symbol, emptyList()).apply {
                constructorArgs.forEachIndexed { i, arg ->
                    putValueArgument(i, arg)
                }
            })
        }

        val restoreLambdaType = pluginContext.irBuiltIns.functionN(1).typeWith(
            bundleClassSymbol.owner.defaultType,
            targetClass.defaultType.makeNullable(),
        )

        return IrFunctionExpressionImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = restoreLambdaType,
            function = restoreLambda,
            origin = IrStatementOrigin.LAMBDA,
        )
    }
}

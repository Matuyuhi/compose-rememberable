package com.matuyuhi.rememberable.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
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
    private val saverClassId = ClassId.topLevel(FqName("androidx.compose.runtime.saveable.Saver"))

    override fun visitClass(declaration: IrClass): IrStatement {
        // Process children first
        val result = super.visitClass(declaration)

        // Check if class has @Rememberable annotation
        if (!declaration.hasAnnotation(rememberableAnnotationFqName)) {
            return result
        }

        // Generate Saver property in companion object
        generateSaverProperty(declaration)

        return result
    }

    private fun generateSaverProperty(targetClass: IrClass) {
        // Find or create companion object
        val companion = targetClass.companionObject() ?: createCompanionObject(targetClass)

        // Check if Saver property already exists
        if (companion.declarations.any { it is IrProperty && it.name.asString() == "Saver" }) {
            return
        }

        val bundleClassSymbol = pluginContext.referenceClass(bundleClassId) ?: return
        val saverClassSymbol = pluginContext.referenceClass(saverClassId) ?: return

        // Find Saver() factory function
        val saverFactoryFunction = pluginContext.referenceFunctions(
            CallableId(FqName("androidx.compose.runtime.saveable"), Name.identifier("Saver"))
        ).firstOrNull() ?: return

        // Build Saver type: Saver<TargetClass, Bundle>
        val saverType = saverClassSymbol.typeWith(
            targetClass.defaultType,
            bundleClassSymbol.defaultType,
        )

        val irFactory = pluginContext.irFactory

        // Create property using builder DSL
        val saverProperty = irFactory.buildProperty {
            name = Name.identifier("Saver")
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            isVar = false
        }.apply {
            parent = companion
        }

        // Create backing field using builder DSL
        val backingField = irFactory.buildField {
            name = Name.identifier("Saver")
            type = saverType
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
            isStatic = false
            origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
        }.apply {
            parent = companion
            correspondingPropertySymbol = saverProperty.symbol
            initializer = irFactory.createExpressionBody(
                buildSaverExpression(
                    pluginContext,
                    this,
                    targetClass,
                    bundleClassSymbol,
                    saverFactoryFunction.owner,
                )
            )
        }

        saverProperty.backingField = backingField

        // Create getter using builder DSL
        val getter = irFactory.buildFun {
            name = Name.special("<get-Saver>")
            returnType = saverType
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
            origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        }.apply {
            parent = companion
            correspondingPropertySymbol = saverProperty.symbol
            dispatchReceiverParameter = companion.thisReceiver?.copyTo(this)
            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                +irReturn(irGetField(irGet(dispatchReceiverParameter!!), backingField))
            }
        }

        saverProperty.getter = getter
        companion.declarations.add(saverProperty)
    }

    private fun buildSaverExpression(
        pluginContext: IrPluginContext,
        field: IrField,
        targetClass: IrClass,
        bundleClassSymbol: IrClassSymbol,
        saverFunction: IrSimpleFunction,
    ): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, field.symbol)

        // Bundle constructor
        val bundleConstructor = bundleClassSymbol.owner.constructors
            .firstOrNull { it.valueParameters.isEmpty() }
            ?: error("Bundle() constructor not found")

        // Get all properties
        val properties = targetClass.properties.filter { it.isVar && !it.isExternal }.toList()

        return builder.irCall(saverFunction).apply {
            // Type arguments: Saver<TargetClass, Bundle>
            putTypeArgument(0, targetClass.defaultType)
            putTypeArgument(1, bundleClassSymbol.defaultType)

            // save lambda
            putValueArgument(0, builder.buildSaveLambda(
                pluginContext, targetClass, bundleClassSymbol,
                bundleConstructor, properties
            ))

            // restore lambda
            putValueArgument(1, builder.buildRestoreLambda(
                pluginContext, targetClass, bundleClassSymbol, properties
            ))
        }
    }

    private fun DeclarationIrBuilder.buildSaveLambda(
        pluginContext: IrPluginContext,
        targetClass: IrClass,
        bundleClassSymbol: IrClassSymbol,
        bundleConstructor: IrConstructor,
        properties: List<IrProperty>,
    ): IrExpression {
        val irFactory = pluginContext.irFactory

        val saveLambda = irFactory.buildFun {
            name = Name.special("<anonymous>")
            returnType = bundleClassSymbol.defaultType.makeNullable()
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            parent = this@buildSaveLambda.scope.getLocalDeclarationParent()

            val saveScopeParam = addValueParameter(
                "saveScopeReceiver",
                pluginContext.irBuiltIns.anyNType,
            )

            val valueParam = addValueParameter(
                "value",
                targetClass.defaultType,
            )

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                val bundleVar = irTemporary(irCallConstructor(bundleConstructor.symbol, emptyList()))

                // Save each property to Bundle
                properties.forEach { property ->
                    val putFunction = bundleClassSymbol.owner.functions
                        .firstOrNull { func ->
                            func.name.asString() == "putString" &&
                                func.valueParameters.size == 2
                        } ?: error("Bundle.putString not found")

                    +irCall(putFunction).apply {
                        dispatchReceiver = irGet(bundleVar)
                        putValueArgument(0, irString(property.name.asString()))
                        putValueArgument(1, irGetField(irGet(valueParam), property.backingField!!))
                    }
                }

                +irReturn(irGet(bundleVar))
            }
        }

        val saveLambdaType = pluginContext.irBuiltIns.functionN(2).typeWith(
            pluginContext.irBuiltIns.anyNType,
            targetClass.defaultType,
            bundleClassSymbol.defaultType.makeNullable(),
        )

        return irBlock(resultType = saveLambdaType) {
            +saveLambda
            +irCallable(
                callableId = CallableId(saveLambda.symbol.ownerSymbol.name, saveLambda.symbol.name),
                type = saveLambdaType,
            )
        }
    }

    private fun DeclarationIrBuilder.buildRestoreLambda(
        pluginContext: IrPluginContext,
        targetClass: IrClass,
        bundleClassSymbol: IrClassSymbol,
        properties: List<IrProperty>,
    ): IrExpression {
        val irFactory = pluginContext.irFactory

        val restoreLambda = irFactory.buildFun {
            name = Name.special("<anonymous>")
            returnType = targetClass.defaultType.makeNullable()
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            parent = this@buildRestoreLambda.scope.getLocalDeclarationParent()

            val bundleParam = addValueParameter(
                "bundle",
                bundleClassSymbol.defaultType,
            )

            body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                // Get the primary constructor
                val primaryConstructor = targetClass.primaryConstructor
                    ?: error("Primary constructor not found")

                // Restore each property from Bundle
                val constructorArgs = properties.map { property ->
                    val getStringFunction = bundleClassSymbol.owner.functions
                        .firstOrNull { func ->
                            func.name.asString() == "getString" &&
                                func.valueParameters.size == 2
                        } ?: error("Bundle.getString not found")

                    irCall(getStringFunction).apply {
                        dispatchReceiver = irGet(bundleParam)
                        putValueArgument(0, irString(property.name.asString()))
                        putValueArgument(1, irString("")) // default value
                    }
                }

                // Create instance using primary constructor
                +irReturn(irCallConstructor(primaryConstructor.symbol, constructorArgs))
            }
        }

        val restoreLambdaType = pluginContext.irBuiltIns.functionN(1).typeWith(
            bundleClassSymbol.defaultType,
            targetClass.defaultType.makeNullable(),
        )

        return irBlock(resultType = restoreLambdaType) {
            +restoreLambda
            +irCallable(
                callableId = CallableId(restoreLambda.symbol.ownerSymbol.name, restoreLambda.symbol.name),
                type = restoreLambdaType,
            )
        }
    }

    private fun createCompanionObject(parentClass: IrClass): IrClass {
        val companion = pluginContext.irFactory.buildClass {
            name = Name.identifier("Companion")
            kind = ClassKind.OBJECT
            isCompanion = true
            visibility = DescriptorVisibilities.PUBLIC
            modality = Modality.FINAL
        }.apply {
            parent = parentClass
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes = listOf(pluginContext.irBuiltIns.anyType)

            // Add primary constructor (required for objects)
            addConstructor {
                isPrimary = true
                visibility = DescriptorVisibilities.PRIVATE
            }.apply {
                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +irDelegatingConstructorCall(
                        pluginContext.irBuiltIns.anyClass.owner.constructors.single()
                    )
                    +IrInstanceInitializerCallImpl(
                        startOffset, endOffset,
                        this@apply.parentAsClass.symbol,
                        pluginContext.irBuiltIns.unitType,
                    )
                }
            }
        }

        parentClass.declarations.add(companion)
        return companion
    }
}

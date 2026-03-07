package com.matuyuhi.rememberable.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class RememberableCompilerTest {

    @Test
    fun `compilation succeeds for Rememberable data class`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            package test

            import com.matuyuhi.rememberable.Rememberable

            @Rememberable
            data class FilterState(val query: String, val page: Int)
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `compilation succeeds for Rememberable non-data class`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            package test

            import com.matuyuhi.rememberable.Rememberable

            @Rememberable
            class UserState(val query: String, val page: Int) {
                companion object {
                    private val DEFAULT_PAGE = 0
                }
            }
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `class without Rememberable annotation is not modified`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            package test

            data class PlainState(val query: String)
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Verify no Saver was generated
        val clazz = result.classLoader.loadClass("test.PlainState")
        val companionClasses = clazz.declaredClasses
        val hasSaver = companionClasses.any { companion ->
            companion.declaredFields.any { field -> field.name == "Saver" }
        }
        assertFalse("PlainState should not have a Saver", hasSaver)
    }

    @Test
    fun `Saver property is generated in companion object`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            package test

            import com.matuyuhi.rememberable.Rememberable

            @Rememberable
            data class UserState(val name: String, val age: Int)
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Verify Saver was generated
        val clazz = result.classLoader.loadClass("test.UserState")
        val companionClasses = clazz.declaredClasses
        val hasSaver = companionClasses.any { companion ->
            companion.declaredFields.any { field -> field.name == "Saver" }
        }
        assertTrue("UserState should have a Saver", hasSaver)
    }

    private fun compile(vararg sourceFiles: SourceFile): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles.toList()
            compilerPluginRegistrars = listOf(RememberableCompilerRegistrar())
            commandLineProcessors = listOf(RememberableCommandLineProcessor())
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
    }
}

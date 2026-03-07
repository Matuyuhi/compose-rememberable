package io.github.matuyuhi.rememberable.compiler

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
    fun `compilation succeeds for Rememberable Parcelable data class`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            package test

            import io.github.matuyuhi.rememberable.Rememberable
            import android.os.Parcelable

            @Rememberable
            data class FilterState(val query: String, val page: Int) : Parcelable {
                override fun describeContents(): Int = 0
                override fun writeToParcel(dest: android.os.Parcel, flags: Int) {}
            }
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `compilation fails for Rememberable class without Parcelable`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            package test

            import io.github.matuyuhi.rememberable.Rememberable

            @Rememberable
            data class BadState(val query: String)
            """.trimIndent()
        )

        val result = compile(source)
        assertEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode)
        assertTrue(
            result.messages.contains("does not implement android.os.Parcelable")
        )
    }

    @Test
    fun `class without Rememberable annotation is not modified`() {
        val source = SourceFile.kotlin(
            "TestClass.kt",
            """
            package test

            import android.os.Parcelable

            data class PlainState(val query: String) : Parcelable {
                override fun describeContents(): Int = 0
                override fun writeToParcel(dest: android.os.Parcel, flags: Int) {}
            }
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

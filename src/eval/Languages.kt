package com.gitlab.ilevn.dune.eval

import java.io.File


val languages = hashMapOf(
    "python" to Python3(),
    "kotlin" to Kotlin()
)

abstract class Language(val version: String) {
    abstract val executablePath: String
    abstract val extension: String
    open val additionalArgs: List<String> = listOf()

    open val formatted: Array<String>
        get() = listOf(
            executablePath,
            *additionalArgs.toTypedArray()
        ).toTypedArray()

    abstract fun createFile(code: String): File
}

class Python3(private val imports: List<String> = listOf()) : Language("3.8") {
    override val executablePath = "/usr/bin/python3"
    override val extension = ".py"

    override fun createFile(code: String): File {
        val formattedCode = buildString {
            append(imports.joinToString("\n") { "import $it" })
            append(code)
        }
        return createTempFile(suffix = extension).also { it.writeText(formattedCode) }
    }
}

class Kotlin : Language("1.3.50") {
    override val executablePath = "/opt/openjdk-12/bin/java"
    override val additionalArgs: List<String>
        get() = "-jar /usr/lib/kotlinc/lib/kotlin-compiler.jar -script".split(" ")
    override val extension = ".kts"

    override fun createFile(code: String) = createTempFile(suffix = extension).also { it.writeText(code) }

}


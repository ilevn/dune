package com.gitlab.ilevn.dune.languages

import com.gitlab.ilevn.dune.eval.ArgumentList
import java.io.File

/**
 * Abstract base class for all languages that Dune handles.
 * @param version The current version of the language.
 */
abstract class Language(val version: String) {
    /**
     * The executable path of the language application.
     */
    abstract val executablePath: String
    /**
     * The language file extension.
     */
    abstract val extension: String
    /**
     * Any additional arguments that may be provided
     * to run a code snippet.
     */
    open val additionalArgs: List<String> = listOf()
    /**
     * The finalised argument string, which is used by
     * [ArgumentList.build].
     */
    open val formatted: List<String>
        get() = listOf(executablePath, *additionalArgs.toTypedArray())

    /**
     * Create a temporary [file][File] containing the user
     * submitted [code].
     */
    abstract fun createFile(code: String): File

    /**
     * A pre-configured list of [arguments][ArgumentList]
     * to pass to NsJail.
     */
    abstract val arguments: ArgumentList
}

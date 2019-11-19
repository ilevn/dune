package com.gitlab.ilevn.dune.languages

import com.gitlab.ilevn.dune.eval.MEM_MAX
import com.gitlab.ilevn.dune.eval.NSJAIL_PATH
import com.gitlab.ilevn.dune.eval.buildArgumentList
import java.io.File

/**
 * Python 3.8 implementation of [Language] with
 * an optional list of [imports]
 */
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

    override val arguments = buildArgumentList(NSJAIL_PATH.absolutePath) {
        misc {
            timeLimit = 5
            rLimit = 700
            environmentVars = mapOf("LANG" to "en_US.UTF-8")
        }
        cgroup {
            pidsMax = 1
            memMax = MEM_MAX
        }
    }
}

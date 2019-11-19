package com.gitlab.ilevn.dune.languages

import com.gitlab.ilevn.dune.eval.NSJAIL_PATH
import com.gitlab.ilevn.dune.eval.buildArgumentList


class Kotlin : Language("1.3.50") {
    override val executablePath = "/opt/openjdk-12/bin/java"
    override val additionalArgs: List<String>
        get() = "-jar /usr/lib/kotlinc/lib/kotlin-compiler.jar -script".split(" ")
    override val extension = ".kts"

    override fun createFile(code: String) = createTempFile(suffix = extension).also { it.writeText(code) }

    override val arguments = buildArgumentList(NSJAIL_PATH.absolutePath) {
        cgroup {
            pidsMax = 20
        }
        misc {
            rLimit = 2450
            timeLimit = 10
        }
    }
}

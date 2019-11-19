package com.gitlab.ilevn.dune.eval


import com.gitlab.ilevn.dune.languages.Kotlin
import com.gitlab.ilevn.dune.languages.Language
import com.gitlab.ilevn.dune.languages.Python3
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

val CGROUP_PIDS_PARENT = File("/sys/fs/cgroup/pids/NSJAIL")
val CGROUP_MEMORY_PARENT = File("/sys/fs/cgroup/memory/NSJAIL")
val NSJAIL_PATH = File("/usr/sbin/nsjail")
val MEM_MAX = 52428800

data class EvalResult(val value: String?, val exitCode: Int, val runtime: Long)

private inline fun <R> measureTimeMillisCaptured(block: () -> R): Pair<R, Long> {
    val start = System.currentTimeMillis()
    val result = block()
    return result to (System.currentTimeMillis() - start)
}

fun ArgumentList.runWith(with: ArgumentList.ExecBuilder.() -> Unit): Pair<String, Int>? {
    execOptions(with)
    return try {
        ProcessBuilder(build())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
            .run {
                waitFor(15, TimeUnit.SECONDS)

                val result = inputStream.bufferedReader().readText().takeUnless { it.isBlank() }
                    ?: errorStream.bufferedReader().readText()

                result to exitValue()
            }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

val languages = hashMapOf(
    "python" to Python3(),
    "kotlin" to Kotlin()
)

object NsJail {
    private val logger = KotlinLogging.logger { }

    init {
        setupParentCGroups(CGROUP_PIDS_PARENT, CGROUP_MEMORY_PARENT)
    }

    private fun setupParentCGroups(pidsPath: File, memPath: File) {
        logger.debug("Setting up memory and pids directories.")

        pidsPath.mkdirs()
        memPath.mkdirs()
        memPath.run {
            resolve("memory.limit_in_bytes").writeText(MEM_MAX.toString())
            resolve("memory.memsw.limit_in_bytes").writeText(MEM_MAX.toString())
        }
    }


    fun execute(code: String, language: Language): EvalResult? {
        logger.info("Received new request...")
        logger.debug("Running with code: `$code` and lang args `${language.formatted.joinToString()}`")

        val file = language.createFile(code).also {
            logger.debug("File path is ${it.absolutePath}")
            logger.debug("File content is ${it.readText()}")
        }

        // Random hostname, why not.
        val hstName = listOf("0x1", "memes", "nice", "easteregg", "arremsmells").random().also {
            logger.debug("Random hostname will be `$it`.")
        }

        val log = createTempFile(suffix = ".log")
        logger.debug("Log file created at ${log.absolutePath}")


        val (proc, time) = language.arguments.run {
            measureTimeMillisCaptured {
                runWith {
                    this.language = language
                    filePath = file.absolutePath
                    verbose = true
                    hostname = hstName
                    logFile = log
                }
            }
        }

        return proc?.let { EvalResult(it.first, it.second, time) }
    }

}

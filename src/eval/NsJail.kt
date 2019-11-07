package com.gitlab.ilevn.dune.eval


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

private fun List<String>.createProcess(): Pair<String, Int>? {
    return try {
        ProcessBuilder(this)
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


class NsJail(private val nsJailBinary: File = NSJAIL_PATH) {
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
        logger.debug("Running with code: `$code` and lang args `${language.formatted.joinToString()}`")

        val file = language.createFile(code).also {
            logger.debug("File path is ${it.absolutePath}")
            logger.debug("File content is ${it.readText()}")
        }

        // Random hostname, why not.
        val hostname = listOf("0x1", "memes", "nice", "easteregg", "arremsmells").random().also {
            logger.debug("Random hostname will be `$it`.")
        }

        val log = createTempFile(suffix = ".log")
        logger.debug("Log file created at ${log.absolutePath}")

        // TODO: Extract into configurable ArgumentList class.
        val arguments = listOf(
            nsJailBinary.absolutePath,
            "-Mo", "--rlimit_as", "2450",
            "--chroot", "/", "-E", "LANG=en_US.UTF-8",
            "-R/usr", "-R/lib", "-R/lib64",
            "--user", "65534",
            "--group", "65534",
            "--time_limit", "10",
            "--disable_proc",
            "--iface_no_lo",
            "--log", log.absolutePath,
            "-H", hostname,
            "--cgroup_mem_mount", CGROUP_MEMORY_PARENT.parent,
            "--cgroup_mem_parent", CGROUP_MEMORY_PARENT.name,
            "--cgroup_pids_max=20",
            "--cgroup_pids_mount", CGROUP_PIDS_PARENT.parent,
            "--cgroup_pids_parent", CGROUP_PIDS_PARENT.name,
            "-v",
            "--",
            *language.formatted, file.absolutePath

        )
        logger.debug("Calling NsJail with ${arguments.joinToString(" ")}")
        logger.info("Received new request...")

        val (proc, time) = measureTimeMillisCaptured {
            arguments.createProcess()
        }

        return proc?.let { EvalResult(it.first, it.second, time) }
    }

}

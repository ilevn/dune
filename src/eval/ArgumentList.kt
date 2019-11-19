package com.gitlab.ilevn.dune.eval

import com.gitlab.ilevn.dune.languages.Language
import java.io.File

private interface Builder {
    fun build(): List<String>
}

@DslMarker
annotation class ArgumentListBuilder

/**
 * Argument builder for the [Nsjail](https://google.github.io/nsjail/) runtime.
 * This class provides a number of builder sub-methods such as [misc], [cgroup] and [execOptions].
 *
 * Call [build] to finalise the configuration.
 */
@ArgumentListBuilder
class ArgumentList(private val nsjailPath: String) {
    private var misc: MiscBuilder = MiscBuilder()
        set(builder) {
            require(builder.timeLimit in 1..15) { "time_limit needs to be within 1 and 15 seconds." }
            require(builder.rLimit in 200..3000) { "rlimit needs to be within 200 and 3000 mb." }
            field = builder
        }

    /**
     * Configure the [rlimit][MiscBuilder.rLimit], [time_limit][MiscBuilder.timeLimit]
     * and [environment variables][MiscBuilder.environmentVars].
     */
    fun misc(builder: MiscBuilder.() -> Unit) {
        misc = MiscBuilder().apply(builder)
    }

    private var cgroup: CGroupBuilder = CGroupBuilder(1)
        set(value) {
            require(value.pidsMax in 0..20) { "Pids max cannot exceed 20" }
            require(value.memMax in 0..MEM_MAX) { "Memory max cannot exceed $MEM_MAX" }
            field = value
        }

    /**
     * Configure the [memory mounting point][CGroupBuilder.memPath], the [pids mounting point][CGroupBuilder.pidsPath],
     * the [maximum amount of bytes of memory][CGroupBuilder.memMax]
     * and the [maximum amount of pids][CGroupBuilder.pidsMax].
     *
     * This defaults to 10 and is capped at 20.
     */
    fun cgroup(builder: CGroupBuilder.() -> Unit) {
        cgroup = CGroupBuilder().apply(builder)
    }

    private var execOptions: ExecBuilder = ExecBuilder()

    /**
     * Configure the [language][Language] as well as the [file path][ExecBuilder.filePath] of the file containing code.
     * Verbose output can be turned on with [ExecBuilder.verbose] set to true.
     */
    fun execOptions(builder: ExecBuilder.() -> Unit) {
        execOptions = ExecBuilder().apply(builder)
    }

    /**
     * Build the ArgumentList
     * @return The newly formatted [List] with defaults and configured options.
     */
    fun build(): List<String> {
        return ArrayList<String>().apply {
            add(nsjailPath)
            listOf(Defaults, misc, cgroup, execOptions).forEach { addAll(it.build()) }
        }.joinToString(" ").split(" ") // Not exactly clean, but whatever.
    }

    private object Defaults : Builder {
        override fun build(): List<String> {
            return "-Mo --chroot / -R/usr -R/lib -R/lib64 --user 65534 --group 65534 --iface_no_lo --disable_proc"
                .split(" ")
        }
    }

    private class ArgumentBuilder(private val name: String, val value: Any, private val isOptional: Boolean = true) {
        internal fun build() = buildString {
            append(if (isOptional) "--" else "-")
            append("$name $value")
        }
    }

    /**
     * @param timeLimit Maximum time that a process can run, in seconds.
     * @param rLimit RLIMIT_AS in MB.
     * @param environmentVars Additional environment variables passed to the jail.
     */
    class MiscBuilder(
        var timeLimit: Int = 10,
        var rLimit: Int = 2450,
        var environmentVars: Map<String, String> = mapOf()
    ) : Builder {
        override fun build(): List<String> {
            return mutableListOf("--time_limit $timeLimit", "--rlimit_as $rLimit").apply {
                addAll(environmentVars.map {
                    ArgumentBuilder(
                        "E",
                        "${it.key.toUpperCase()}=${it.value}",
                        isOptional = false
                    ).build()
                })
            }.toList()
        }
    }

    /**
     * @param pidsMax Maximum number of pids in a cgroup, 10 by default.
     * @param memMax The maximum number of bytes to use in a cgroup, uncapped by default.
     * @param memPath Location of mem cgroup FS.
     * @param pidsPath Location of pids cgroup FS.
     */
    class CGroupBuilder(
        var pidsMax: Int = 10,
        var memMax: Int = 0,
        var memPath: File = CGROUP_MEMORY_PARENT,
        var pidsPath: File = CGROUP_PIDS_PARENT
    ) : Builder {

        override fun build(): List<String> = listOf(
            "--cgroup_pids_max=$pidsMax",
            "--cgroup_mem_max=$memMax",
            "--cgroup_mem_mount ${memPath.parent} --cgroup_mem_parent ${memPath.name}",
            "--cgroup_pids_mount ${pidsPath.parent} --cgroup_pids_parent ${pidsPath.name}"
        )
    }

    /**
     * @param language The [Language] to use.
     * @param filePath The file path containing the code snippet to run.
     * @param verbose Whether to log output verbosely or not.
     * @param hostname The hostname to use.
     * @param logFile The log file to use.
     */
    class ExecBuilder(
        var language: Language? = null,
        var filePath: String? = null,
        var verbose: Boolean = false,
        var hostname: String? = null,
        var logFile: File? = null
    ) : Builder {

        override fun build() = mutableListOf<String>().apply {
            if (verbose) {
                add("-v")
            }
            hostname?.let {
                add("-H $it")
            }

            logFile?.let {
                add("--log ${it.absolutePath}")
            }

            language?.let {
                add("-- ${it.formatted.joinToString(" ")} $filePath")
            }
        }.toList()
    }
}

/**
 * Create NSJAIL arguments using the [ArgumentList] builder block.
 * @param nsjailPath The path pointing to an NSJAIL executable.
 *
 * @sample com.gitlab.ilevn.dune.languages.Kotlin.arguments
 */
@ArgumentListBuilder
inline fun buildArgumentList(nsjailPath: String, builder: ArgumentList.() -> Unit) =
    ArgumentList(nsjailPath).apply(builder)




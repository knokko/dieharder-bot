package random.testing

import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.filechooser.FileSystemView
import kotlin.math.abs
import kotlin.random.Random

class TestRunner(
        private val generator: RandomFileGenerator,
        private val tester: TestingSoftware,
        private val size: Long,
) {

    private fun directory() = File("${generator}_${tester}_$size")

    fun saveResults(results: List<TrialResult>) {
        val directory = directory()
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Failed to create directory $directory")

        val rng = Random.Default
        var id = rng.nextInt()
        while (File("$directory/$id.trials").exists()) id = rng.nextInt()

        TrialResult.dump(File("$directory/$id.trials"), results)
    }

    fun loadAllResults(): List<TrialResult> {
        val directory = directory()
        val files = directory.listFiles()

        val results = mutableListOf<TrialResult>()
        if (files != null) {
            for (file in files.filter { it.extension == "trials" }) {
                results.addAll(TrialResult.parse(file))
            }
        }

        return results
    }

    fun runTrial(): Collection<TrialResult> {
        val seed = abs((System.nanoTime() + (1234567890 * Math.random()).toLong()).toInt())
        val file = File(FileSystemView.getFileSystemView().homeDirectory.absolutePath + "/rand${UUID.randomUUID()}_$seed.bin")
        generator.generate(seed, size, file)
        val result = tester.performTest(file, seed.toLong())
        if (!file.delete()) System.err.println("Failed to delete $file")
        return result
    }

    fun runTrials(numTrials: Int): List<TrialResult> {
        val results = Collections.synchronizedList(mutableListOf<TrialResult>())
        val threads = mutableListOf<Thread>()

        val resultCounter = AtomicInteger()

        val numThreads = generator.recommendedNumberOfThreads()
        for (threadIndex in 0 until numThreads) {
            val thread = Thread {
                for (counter in 0 until numTrials / numThreads) {
                    results.addAll(runTrial())
                    resultCounter.incrementAndGet()
                }
            }
            threads.add(thread)
            thread.start()
        }

        for (thread in threads) thread.join()

        threads.clear()
        for (counter in resultCounter.get() until numTrials) {
            val thread = Thread { results.addAll(runTrial()) }
            threads.add(thread)
            thread.start()
        }
        for (thread in threads) thread.join()

        println("size is ${results.size}")
        return results
    }

    fun printPValues(name: String, results: List<TrialResult>) {
        val numbersPerLine = 10
        val format = "    " + Array(numbersPerLine) { "%.4f" }.joinToString(" ") + " ..."
        println(format)
        println("$name = [")
        for (rawIndex in 0 until results.size / numbersPerLine) {
            val currentResults = results.subList(numbersPerLine * rawIndex, numbersPerLine * (rawIndex + 1)).map { it.pValue }
            println(String.format(format, *currentResults.toTypedArray()))
        }
        println("]")
    }
}

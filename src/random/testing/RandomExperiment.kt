package random.testing

import java.io.File
import java.io.IOException
import java.util.*
import kotlin.random.Random

enum class ExperimentType {
    MATLAB,
    SEEDED,
    UNSEEDED,
    JAVA,
    CRYPTO;
}

class RandomExperiment(
        val type: ExperimentType,
        val numTrials: Int,
        val tupleSize: Int,
        val samplesPerTest: Int
) {

    private fun performTrial() = when(type) {
        ExperimentType.MATLAB -> performMatlabTrial(tupleSize, samplesPerTest)
        ExperimentType.SEEDED -> performBuiltinTrial(tupleSize, samplesPerTest, true)
        ExperimentType.UNSEEDED -> performBuiltinTrial(tupleSize, samplesPerTest, false)
        ExperimentType.JAVA -> performJavaTrial(tupleSize, samplesPerTest, false)
        ExperimentType.CRYPTO -> performJavaTrial(tupleSize, samplesPerTest, true)
    }

    fun conduct(): List<TrialResult> {
        val results = Collections.synchronizedList(mutableListOf<TrialResult>())
        val threads = mutableListOf<Thread>()

        val numThreads = if (type == ExperimentType.MATLAB) 4 else 10
        for (threadIndex in 0 until numThreads) {
            val thread = Thread {
                for (counter in 0 until numTrials / numThreads) results.add(performTrial())
            }
            threads.add(thread)
            thread.start()
        }

        for (thread in threads) thread.join()

        threads.clear()
        for (counter in results.size until numTrials) {
            val thread = Thread { results.add(performTrial()) }
            threads.add(thread)
            thread.start()
        }
        for (thread in threads) thread.join()

        println("size is ${results.size}")
        return results
    }

    fun directory() = File("${type.name.lowercase(Locale.ROOT)}${samplesPerTest}k${tupleSize}n")

    fun save(results: List<TrialResult>) {
        val directory = directory()
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Failed to create directory $directory")

        val rng = Random.Default
        var id = rng.nextInt()
        while (File("$directory/$id.trials").exists()) id = rng.nextInt()

        TrialResult.dump(File("$directory/$id.trials"), results)
    }

    fun loadAll(): List<TrialResult> {
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
}

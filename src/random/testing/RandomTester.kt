package random.testing

import java.io.File
import java.lang.Double.parseDouble
import java.lang.Long.parseLong
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.SecureRandom
import java.util.*
import javax.swing.filechooser.FileSystemView
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

fun main() {
    val experiment = RandomExperiment(ExperimentType.CRYPTO, 100, 1, 0)
    for (counter in 0 until 15) {
        experiment.save(experiment.conduct())
    }
}

private fun getResults(command: Array<String>): List<String> {
    val process = Runtime.getRuntime().exec(command)
    if (process.waitFor() != 0) throw RuntimeException()

    val errors = Scanner(process.errorStream)
    while (errors.hasNextLine()) {
        System.err.println(errors.nextLine())
    }
    errors.close()

    val scanner = Scanner(process.inputStream)
    while (!scanner.nextLine().contains("p-value")) {
        // do nothing
    }
    scanner.nextLine()

    val results = scanner.nextLine().split("|")
    scanner.close()
    return results
}

fun performMatlabTrial(tupleSize: Int, samplesPerTest: Int): TrialResult {
    val seed = Random.Default.nextInt(Int.MAX_VALUE).toLong()
    val numTrials = 2.0.pow(tupleSize).roundToLong() * 500 * samplesPerTest
    val matlabCommand = "fileID = fopen('~/rand$seed.bin','w'); rng($seed, 'twister'); " +
            "for i=1:1000; fwrite(fileID, randi([0 255], ${numTrials / 1000}, 1)); end; " +
            "[status, stdout] = system('dieharder -g 201 -d 200 -n $tupleSize -f ~/rand$seed.bin -t $samplesPerTest'); stdout"

    val results = getResults(arrayOf("/usr/local/MATLAB/R2023b/bin/matlab", "-batch", matlabCommand))
    val pValue = parseDouble(results[4])
    val assessment = Assessment.valueOf(results[5].trim())

    val dumpFile = File("${FileSystemView.getFileSystemView().homeDirectory}/rand$seed.bin")

    dumpFile.delete()

    return TrialResult(assessment, pValue, seed)
}

private fun byteSeed(longSeed: Long): ByteArray {
    val buffer = ByteBuffer.allocate(8)
    buffer.putLong(longSeed)
    buffer.flip()

    return buffer.array()
}

fun performJavaTrial(tupleSize: Int, samplesPerTest: Int, secure: Boolean): TrialResult {
    val seed = Random.Default.nextInt(Int.MAX_VALUE).toLong()

    val dumpFile = File("${FileSystemView.getFileSystemView().homeDirectory}/rand$seed.bin")
    val rng = if (secure) SecureRandom(byteSeed(seed)) else java.util.Random(seed)
    val numTrials = 2.0.pow(tupleSize).roundToLong() * 500 * if (samplesPerTest != 0) samplesPerTest else 200_000

    val dumpOutput = Files.newOutputStream(dumpFile.toPath())
    val bytes = ByteArray(1000)
    for (counter in 0 until numTrials / bytes.size) {
        rng.nextBytes(bytes)
        dumpOutput.write(bytes)
    }
    dumpOutput.flush()
    dumpOutput.close()

    val results = getResults(arrayOf("dieharder", "-g", "201", "-d", "200", "-f", dumpFile.absolutePath, "-t", samplesPerTest.toString(), "-n", tupleSize.toString()))
    val pValue = parseDouble(results[4])
    val assessment = Assessment.valueOf(results[5].trim())

    dumpFile.delete()

    return TrialResult(assessment, pValue, seed)
}

fun performBuiltinTrial(tupleSize: Int, samplesPerTest: Int, seeded: Boolean): TrialResult {
    val seed = if (seeded) Random.Default.nextInt(Int.MAX_VALUE).toLong() else 0
    val seedCommand = if (seeded) arrayOf("-s", "1", "-S", seed.toString()) else emptyArray()
    val results = getResults(arrayOf("dieharder", "-g", "403", "-d", "200", "-t", samplesPerTest.toString(), "-n", tupleSize.toString()) + seedCommand)

    val pValue = parseDouble(results[4])
    val assessment = Assessment.valueOf(results[5].trim())
    if (seeded) {
        val actualSeed = parseLong(results[6].trim())
        if (seed != actualSeed) throw RuntimeException()
    }

    return TrialResult(assessment, pValue, seed)
}

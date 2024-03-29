package random.testing

import org.apache.commons.statistics.distribution.ChiSquaredDistribution
import java.io.File
import java.lang.Double.parseDouble
import java.nio.file.Files
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively
import kotlin.math.pow
import kotlin.math.roundToLong

interface TestingSoftware {

    fun performTest(file: File): Pair<Assessment, Double>
}

class DieharderTester(private val tupleSize: Int) : TestingSoftware {

    override fun toString() = "dieharder$tupleSize"

    override fun performTest(file: File): Pair<Assessment, Double> {
        val length = file.length()
        if (length <= 0) throw IllegalArgumentException("Illegal length $length for file $file")

        val samplesPerTest = length / (2.0.pow(tupleSize).roundToLong() * 500)
        val command = arrayOf(
                "/home/knokko/programming/dieharder/dieharder/dieharder",
                "-g", "201",
                "-d", "200",
                "-f", file.absolutePath,
                "-t", samplesPerTest.toString(),
                "-n", tupleSize.toString()
        )

        val process = Runtime.getRuntime().exec(command)
        failOnError(process)

        val scanner = Scanner(process.inputStream)
        while (!scanner.nextLine().contains("p-value")) {
            // do nothing
        }
        scanner.nextLine()

        val pValue = parseDouble(scanner.nextLine().split("|")[4])
        scanner.close()

        val assessment = if (pValue < 0.0001 || pValue > 0.9999) Assessment.FAILED else if (pValue < 0.05) Assessment.WEAK else Assessment.PASSED

        return Pair(assessment, pValue)
    }
}

class GjRandTester(private val targetTest: String?) : TestingSoftware {

    override fun toString() = "gjrand_$targetTest"

    @OptIn(ExperimentalPathApi::class)
    override fun performTest(file: File): Pair<Assessment, Double> {
        val size = file.length()
        val mb = 1024L * 1024L

        val gjSize: String = if (size < 10 * mb) throw IllegalArgumentException("File size ($size) must be at least 10MB")
        else if (size < 100 * mb) "tiny"
        else if (size < 1024 * mb) "small"
        else if (size < 10 * 1024 * mb) "standard"
        else "big"

        val reportDirectory = Files.createTempDirectory(null)

        val processBuilder = ProcessBuilder(listOf("./mcp", "--$gjSize", "-d", reportDirectory.absolutePathString()))
        processBuilder.directory(File("/home/knokko/programming/gjrand.4.3.0.0/testunif/"))
        processBuilder.redirectInput(file)

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        reportDirectory.deleteRecursively()

        if (exitCode == 0) {
            var currentTestName = ""
            var finalPValue = -1.0
            val results = mutableMapOf<String, Double>()

            val inputScanner = Scanner(process.inputStream)
            while (inputScanner.hasNextLine()) {
                val line = inputScanner.nextLine()
                if (line == "============") {
                    val testNameLine = inputScanner.nextLine()
                    currentTestName = testNameLine.substring(0, testNameLine.indexOf(" "))
                    if (inputScanner.nextLine() != "=======") throw RuntimeException("Unexpected next line")
                }

                if (line.startsWith("P = ")) {
                    val indexSpace = line.indexOf(" ", 4)
                    if (indexSpace == -1) {
                        if (currentTestName.isEmpty()) throw IllegalStateException()
                        val pValue = parseDouble(line.substring("P = ".length))
                        results[currentTestName] = pValue
                        currentTestName = ""
                    } else {
                        finalPValue = parseDouble(line.substring("P = ".length, indexSpace))
                    }
                }
            }
            inputScanner.close()

//            for ((test, pValue) in results) {
//                println("pValue of $test is $pValue")
//            }
            if (targetTest != null) finalPValue = results[targetTest]!!

            val assessment = if (finalPValue < 0.0001) Assessment.FAILED else if (finalPValue < 0.05) Assessment.WEAK else Assessment.PASSED

            return Pair(assessment, finalPValue)
        } else {
            val errorScanner = Scanner(process.errorStream)
            while (errorScanner.hasNextLine()) {
                System.err.println(errorScanner.nextLine())
            }
            errorScanner.close()
            throw RuntimeException("Failure")
        }
    }
}

class BitDistributionTester(private val tupleLength: Int) : TestingSoftware {

    init {
        if (tupleLength <= 0) throw IllegalArgumentException("Invalid tupleLength $tupleLength")
    }

    override fun toString() = "bitdist$tupleLength"

    override fun performTest(file: File): Pair<Assessment, Double> {
        var numPossibilities = 1
        for (bit in 0 until tupleLength) numPossibilities *= 2

        val occurrenceCounter = LongArray(numPossibilities)

        val currentTuple = mutableListOf<Boolean>()

        val input = Files.newInputStream(file.toPath())
        var nextByte = input.read()
        while (nextByte != -1) {
            for (bit in 0 until 8) {
                val nextBoolean = (nextByte and (1 shl bit)) != 0
                currentTuple.add(nextBoolean)
                if (currentTuple.size == tupleLength) {
                    var tupleValue = 0
                    var bitValue = 1
                    for (tupleBit in 0 until tupleLength) {
                        if (currentTuple[tupleBit]) tupleValue += bitValue
                        bitValue *= 2
                    }
                    occurrenceCounter[tupleValue] += 1L
                    currentTuple.clear()
                }
            }
            nextByte = input.read()
        }
        input.close()

        val numTuples = occurrenceCounter.sum()
        val expectedCount = numTuples / numPossibilities
        if (expectedCount < 100) throw IllegalArgumentException("File is too small")

        var chiSquaredTestStatistic = 0.0
        for (tupleIndex in 0 until numPossibilities) {
            val absoluteDeviation = occurrenceCounter[tupleIndex] - expectedCount
            chiSquaredTestStatistic += (absoluteDeviation * absoluteDeviation) / expectedCount.toDouble()
        }

        val distribution = ChiSquaredDistribution.of(numPossibilities - 1.0)

        val pValue = distribution.cumulativeProbability(chiSquaredTestStatistic)
        val assessment = if (pValue < 0.0001 || pValue > 0.9999) Assessment.FAILED
        else if (pValue < 0.01 || pValue > 0.99) Assessment.WEAK else Assessment.PASSED

        return Pair(assessment, pValue)
    }
}

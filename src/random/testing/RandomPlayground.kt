package random.testing

import org.apache.commons.statistics.distribution.ChiSquaredDistribution
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.Files
import java.security.SecureRandom
import java.util.Random

fun main() {
//    val experiment = RandomExperiment(ExperimentType.JAVA, 100, 1, 10000)
//    val results = experiment.loadAll()
//    experiment.printPValues(results)
    //generateFile()
    //printFileStats()
    val testRunner = TestRunner(JavaSecureGenerator(), DieharderTester(1), 10_000_000L)
    val results = testRunner.loadAllResults()
//    testRunner.printPValues("java_bitdist11", results)
    println("${results.count { it.pValue > 0.9 }} / ${results.size}")

//    val testRunner = TestRunner(JavaStandardGenerator(), DieharderTester(1), 10_000_000L)
//    println(testRunner.runTrial())
}

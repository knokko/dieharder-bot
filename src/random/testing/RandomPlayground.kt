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
    val testRunner = TestRunner(MatlabRandomGenerator(), GjRandTester(), 10L * 1024L * 1024L * 1024L)
    val results = testRunner.loadAllResults()
    //testRunner.printPValues("matlab_bitdist1", results)
    for (subTest in results.map { it.subTest }.toSet()) {
        val results = results.filter { it.subTest == subTest }
        println("$subTest: ${results.count { it.pValue < 0.1 }} / ${results.size}")
    }

//    val testRunner = TestRunner(JavaStandardGenerator(), DieharderTester(1), 10_000_000L)
//    println(testRunner.runTrial())
}

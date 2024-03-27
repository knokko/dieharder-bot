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
    val testRunner = TestRunner(JavaStandardGenerator(), BitDistributionTester(11), 8 * 1024 * 1024)
    val results = testRunner.loadAllResults()
//    println("${results.count { it.pValue < 0.1 }} / ${results.size}")

//    val testRunner = TestRunner(JavaSecureGenerator(), BitDistributionTester(3), 1024L * 1024)
//    println(testRunner.runTrial())
}

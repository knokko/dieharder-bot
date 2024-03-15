package random.testing

fun main() {
    val experiment = RandomExperiment(ExperimentType.CRYPTO, 100, 1, 0)
    val results = experiment.loadAll()
    println("${results.count { it.assessment != Assessment.PASSED }} / ${results.size}")
}

package random.testing

fun main() {
    val experiment = RandomExperiment(ExperimentType.MATLAB, 100, 1, 10000)
    val results = experiment.loadAll()
    println("${results.count { it.assessment != Assessment.PASSED }} / ${results.size}")
    experiment.printPValues(results)
}

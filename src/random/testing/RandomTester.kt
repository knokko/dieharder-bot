package random.testing

fun main() {
    val testRunner = TestRunner(MatlabRandomGenerator(), GjRandTester(), 10L * 1024L * 1024L * 1024L)
    for (counter in 0 until 100) {
        val results = testRunner.runTrials(10)
        testRunner.saveResults(results)
        println("finished iteration $counter")
    }
}

package random.testing

fun main() {
    val testRunner = TestRunner(JavaSecureGenerator(), GjRandTester(), 10L * 1024L * 1024L * 1024L)
    for (counter in 0 until 13) {
        val results = testRunner.runTrials(6)
        testRunner.saveResults(results)
        println("finished iteration $counter")
    }
}

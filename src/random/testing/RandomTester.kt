package random.testing

fun main() {
    val testRunner = TestRunner(JavaSecureGenerator(), GjRandTester(), 1024L * 1024L * 1024L)
    for (counter in 0 until 10) {
        val results = testRunner.runTrials(10)
        testRunner.saveResults(results)
        println("finished iteration $counter")
    }
}

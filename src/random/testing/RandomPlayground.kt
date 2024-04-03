package random.testing

fun main() {
    val secureRunner = TestRunner(JavaSecureGenerator(), GjRandTester(), 10L * 1024L * 1024L * 1024L)
    val secureResults = secureRunner.loadAllResults()

    val matlabRunner = TestRunner(MatlabRandomGenerator(), GjRandTester(), 10L * 1024L * 1024L * 1024L)
    val matlabResults = matlabRunner.loadAllResults().subList(0, 48 * 13)

//    val javaRunner = TestRunner(JavaStandardGenerator(), GjRandTester(), 10L * 1024L * 1024L)
//    val javaResults = javaRunner.loadAllResults().subList(0, 13000)

    for (subTest in secureResults.map { it.subTest }.toSet()) {
        println("$subTest: ${secureResults.count { it.subTest == subTest && it.pValue < 0.1 }} / ${secureResults.count { it.subTest == subTest }}")
        println("$subTest: ${matlabResults.count { it.subTest == subTest && it.pValue < 0.1 }} / ${matlabResults.count { it.subTest == subTest }}")
//        println("$subTest: ${javaResults.count { it.subTest == subTest && it.pValue < 0.1 }} / ${javaResults.count { it.subTest == subTest }}")
    }

//    matlabRunner.printPValues("matlab_gjrand10M", matlabResults.filter { it.subTest == null })
//    secureRunner.printPValues("crypto_gjrand10M", secureResults.filter { it.subTest == null })
//    javaRunner.printPValues("java_gjrand10M", javaResults.filter { it.subTest == null })
}

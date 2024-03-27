package random.testing

import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

// Copied from this SO answer: https://stackoverflow.com/a/54797901
fun normCDF(z: Double): Double {
    val LeftEndpoint = -100.0
    val nRectangles = 100000
    var runningSum = 0.0
    var x: Double
    for (n in 0 until nRectangles) {
        x = LeftEndpoint + n * (z - LeftEndpoint) / nRectangles
        runningSum += sqrt(2 * Math.PI).pow(-1.0) * exp(-x.pow(2.0) / 2) * (z - LeftEndpoint) / nRectangles
    }
    println(runningSum)
    return runningSum
}

fun normalCDF(x: Double): Double {
    return 0.5 * (1 + erf(x / sqrt(2.0)))
}

fun erf(z: Double): Double {
    val nTerms = 315
    var runningSum = 0.0
    for (n in 0 until nTerms) {
        runningSum += (-1.0).pow(n.toDouble()) * z.pow((2 * n + 1).toDouble()) / (factorial(n) * (2 * n + 1))
        println("runningSum is $runningSum first is ${(-1.0).pow(n.toDouble())} second is ${z.pow((2 * n + 1).toDouble())} third is ${factorial(n) * (2 * n + 1)}")
    }
    return (2 / sqrt(Math.PI)) * runningSum
}

fun factorial(n: Int): Double {
    if (n == 0) return 1.0
    if (n == 1) return 1.0
    return n * factorial(n - 1)
}

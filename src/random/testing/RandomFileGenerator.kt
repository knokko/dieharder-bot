package random.testing

import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import java.security.SecureRandom
import java.util.*
import kotlin.math.roundToLong
import kotlin.math.sqrt

interface RandomFileGenerator {

    fun generate(seed: Int, size: Long, file: File)

    fun recommendedNumberOfThreads() = 10
}

fun failOnError(process: Process) {
    val exitCode = process.waitFor()

    if (exitCode != 0) {
        System.err.println("Command failed:")
        val errors = Scanner(process.errorStream)
        while (errors.hasNextLine()) {
            System.err.println(errors.nextLine())
        }
        throw RuntimeException("Exit code is $exitCode")
    }
}

class MatlabRandomGenerator(private val sequentialSeeds: Boolean) : RandomFileGenerator {

    override fun toString() = "matlab${if (sequentialSeeds) "_seq" else ""}"

    override fun generate(seed: Int, size: Long, file: File) {
        val matlabCommand = if (sequentialSeeds) {
            val matrixSize = 1024
            val numIterations = size / (matrixSize * matrixSize)
            if (matrixSize * matrixSize * numIterations != size) {
                throw IllegalArgumentException("Size ($size) must be a multiple of 1024*1024")
            }

            "fileID = fopen('${file.absolutePath}', 'w'); seed = $seed; byte_matrix = zeros($matrixSize, $matrixSize); " +
                    "for i=1:$numIterations; for x=1:$matrixSize; rng(seed + mtimes(i, $matrixSize) + x, 'twister'); " +
                    "byte_matrix(x, :) = randi([0 255], $matrixSize, 1); end; for y=1:$matrixSize; fwrite(fileID, byte_matrix(:, y)); end; end;"
        } else {
            if (size % 1024L != 0L) throw IllegalArgumentException("Size ($size) must be a multiple of 1024")
            "fileID = fopen('${file.absolutePath}','w'); rng($seed, 'twister'); " +
                    "for i=1:1024; fwrite(fileID, randi([0 255], ${size / 1024}, 1)); end; "
        }
        println("matlabCommand is $matlabCommand")
        val command = arrayOf(MATLAB_PATH, "-batch", matlabCommand, "-nojvm")

        failOnError(Runtime.getRuntime().exec(command))
    }

    override fun recommendedNumberOfThreads() = 4
}

abstract class JavaRandomGenerator : RandomFileGenerator {

    override fun generate(seed: Int, size: Long, file: File) {
        val buffer = if (size % 1000L == 0L) ByteArray(1000) else ByteArray(1024)
        if (size % buffer.size != 0L) throw IllegalArgumentException("Size ($size) must be a multiple of ${buffer.size}")

        val rng = createGenerator(seed)
        val output = Files.newOutputStream(file.toPath())

        for (counter in 0 until size / buffer.size) {
            rng.nextBytes(buffer)
            output.write(buffer)
        }

        output.flush()
        output.close()
    }

    protected abstract fun createGenerator(seed: Int): Random
}

class JavaStandardGenerator : JavaRandomGenerator() {

    override fun toString() = "java_weak"

    override fun createGenerator(seed: Int) = Random(seed.toLong())
}

class JavaSecureGenerator : JavaRandomGenerator() {

    override fun toString() = "java_secure"

    private fun byteSeed(longSeed: Int): ByteArray {
        val buffer = ByteBuffer.allocate(8)
        buffer.putLong(longSeed.toLong())
        buffer.flip()

        return buffer.array()
    }

    override fun createGenerator(seed: Int) = SecureRandom(byteSeed(seed))
}

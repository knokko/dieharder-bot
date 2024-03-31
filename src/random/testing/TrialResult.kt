package random.testing

import java.io.File
import java.io.PrintWriter
import java.lang.Double.parseDouble
import java.lang.Long.parseLong
import java.util.*

enum class Assessment {
    PASSED,
    WEAK,
    FAILED;
}

class TrialResult(
        val assessment: Assessment,
        val pValue: Double,
        val seed: Long,
        val subTest: String?
) {
    override fun toString() = "TrialResult($assessment, p=$pValue, seed=$seed${if (subTest != null) ", sub=$subTest" else ""})"

    companion object {
        fun dump(file: File, results: List<TrialResult>) {
            val writer = PrintWriter(file)
            for (result in results) writer.println(result)
            writer.flush()
            writer.close()
        }

        fun parse(file: File): List<TrialResult> {
            val results = mutableListOf<TrialResult>()
            val scanner = Scanner(file)
            while (scanner.hasNextLine()) {
                results.add(parse(scanner.nextLine()))
            }
            scanner.close()
            return results
        }

        private fun parse(line: String): TrialResult {
            val parts = line.split(", ", "(", ")", "=")
            return TrialResult(
                    assessment = Assessment.valueOf(parts[1]),
                    pValue = parseDouble(parts[3]),
                    seed = parseLong(parts[5]),
                    subTest = if (parts.size > 7) parts[7] else null
            )
        }
    }
}

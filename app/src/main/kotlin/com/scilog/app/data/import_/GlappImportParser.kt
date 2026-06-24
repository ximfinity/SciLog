package com.scilog.app.data.import_

import com.scilog.app.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses Glapp.io export data (CSV or JSON) and returns domain model lists
 * ready to be persisted via repositories.
 */
object GlappImportParser {

    data class ImportResult(
        val shots: List<Shot>,
        val weights: List<Weight>,
        val errors: List<String>
    )

    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("MM/dd/yyyy", Locale.US)
    )

    fun parseJson(jsonString: String): ImportResult {
        val shots = mutableListOf<Shot>()
        val weights = mutableListOf<Weight>()
        val errors = mutableListOf<String>()

        runCatching {
            val root = JSONObject(jsonString)

            // Parse shots array
            root.optJSONArray("shots")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching {
                        val obj = arr.getJSONObject(i)
                        shots += parseGlappShot(obj)
                    }.onFailure { errors += "Shot[$i]: ${it.message}" }
                }
            }

            // Parse weights array
            root.optJSONArray("weights")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching {
                        val obj = arr.getJSONObject(i)
                        weights += parseGlappWeight(obj)
                    }.onFailure { errors += "Weight[$i]: ${it.message}" }
                }
            }
        }.onFailure { errors += "JSON parse error: ${it.message}" }

        return ImportResult(shots, weights, errors)
    }

    fun parseCsv(csv: String): ImportResult {
        val shots = mutableListOf<Shot>()
        val weights = mutableListOf<Weight>()
        val errors = mutableListOf<String>()

        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportResult(emptyList(), emptyList(), listOf("Empty CSV"))

        val header = lines.first().split(",").map { it.trim().lowercase() }

        lines.drop(1).forEachIndexed { idx, line ->
            val cols = line.split(",").map { it.trim() }
            val row = header.zip(cols).toMap()
            runCatching {
                when {
                    row.containsKey("dose") || row.containsKey("dose_mg") -> {
                        shots += parseCsvShot(row)
                    }
                    row.containsKey("weight") || row.containsKey("weight_lbs") -> {
                        weights += parseCsvWeight(row)
                    }
                }
            }.onFailure { errors += "Row ${idx + 2}: ${it.message}" }
        }

        return ImportResult(shots, weights, errors)
    }

    private fun parseGlappShot(obj: JSONObject): Shot {
        val dateStr = obj.optString("date") ?: obj.optString("timestamp") ?: error("No date field")
        val ms = parseDate(dateStr)
        val dose = obj.optDouble("dose", -1.0).takeIf { it > 0 }
            ?: obj.optDouble("dose_mg", -1.0).takeIf { it > 0 }
            ?: error("No dose field")
        val medStr = obj.optString("medication", "").ifBlank { "SEMAGLUTIDE" }.uppercase()
        val med = MedicationType.entries.firstOrNull { it.name == medStr } ?: MedicationType.SEMAGLUTIDE

        return Shot(
            timestampMs = ms,
            doseMg = dose,
            medicationType = med,
            notes = obj.optString("notes", "")
        )
    }

    private fun parseGlappWeight(obj: JSONObject): Weight {
        val dateStr = obj.optString("date") ?: obj.optString("timestamp") ?: error("No date field")
        val ms = parseDate(dateStr)
        val lbs = obj.optDouble("weight_lbs", -1.0).takeIf { it > 0 }
            ?: obj.optDouble("weight", -1.0).takeIf { it > 0 }
            ?: error("No weight field")
        return Weight(timestampMs = ms, weightLbs = lbs, notes = obj.optString("notes", ""))
    }

    private fun parseCsvShot(row: Map<String, String>): Shot {
        val dateStr = row["date"] ?: row["timestamp"] ?: error("No date column")
        val ms = parseDate(dateStr)
        val dose = (row["dose"] ?: row["dose_mg"])?.toDoubleOrNull() ?: error("No dose column")
        val medStr = (row["medication"] ?: "").uppercase().ifBlank { "SEMAGLUTIDE" }
        val med = MedicationType.entries.firstOrNull { it.name == medStr } ?: MedicationType.SEMAGLUTIDE
        return Shot(timestampMs = ms, doseMg = dose, medicationType = med)
    }

    private fun parseCsvWeight(row: Map<String, String>): Weight {
        val dateStr = row["date"] ?: row["timestamp"] ?: error("No date column")
        val ms = parseDate(dateStr)
        val lbs = (row["weight"] ?: row["weight_lbs"])?.toDoubleOrNull() ?: error("No weight column")
        return Weight(timestampMs = ms, weightLbs = lbs)
    }

    private fun parseDate(str: String): Long {
        for (fmt in dateFormats) {
            runCatching { return fmt.parse(str)!!.time }.onSuccess { return it }
        }
        // Try epoch ms
        str.toLongOrNull()?.let { return it }
        error("Cannot parse date: $str")
    }
}

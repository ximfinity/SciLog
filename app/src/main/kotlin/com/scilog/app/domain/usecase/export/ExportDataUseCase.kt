package com.scilog.app.domain.usecase.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.scilog.app.domain.repository.ShotRepository
import com.scilog.app.domain.repository.WeightRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val shotRepository: ShotRepository,
    private val weightRepository: WeightRepository,
    @ApplicationContext private val context: Context
) {
    private val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    suspend fun exportCsv(): Uri {
        val shots = shotRepository.getAllShots().first()
        val weights = weightRepository.getAllWeights().first()

        val sb = StringBuilder()
        sb.appendLine("type,date,value,unit,medication,site,microdose,notes")
        shots.forEach { shot ->
            val site = shot.injectionSite?.displayName?.replace(",", " ") ?: ""
            val notes = shot.notes.replace("\"", "\"\"")
            sb.appendLine(
                "shot,${fmt.format(Date(shot.timestampMs))},${shot.doseMg},mg," +
                "${shot.medicationType.displayName},$site,${shot.isMicrodose},\"$notes\""
            )
        }
        weights.forEach { w ->
            val notes = w.notes.replace("\"", "\"\"")
            sb.appendLine(
                "weight,${fmt.format(Date(w.timestampMs))},${w.weightLbs},lbs,,,,\"$notes\""
            )
        }

        return write("scilog_export.csv", sb.toString())
    }

    private fun write(filename: String, content: String): Uri {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val file = File(dir, filename).also { it.writeText(content) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}

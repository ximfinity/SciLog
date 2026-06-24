package com.scilog.app.presentation.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scilog.app.core.math.TwoCompartmentPKEngine
import com.scilog.app.domain.model.Symptom
import com.scilog.app.domain.model.SymptomType
import com.scilog.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DecayChartCanvas(
    actualPoints: List<TwoCompartmentPKEngine.PKPoint>,
    projectedPoints: List<TwoCompartmentPKEngine.PKPoint>,
    cMaxSS: Double,
    cMinSS: Double,
    symptoms: List<Symptom>,
    showSymptoms: Boolean,
    targetCmaxSS: Double = 0.0,
    targetCminSS: Double = 0.0,
    targetDoseMg: Double? = null,
    modifier: Modifier = Modifier
) {
    val allPoints    = actualPoints + projectedPoints
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val isDark       = LocalAppIsDark.current
    val eucalyptus   = Color(0xFF4F6B57)
    val chartBg      = if (isDark) Color(0xFF1C1915) else Color(0xFFF5F0E7)
    val charcoal     = if (isDark) Color(0xFFE8E3D8) else Color(0xFF2B2722)
    val cMaxColor    = Color(0xFF2D5A27)
    val cMinColor    = Color(0xFFB85C00)
    val targetColor  = Color(0xFF1A4A8A)
    val textMeasurer = rememberTextMeasurer()
    val dateFmt      = remember { SimpleDateFormat("M/d", Locale.US) }

    Canvas(modifier = modifier.fillMaxWidth().height(300.dp)) {
        if (allPoints.isEmpty()) return@Canvas

        val padL  = 50f; val padR = 10f
        val padT  = 20f; val padB = 36f
        val chartW = size.width - padL - padR
        val chartH = size.height - padT - padB

        // ── Rounded-rect background ───────────────────────────────────────
        drawRoundRect(
            color        = chartBg,
            topLeft      = Offset(0f, 0f),
            size         = size,
            cornerRadius = CornerRadius(12f, 12f)
        )

        val minTs  = allPoints.minOf { it.timestampMs }
        val maxTs  = allPoints.maxOf { it.timestampMs }
        val tsSpan = (maxTs - minTs).toDouble().coerceAtLeast(1.0)

        // Normalize to % of Cmax_SS when data is available
        val usePercent = cMaxSS > 0.0001
        fun normOf(concMgL: Double) = if (usePercent) concMgL / cMaxSS * 100.0 else concMgL

        // Bug fix: include targetCmaxSS so target lines above current dose don't clip
        val maxNorm = if (usePercent) {
            val pointsMax = allPoints.maxOf { normOf(it.concMgL) }.coerceAtLeast(100.0)
            val targetMax = if (targetCmaxSS > 0.0001) normOf(targetCmaxSS) else 0.0
            maxOf(pointsMax, targetMax) * 1.18
        } else {
            val pointsMax = allPoints.maxOf { it.concMgL }.coerceAtLeast(cMaxSS)
            val targetMax = if (targetCmaxSS > 0.0001) targetCmaxSS else 0.0
            (maxOf(pointsMax, targetMax) * 1.18).coerceAtLeast(0.01)
        }

        fun xOf(ts: Long)     = padL + ((ts - minTs) / tsSpan * chartW).toFloat()
        fun yOf(norm: Double) = padT + chartH - (norm / maxNorm * chartH).toFloat()

        val gridStyle     = TextStyle(fontSize = 9.sp, color = onSurface.copy(alpha = 0.45f))
        val tickStyle     = TextStyle(fontSize = 8.sp, color = onSurface.copy(alpha = 0.45f))
        val ssRefDash     = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
        val dotGridEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f))

        // ── Y-axis grid lines + labels ─────────────────────────────────────
        // Baseline
        drawLine(
            color = charcoal.copy(alpha = 0.18f),
            start = Offset(padL, padT + chartH),
            end = Offset(padL + chartW, padT + chartH),
            strokeWidth = 1.2f
        )
        if (usePercent) {
            for (pct in listOf(25, 50, 75, 100)) {
                val y = yOf(pct.toDouble())
                drawLine(
                    color = charcoal.copy(0.09f),
                    start = Offset(padL, y),
                    end = Offset(padL + chartW, y),
                    strokeWidth = 0.8f,
                    pathEffect = dotGridEffect
                )
                val m = textMeasurer.measure("${pct}%", gridStyle)
                drawText(m, topLeft = Offset(padL - m.size.width - 4f, y - m.size.height / 2f))
            }
            val unitM = textMeasurer.measure("% peak", TextStyle(fontSize = 8.sp, color = onSurface.copy(alpha = 0.4f)))
            drawText(unitM, topLeft = Offset(0f, padT - 14f))
        } else {
            for (i in 1..5) {
                val frac = i.toFloat() / 5f
                val y = padT + chartH * (1f - frac)
                val conc = maxNorm * frac
                drawLine(
                    color = charcoal.copy(0.09f),
                    start = Offset(padL, y),
                    end = Offset(padL + chartW, y),
                    strokeWidth = 0.8f,
                    pathEffect = dotGridEffect
                )
                val m = textMeasurer.measure("%.3f".format(conc), gridStyle)
                drawText(m, topLeft = Offset(padL - m.size.width - 4f, y - m.size.height / 2f))
            }
            val unitM = textMeasurer.measure("mg/L", TextStyle(fontSize = 8.sp, color = onSurface.copy(alpha = 0.4f)))
            drawText(unitM, topLeft = Offset(0f, padT - 14f))
        }

        // ── X-axis weekly tick marks ───────────────────────────────────────
        val weekMs = 7L * 86_400_000L
        var tickMs = minTs
        while (tickMs <= maxTs) {
            val tx = xOf(tickMs)
            drawLine(charcoal.copy(0.15f), Offset(tx, padT + chartH), Offset(tx, padT + chartH + 4f), 1f)
            val m = textMeasurer.measure(dateFmt.format(Date(tickMs)), tickStyle)
            drawText(m, topLeft = Offset(tx - m.size.width / 2f, padT + chartH + 6f))
            tickMs += weekMs
        }

        // ── Cmax SS reference line — label on LEFT (curve is low there) ────
        if (cMaxSS > 0.0001) {
            val refY = yOf(normOf(cMaxSS))
            drawLine(cMaxColor.copy(0.65f), Offset(padL, refY), Offset(padL + chartW, refY),
                strokeWidth = 1.4f, pathEffect = ssRefDash)
            val lbl1 = textMeasurer.measure(
                if (usePercent) "Peak SS" else "Cmax",
                TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = cMaxColor)
            )
            val lbl2 = textMeasurer.measure(
                if (usePercent) "100%" else "%.3f mg/L".format(cMaxSS),
                TextStyle(fontSize = 7.sp, color = cMaxColor)
            )
            val xL = padL + 5f
            drawText(lbl1, topLeft = Offset(xL, refY - lbl1.size.height - 1f))
            drawText(lbl2, topLeft = Offset(xL, refY + 2f))
        }

        // ── Cmin SS reference line — label on LEFT ─────────────────────────
        if (cMinSS > 0.0001) {
            val refY = yOf(normOf(cMinSS))
            drawLine(cMinColor.copy(0.65f), Offset(padL, refY), Offset(padL + chartW, refY),
                strokeWidth = 1.4f, pathEffect = ssRefDash)
            val minPct = (cMinSS / cMaxSS * 100).roundToInt()
            val lbl1 = textMeasurer.measure(
                if (usePercent) "Trough SS" else "Cmin",
                TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = cMinColor)
            )
            val lbl2 = textMeasurer.measure(
                if (usePercent) "${minPct}%" else "%.3f mg/L".format(cMinSS),
                TextStyle(fontSize = 7.sp, color = cMinColor)
            )
            val xL = padL + 5f
            drawText(lbl1, topLeft = Offset(xL, refY - lbl1.size.height - 1f))
            drawText(lbl2, topLeft = Offset(xL, refY + 2f))
        }

        // ── Target dose reference lines + shaded therapeutic band ──────────
        // Target labels go on the RIGHT side; current-dose labels are on LEFT,
        // so the two groups never compete for space.
        if (targetCmaxSS > 0.0001 && targetCminSS > 0.0001) {
            val yTop = yOf(normOf(targetCmaxSS))
            val yBot = yOf(normOf(targetCminSS))
            drawRect(
                color = targetColor.copy(alpha = 0.07f),
                topLeft = Offset(padL, yTop),
                size = Size(chartW, yBot - yTop)
            )
        }
        val doseTag = targetDoseMg?.let { "%.1fmg".format(it) } ?: ""
        if (targetCmaxSS > 0.0001) {
            val refY = yOf(normOf(targetCmaxSS))
            drawLine(targetColor.copy(0.55f), Offset(padL, refY), Offset(padL + chartW, refY),
                strokeWidth = 1.4f, pathEffect = ssRefDash)
            val label = if (doseTag.isNotEmpty()) "T.Peak  $doseTag" else "T.Peak"
            val pctStr = if (usePercent) "${normOf(targetCmaxSS).roundToInt()}%" else "%.3f mg/L".format(targetCmaxSS)
            val lbl1 = textMeasurer.measure(label,
                TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = targetColor))
            val lbl2 = textMeasurer.measure(pctStr,
                TextStyle(fontSize = 7.sp, color = targetColor))
            val xR = padL + chartW - max(lbl1.size.width, lbl2.size.width) - 6f
            drawText(lbl1, topLeft = Offset(xR, refY - lbl1.size.height - 1f))
            drawText(lbl2, topLeft = Offset(xR, refY + 2f))
        }
        if (targetCminSS > 0.0001) {
            val refY = yOf(normOf(targetCminSS))
            drawLine(targetColor.copy(0.45f), Offset(padL, refY), Offset(padL + chartW, refY),
                strokeWidth = 1.4f, pathEffect = ssRefDash)
            val label = if (doseTag.isNotEmpty()) "T.Trough  $doseTag" else "T.Trough"
            val pctStr = if (usePercent) "${normOf(targetCminSS).roundToInt()}%" else "%.3f mg/L".format(targetCminSS)
            val lbl1 = textMeasurer.measure(label,
                TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = targetColor))
            val lbl2 = textMeasurer.measure(pctStr,
                TextStyle(fontSize = 7.sp, color = targetColor))
            val xR = padL + chartW - max(lbl1.size.width, lbl2.size.width) - 6f
            drawText(lbl1, topLeft = Offset(xR, refY - lbl1.size.height - 1f))
            drawText(lbl2, topLeft = Offset(xR, refY + 2f))
        }

        // ── Filled area under actual curve ────────────────────────────────
        if (actualPoints.size > 1) {
            val fillPath = Path().apply {
                moveTo(xOf(actualPoints.first().timestampMs), yOf(normOf(actualPoints.first().concMgL)))
                actualPoints.drop(1).forEach { lineTo(xOf(it.timestampMs), yOf(normOf(it.concMgL))) }
                lineTo(xOf(actualPoints.last().timestampMs), padT + chartH)
                lineTo(xOf(actualPoints.first().timestampMs), padT + chartH)
                close()
            }
            drawPath(fillPath, brush = Brush.verticalGradient(
                listOf(eucalyptus.copy(0.28f), eucalyptus.copy(0.02f)),
                startY = padT, endY = padT + chartH
            ))
        }

        // ── Solid line — actual ───────────────────────────────────────────
        if (actualPoints.size > 1) {
            val path = Path().apply {
                moveTo(xOf(actualPoints.first().timestampMs), yOf(normOf(actualPoints.first().concMgL)))
                actualPoints.drop(1).forEach { lineTo(xOf(it.timestampMs), yOf(normOf(it.concMgL))) }
            }
            drawPath(path, eucalyptus, style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // ── Dashed line — projected ───────────────────────────────────────
        val bridgePt = actualPoints.lastOrNull()
        if (bridgePt != null && projectedPoints.isNotEmpty()) {
            val path = Path().apply {
                moveTo(xOf(bridgePt.timestampMs), yOf(normOf(bridgePt.concMgL)))
                projectedPoints.forEach { lineTo(xOf(it.timestampMs), yOf(normOf(it.concMgL))) }
            }
            drawPath(
                path, eucalyptus.copy(alpha = 0.38f),
                style = Stroke(2.0f, cap = StrokeCap.Round, join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)))
            )
        }

        // ── NOW vertical marker ───────────────────────────────────────────
        val nowMs = System.currentTimeMillis()
        val nowX  = xOf(nowMs)
        if (nowX in padL..(padL + chartW)) {
            drawLine(onSurface.copy(0.35f), Offset(nowX, padT), Offset(nowX, padT + chartH),
                strokeWidth = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            val nowM = textMeasurer.measure("NOW", TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Medium, color = onSurface.copy(0.35f)))
            drawText(nowM, topLeft = Offset(nowX + 3f, padT + 2f))
        }

        // ── Symptom overlay dots ──────────────────────────────────────────
        if (showSymptoms) {
            symptoms.forEach { symptom ->
                val sx = xOf(symptom.timestampMs)
                if (sx !in padL..(padL + chartW)) return@forEach
                val closest = allPoints.minByOrNull { kotlin.math.abs(it.timestampMs - symptom.timestampMs) }
                val sy = closest?.let { yOf(normOf(it.concMgL)) } ?: (padT + chartH / 2f)
                val dot = symptomColor(symptom.symptomType)
                drawCircle(dot.copy(0.85f), radius = 4f + symptom.severity * 1.5f, center = Offset(sx, sy))
                drawCircle(Color.White.copy(0.5f), radius = (4f + symptom.severity * 1.5f) * 0.4f, center = Offset(sx, sy))
            }
        }
    }
}

private fun symptomColor(type: SymptomType): Color = when (type) {
    SymptomType.NAUSEA               -> ChartNausea
    SymptomType.FATIGUE              -> ChartFatigue
    SymptomType.APPETITE_SUPPRESSION -> ChartAppetite
    SymptomType.HEADACHE             -> ChartHeadache
    else                             -> ChartDefault
}

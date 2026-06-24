package com.scilog.app.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scilog.app.core.math.TwoCompartmentPKEngine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HomeDecayChart(
    actualPoints: List<TwoCompartmentPKEngine.PKPoint>,
    projectedPoints: List<TwoCompartmentPKEngine.PKPoint>,
    cMaxSS: Double,
    cMinSS: Double,
    projectedDosesMs: List<Long>,
    targetCmaxSS: Double = 0.0,
    targetCminSS: Double = 0.0,
    modifier: Modifier = Modifier
) {
    if (actualPoints.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Log a shot to see your serum level forecast",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val nowMs        = System.currentTimeMillis()
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val cMaxColor    = Color(0xFF2E7D32)
    val cMinColor    = Color(0xFFE65100)
    val textMeasurer = rememberTextMeasurer()
    val dateFmt      = remember { SimpleDateFormat("M/d", Locale.US) }

    val allPoints  = actualPoints + projectedPoints
    val minTs      = allPoints.minOf { it.timestampMs }
    val maxTs      = allPoints.maxOf { it.timestampMs }
    val tsSpan     = (maxTs - minTs).toDouble().coerceAtLeast(1.0)

    // Normalize to % of Cmax_SS
    val usePercent = cMaxSS > 0.0001
    fun normOf(concMgL: Double) = if (usePercent) concMgL / cMaxSS * 100.0 else concMgL
    val maxNorm = if (usePercent) {
        allPoints.maxOf { normOf(it.concMgL) }.coerceAtLeast(100.0) * 1.18
    } else {
        (allPoints.maxOf { it.concMgL }.coerceAtLeast(cMaxSS) * 1.18).coerceAtLeast(0.01)
    }

    val ssRefDash = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))
    val nowDash   = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))
    val projDash  = PathEffect.dashPathEffect(floatArrayOf(10f, 7f))

    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val padL = 32f; val padR = 8f
        val padT = 10f; val padB = 28f
        val cW = size.width - padL - padR
        val cH = size.height - padT - padB

        fun xOf(ts: Long)     = padL + ((ts - minTs) / tsSpan * cW).toFloat()
        fun yOf(norm: Double) = padT + cH - (norm / maxNorm * cH).toFloat()

        val labelStyle = TextStyle(fontSize = 7.5.sp, color = onSurface.copy(alpha = 0.45f))

        // ── Subtle grid lines ─────────────────────────────────────────────
        drawLine(onSurface.copy(0.15f), Offset(padL, padT + cH), Offset(padL + cW, padT + cH), 1.2f)
        for (pct in listOf(50, 100)) {
            if (!usePercent) break
            val y = yOf(pct.toDouble())
            drawLine(onSurface.copy(0.05f), Offset(padL, y), Offset(padL + cW, y), 0.8f)
        }

        // ── X-axis weekly ticks ───────────────────────────────────────────
        val weekMs = 7L * 86_400_000L
        var tickMs = minTs
        while (tickMs <= maxTs) {
            val tx = xOf(tickMs)
            drawLine(onSurface.copy(0.12f), Offset(tx, padT + cH), Offset(tx, padT + cH + 3f), 1f)
            val m = textMeasurer.measure(dateFmt.format(Date(tickMs)), labelStyle)
            drawText(m, topLeft = Offset(tx - m.size.width / 2f, padT + cH + 4f))
            tickMs += weekMs
        }

        // ── Cmax SS reference line ────────────────────────────────────────
        if (cMaxSS > 0.0001) {
            val y = yOf(normOf(cMaxSS))
            drawLine(cMaxColor.copy(0.5f), Offset(padL, y), Offset(padL + cW, y), 1.2f, pathEffect = ssRefDash)
            val label = if (usePercent) "Peak" else "Cmax"
            val m = textMeasurer.measure(label, TextStyle(fontSize = 6.5.sp, color = cMaxColor.copy(0.8f)))
            drawText(m, topLeft = Offset(padL + cW - m.size.width - 2f, y - m.size.height - 1f))
        }

        // ── Cmin SS reference line ────────────────────────────────────────
        if (cMinSS > 0.0001) {
            val y = yOf(normOf(cMinSS))
            drawLine(cMinColor.copy(0.5f), Offset(padL, y), Offset(padL + cW, y), 1.2f, pathEffect = ssRefDash)
            val minPct = (cMinSS / cMaxSS * 100).roundToInt()
            val label = if (usePercent) "${minPct}%" else "Cmin"
            val m = textMeasurer.measure(label, TextStyle(fontSize = 6.5.sp, color = cMinColor.copy(0.8f)))
            drawText(m, topLeft = Offset(padL + cW - m.size.width - 2f, y + 1f))
        }

        // ── Target dose reference lines ───────────────────────────────────
        val targetColor = Color(0xFF1565C0)
        if (targetCmaxSS > 0.0001) {
            val y = yOf(normOf(targetCmaxSS))
            drawLine(targetColor.copy(0.5f), Offset(padL, y), Offset(padL + cW, y), 1.2f, pathEffect = ssRefDash)
            val m = textMeasurer.measure("T.Peak", TextStyle(fontSize = 6.5.sp, color = targetColor.copy(0.8f)))
            drawText(m, topLeft = Offset(padL - m.size.width - 2f, y - m.size.height - 1f))
        }
        if (targetCminSS > 0.0001) {
            val y = yOf(normOf(targetCminSS))
            drawLine(targetColor.copy(0.4f), Offset(padL, y), Offset(padL + cW, y), 1.2f, pathEffect = ssRefDash)
            val m = textMeasurer.measure("T.Trgh", TextStyle(fontSize = 6.5.sp, color = targetColor.copy(0.8f)))
            drawText(m, topLeft = Offset(padL - m.size.width - 2f, y + 1f))
        }

        // ── Filled area under actual curve ────────────────────────────────
        if (actualPoints.size > 1) {
            val fillPath = Path().apply {
                moveTo(xOf(actualPoints.first().timestampMs), yOf(normOf(actualPoints.first().concMgL)))
                actualPoints.drop(1).forEach { lineTo(xOf(it.timestampMs), yOf(normOf(it.concMgL))) }
                lineTo(xOf(actualPoints.last().timestampMs), padT + cH)
                lineTo(xOf(actualPoints.first().timestampMs), padT + cH)
                close()
            }
            drawPath(fillPath, Brush.verticalGradient(
                listOf(primaryColor.copy(0.28f), primaryColor.copy(0.02f)),
                startY = padT, endY = padT + cH
            ))
        }

        // ── Solid actual line ─────────────────────────────────────────────
        if (actualPoints.size > 1) {
            val path = Path().apply {
                moveTo(xOf(actualPoints.first().timestampMs), yOf(normOf(actualPoints.first().concMgL)))
                actualPoints.drop(1).forEach { lineTo(xOf(it.timestampMs), yOf(normOf(it.concMgL))) }
            }
            drawPath(path, primaryColor, style = Stroke(2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // ── Dashed projected line ─────────────────────────────────────────
        val bridge = actualPoints.lastOrNull()
        if (bridge != null && projectedPoints.isNotEmpty()) {
            val path = Path().apply {
                moveTo(xOf(bridge.timestampMs), yOf(normOf(bridge.concMgL)))
                projectedPoints.forEach { lineTo(xOf(it.timestampMs), yOf(normOf(it.concMgL))) }
            }
            drawPath(path, primaryColor.copy(0.40f),
                style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = projDash))
        }

        // ── NOW marker ────────────────────────────────────────────────────
        val nowX = xOf(nowMs)
        if (nowX in padL..(padL + cW)) {
            drawLine(onSurface.copy(0.28f), Offset(nowX, padT), Offset(nowX, padT + cH), 1.4f, pathEffect = nowDash)
            val nm = textMeasurer.measure("NOW", TextStyle(fontSize = 6.5.sp, color = onSurface.copy(0.35f)))
            drawText(nm, topLeft = Offset(nowX + 2f, padT + 1f))
        }

        // ── Y-axis unit label ─────────────────────────────────────────────
        val unitLabel = if (usePercent) "%" else "mg/L"
        val unitM = textMeasurer.measure(unitLabel, TextStyle(fontSize = 7.sp, color = onSurface.copy(0.35f), fontWeight = FontWeight.Medium))
        drawText(unitM, topLeft = Offset(0f, padT - 8f))
    }
}

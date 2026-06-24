package com.scilog.app.presentation.weight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.model.Weight
import com.scilog.app.presentation.theme.LocalAppIsDark
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ── Daily aggregation ─────────────────────────────────────────────────────────

data class DailyStats(
    val dayMs: Long,
    val avgLbs: Double,
    val minLbs: Double,
    val maxLbs: Double,
    val entries: List<Weight>
)

fun weightsByRange(weights: List<Weight>, range: WeightDateRange): List<Weight> {
    if (range.days == null) return weights.sortedBy { it.timestampMs }
    val cutoff = System.currentTimeMillis() - range.days * 86_400_000L
    return weights.filter { it.timestampMs >= cutoff }.sortedBy { it.timestampMs }
}

fun dailyStats(weights: List<Weight>): List<DailyStats> {
    val cal = Calendar.getInstance()
    return weights
        .groupBy { w ->
            cal.timeInMillis = w.timestampMs
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        .map { (dayMs, entries) ->
            DailyStats(
                dayMs   = dayMs,
                avgLbs  = entries.sumOf { it.weightLbs } / entries.size,
                minLbs  = entries.minOf { it.weightLbs },
                maxLbs  = entries.maxOf { it.weightLbs },
                entries = entries
            )
        }
        .sortedBy { it.dayMs }
}

// ── Linear regression → (slope mg/ms, intercept) ─────────────────────────────

data class Regression(val slope: Double, val intercept: Double) {
    fun yAt(x: Double) = slope * x + intercept
    fun xAt(y: Double) = if (abs(slope) < 1e-20) null else (y - intercept) / slope
}

fun linearRegression(points: List<Pair<Double, Double>>): Regression? {
    val n = points.size.toDouble()
    if (n < 2) return null
    val xMean = points.sumOf { it.first } / n
    val yMean = points.sumOf { it.second } / n
    val denom = points.sumOf { (it.first - xMean).pow(2) }
    if (denom < 1e-20) return null
    val slope = points.sumOf { (it.first - xMean) * (it.second - yMean) } / denom
    return Regression(slope, yMean - slope * xMean)
}

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun WeightFullChart(
    weights: List<Weight>,
    shots: List<Shot>,
    targetWeightLbs: Double?,
    showTrendLine: Boolean,
    showProjection: Boolean,
    showMinMax: Boolean,
    graphicsLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp
) {
    if (weights.isEmpty()) return

    val isDark        = LocalAppIsDark.current
    val onSurface     = MaterialTheme.colorScheme.onSurface
    val chartBg       = if (isDark) Color(0xFF1C1915) else Color(0xFFF5F0E7)
    val charcoal      = if (isDark) Color(0xFFE8E3D8) else Color(0xFF2B2722)
    val eucalyptus    = Color(0xFF4F6B57)
    val shotLineColor = Color(0xFF8B6B2B)
    val goalColor     = Color(0xFF2D5A27).copy(alpha = 0.60f)
    val textMeasurer  = rememberTextMeasurer()
    val monthFmt      = remember { SimpleDateFormat("MMM", Locale.US) }
    val weekFmt       = remember { SimpleDateFormat("M/d", Locale.US) }

    val sorted  = weights.sortedBy { it.timestampMs }
    val daily   = dailyStats(sorted)
    val regPts  = daily.map { it.dayMs.toDouble() to it.avgLbs }
    val regression = if (showTrendLine || showProjection) linearRegression(regPts) else null

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawWithContent {
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            }
    ) {
        val padL = 50f
        val padR = 16f
        val padT = 20f
        val padB = 56f
        val chartW = size.width - padL - padR
        val chartH = size.height - padT - padB

        // ── Rounded-rect background ───────────────────────────────────────
        drawRoundRect(
            color        = chartBg,
            topLeft      = Offset(0f, 0f),
            size         = size,
            cornerRadius = CornerRadius(12f, 12f)
        )

        val minTs = sorted.first().timestampMs
        val maxTs = maxOf(sorted.last().timestampMs, System.currentTimeMillis())
        val tsRange = (maxTs - minTs).toDouble().coerceAtLeast(1.0)

        val allLbs = sorted.map { it.weightLbs }
        val rawMin = allLbs.min()
        val rawMax = allLbs.max()
        // Include goal weight in Y range if visible
        val wMinRaw = if (targetWeightLbs != null) minOf(rawMin, targetWeightLbs) else rawMin
        val wMaxRaw = if (targetWeightLbs != null) maxOf(rawMax, targetWeightLbs) else rawMax
        val wPad    = (wMaxRaw - wMinRaw) * 0.12 + 1.0
        val wMin    = wMinRaw - wPad
        val wMax    = wMaxRaw + wPad
        val wRange  = (wMax - wMin).coerceAtLeast(0.1)

        fun xOf(ts: Long)    = padL + ((ts - minTs) / tsRange * chartW).toFloat()
        fun yOf(lbs: Double) = padT + chartH - ((lbs - wMin) / wRange * chartH).toFloat()

        val dotGridEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 7f))

        // ── Y-axis grid lines and labels ──────────────────────────────────
        val labelStyle = TextStyle(fontSize = 9.sp, color = onSurface.copy(alpha = 0.45f))
        val nGridLines = 4
        val gridStep   = (wMax - wMin) / nGridLines
        for (i in 0..nGridLines) {
            val lbs = wMin + i * gridStep
            val y   = yOf(lbs)
            drawLine(
                color       = charcoal.copy(0.09f),
                start       = Offset(padL, y),
                end         = Offset(padL + chartW, y),
                strokeWidth = 0.8f,
                pathEffect  = dotGridEffect
            )
            val m = textMeasurer.measure("%.0f".format(lbs), labelStyle)
            drawText(m, topLeft = Offset(padL - m.size.width - 4f, y - m.size.height / 2f))
        }

        // X-axis baseline
        drawLine(
            color       = charcoal.copy(alpha = 0.20f),
            start       = Offset(padL, padT + chartH),
            end         = Offset(padL + chartW, padT + chartH),
            strokeWidth = 1.2f
        )

        // ── X-axis ticks: monthly if range >= 50 days, else weekly ────────
        val rangeDays = (maxTs - minTs) / 86_400_000L
        val tickStyle = TextStyle(fontSize = 8.sp, color = onSurface.copy(alpha = 0.4f))

        if (rangeDays >= 50) {
            // Monthly ticks: first day of each month in the range
            val cal = Calendar.getInstance()
            cal.timeInMillis = minTs
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            // Advance to first month-start that is >= minTs
            if (cal.timeInMillis < minTs) cal.add(Calendar.MONTH, 1)
            while (cal.timeInMillis <= maxTs) {
                val tx = xOf(cal.timeInMillis)
                drawLine(charcoal.copy(0.15f), Offset(tx, padT + chartH), Offset(tx, padT + chartH + 4f), 1f)
                val m = textMeasurer.measure(monthFmt.format(cal.time), tickStyle)
                drawText(m, topLeft = Offset(tx - m.size.width / 2f, padT + chartH + 5f))
                cal.add(Calendar.MONTH, 1)
            }
        } else {
            // Weekly ticks
            val weekMs = 7L * 86_400_000L
            var tickMs = minTs
            while (tickMs <= maxTs) {
                val tx = xOf(tickMs)
                drawLine(charcoal.copy(0.15f), Offset(tx, padT + chartH), Offset(tx, padT + chartH + 4f), 1f)
                val m = textMeasurer.measure(weekFmt.format(Date(tickMs)), tickStyle)
                drawText(m, topLeft = Offset(tx - m.size.width / 2f, padT + chartH + 5f))
                tickMs += weekMs
            }
        }

        // ── Goal weight horizontal line ───────────────────────────────────
        if (targetWeightLbs != null) {
            val y = yOf(targetWeightLbs)
            drawLine(
                color       = goalColor,
                start       = Offset(padL, y),
                end         = Offset(padL + chartW, y),
                strokeWidth = 1.5f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
            val gm = textMeasurer.measure(
                "Goal  %.0f".format(targetWeightLbs),
                TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Medium, color = goalColor)
            )
            drawText(gm, topLeft = Offset(padL + 3f, y - gm.size.height - 2f))
        }

        // ── Shot markers — staggered two-row labels to avoid overlap ─────
        val shotLabelStyle = TextStyle(fontSize = 7.5.sp, color = shotLineColor, fontWeight = FontWeight.SemiBold)
        val baselineY = padT + chartH
        shots.filter { it.timestampMs in minTs..maxTs }.forEachIndexed { idx, shot ->
            val x = xOf(shot.timestampMs)
            drawLine(
                color       = shotLineColor.copy(alpha = 0.6f),
                start       = Offset(x, padT),
                end         = Offset(x, baselineY),
                strokeWidth = 1.2f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
            val sm = textMeasurer.measure("${shot.doseMg}mg", shotLabelStyle)
            // Alternate between two rows; center label on shot line
            val labelY = if (idx % 2 == 0) baselineY + 14f else baselineY + 30f
            drawText(sm, topLeft = Offset(x - sm.size.width / 2f, labelY))
        }

        // ── Projection line ───────────────────────────────────────────────
        if (showProjection && regression != null && regression.slope < 0) {
            val lastDay = daily.last()
            val projEndMs: Long = if (targetWeightLbs != null) {
                val goalMs = regression.xAt(targetWeightLbs)
                if (goalMs != null && goalMs > lastDay.dayMs) minOf(goalMs.toLong(), maxTs + 90L * 86_400_000L)
                else maxTs + 90L * 86_400_000L
            } else {
                maxTs + 90L * 86_400_000L
            }
            val projStartX = xOf(lastDay.dayMs)
            val projStartY = yOf(lastDay.avgLbs)
            val projEndLbs = regression.yAt(projEndMs.toDouble()).coerceIn(wMin, wMax)
            val projEndX   = xOf(projEndMs)
            val projEndY   = yOf(projEndLbs)
            val projPath   = Path().apply {
                moveTo(projStartX, projStartY)
                lineTo(projEndX.coerceIn(padL, padL + chartW), projEndY)
            }
            drawPath(
                path   = projPath,
                color  = eucalyptus.copy(alpha = 0.40f),
                style  = Stroke(
                    width      = 1.8f,
                    cap        = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            )
        }

        // ── Min/max error bars ────────────────────────────────────────────
        if (showMinMax) {
            daily.forEach { ds ->
                if (ds.minLbs < ds.maxLbs) {
                    val x     = xOf(ds.dayMs)
                    val yTop  = yOf(ds.maxLbs)
                    val yBot  = yOf(ds.minLbs)
                    drawLine(
                        color       = eucalyptus.copy(alpha = 0.22f),
                        start       = Offset(x, yTop),
                        end         = Offset(x, yBot),
                        strokeWidth = 2f,
                        cap         = StrokeCap.Round
                    )
                    drawLine(eucalyptus.copy(0.22f), Offset(x - 3f, yTop), Offset(x + 3f, yTop), 1.5f)
                    drawLine(eucalyptus.copy(0.22f), Offset(x - 3f, yBot), Offset(x + 3f, yBot), 1.5f)
                }
            }
        }

        // ── Trend line (linear regression) ───────────────────────────────
        if (showTrendLine && regression != null && daily.size >= 2) {
            val x0 = xOf(minTs)
            val x1 = xOf(maxTs)
            val y0 = yOf(regression.yAt(minTs.toDouble()).coerceIn(wMin, wMax))
            val y1 = yOf(regression.yAt(maxTs.toDouble()).coerceIn(wMin, wMax))
            drawLine(
                color       = eucalyptus.copy(alpha = 0.55f),
                start       = Offset(x0, y0),
                end         = Offset(x1, y1),
                strokeWidth = 1.8f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
            )
        }

        // ── Main daily-average line ───────────────────────────────────────
        if (daily.size > 1) {
            val fillPath = Path().apply {
                moveTo(xOf(daily.first().dayMs), yOf(daily.first().avgLbs))
                daily.drop(1).forEach { lineTo(xOf(it.dayMs), yOf(it.avgLbs)) }
                lineTo(xOf(daily.last().dayMs), padT + chartH)
                lineTo(xOf(daily.first().dayMs), padT + chartH)
                close()
            }
            drawPath(
                fillPath,
                Brush.verticalGradient(
                    listOf(eucalyptus.copy(0.18f), eucalyptus.copy(0.01f)),
                    startY = padT, endY = padT + chartH
                )
            )
            val linePath = Path().apply {
                moveTo(xOf(daily.first().dayMs), yOf(daily.first().avgLbs))
                daily.drop(1).forEach { lineTo(xOf(it.dayMs), yOf(it.avgLbs)) }
            }
            drawPath(linePath, eucalyptus, style = Stroke(2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // ── Open-circle day markers at each daily average point ───────────
        val ivoryFill = if (isDark) Color(0xFF1C1915) else Color(0xFFF5F0E7)
        daily.forEach { ds ->
            val cx = xOf(ds.dayMs)
            val cy = yOf(ds.avgLbs)
            drawCircle(eucalyptus, radius = 3.5f, center = Offset(cx, cy))
            drawCircle(ivoryFill, radius = 3.5f - 1.2f, center = Offset(cx, cy))
        }
    }
}

private fun DrawScope.drawDot(x: Float, y: Float, outerColor: Color, innerColor: Color, r: Float) {
    drawCircle(outerColor, radius = r, center = Offset(x, y))
    drawCircle(innerColor, radius = r * 0.55f, center = Offset(x, y))
}

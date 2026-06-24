package com.scilog.app.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scilog.app.domain.model.Shot
import com.scilog.app.domain.model.Weight
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeightProgressChart(
    weights: List<Weight>,
    shots: List<Shot>,
    modifier: Modifier = Modifier
) {
    if (weights.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.primary
    val shotColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val textMeasurer = rememberTextMeasurer()
    val dateFmt = remember { SimpleDateFormat("M/d", Locale.US) }

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(160.dp)
    ) {
        val padL = 44f
        val padR = 12f
        val padT = 14f
        val padB = 28f
        val chartW = size.width - padL - padR
        val chartH = size.height - padT - padB

        val sortedWeights = weights.sortedBy { it.timestampMs }
        val minTs = sortedWeights.first().timestampMs
        val maxTs = maxOf(sortedWeights.last().timestampMs, System.currentTimeMillis())
        val tsRange = (maxTs - minTs).toDouble().coerceAtLeast(1.0)

        val minW = sortedWeights.minOf { it.weightLbs }
        val maxW = sortedWeights.maxOf { it.weightLbs }
        val wPad = (maxW - minW) * 0.12 + 1.0
        val wMin = minW - wPad
        val wMax = maxW + wPad
        val wRange = (wMax - wMin).coerceAtLeast(0.1)

        fun xOf(ts: Long) = padL + ((ts - minTs) / tsRange * chartW).toFloat()
        fun yOf(lbs: Double) = padT + chartH - ((lbs - wMin) / wRange * chartH).toFloat()

        // Y axis grid lines and labels at min, mid, max
        val yLevels = listOf(wMin + wPad, (wMin + wMax) / 2, wMax - wPad)
        val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor, fontWeight = FontWeight.Normal)
        yLevels.forEach { lbs ->
            val y = yOf(lbs)
            drawLine(axisColor, Offset(padL, y), Offset(padL + chartW, y), strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            val label = "%.0f".format(lbs)
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(measured, topLeft = Offset(padL - measured.size.width - 4f, y - measured.size.height / 2f))
        }

        // X axis
        drawLine(axisColor, Offset(padL, padT + chartH), Offset(padL + chartW, padT + chartH), strokeWidth = 1f)

        // Shot injection markers — vertical dashed lines with dosage label
        val shotLineColor = shotColor
        val shotStyle = TextStyle(fontSize = 8.sp, color = shotColor, fontWeight = FontWeight.SemiBold)
        shots.filter { it.timestampMs in minTs..maxTs }.forEach { shot ->
            val x = xOf(shot.timestampMs)
            drawLine(
                color = shotLineColor,
                start = Offset(x, padT),
                end = Offset(x, padT + chartH),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
            val doseLabel = "${shot.doseMg}mg"
            val measured = textMeasurer.measure(doseLabel, shotStyle)
            drawText(measured, topLeft = Offset(x + 2f, padT + 2f))
        }

        // Weight line
        val linePath = Path()
        sortedWeights.forEachIndexed { i, w ->
            val x = xOf(w.timestampMs)
            val y = yOf(w.weightLbs)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, lineColor, style = Stroke(width = 2.5f))

        // Weight dots + x-axis date labels
        val xLabelStyle = TextStyle(fontSize = 8.sp, color = labelColor)
        sortedWeights.forEachIndexed { i, w ->
            val x = xOf(w.timestampMs)
            val y = yOf(w.weightLbs)
            drawCircle(dotColor, radius = 4.5f, center = Offset(x, y))
            drawCircle(Color.White, radius = 2.5f, center = Offset(x, y))

            // Only label first, last, and every ~4th point to avoid crowding
            if (i == 0 || i == sortedWeights.lastIndex || i % 4 == 0) {
                val dateLabel = dateFmt.format(Date(w.timestampMs))
                val measured = textMeasurer.measure(dateLabel, xLabelStyle)
                drawText(measured, topLeft = Offset(x - measured.size.width / 2f, padT + chartH + 4f))
            }
        }
    }
}

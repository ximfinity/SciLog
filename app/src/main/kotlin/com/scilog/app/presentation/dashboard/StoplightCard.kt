package com.scilog.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scilog.app.presentation.theme.ErrorRed
import com.scilog.app.presentation.theme.LocalAppIsDark
import com.scilog.app.presentation.theme.SuccessGreen
import com.scilog.app.presentation.theme.WarningAmber

@Composable
fun StoplightCard(
    level: StoplightLevel,
    label: String,
    description: String,
    currentMg: Double,
    fractionOfPeak: Double,
    hoursUntilNextDose: Double?,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppIsDark.current
    val (cardColor, textColor) = when (level) {
        StoplightLevel.GREEN ->
            if (isDark) Color(0xFF0D2B1E) to SuccessGreen
            else Color(0xFFDCFCE7) to Color(0xFF14532D)
        StoplightLevel.YELLOW ->
            if (isDark) Color(0xFF2B2000) to WarningAmber
            else Color(0xFFFEF9C3) to Color(0xFF713F12)
        StoplightLevel.RED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Traffic light column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TrafficDot(active = level == StoplightLevel.GREEN, color = SuccessGreen, activeSize = 22.dp)
                TrafficDot(active = level == StoplightLevel.YELLOW, color = WarningAmber, activeSize = 22.dp)
                TrafficDot(active = level == StoplightLevel.RED, color = ErrorRed, activeSize = 22.dp)
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "Medication Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.65f)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip("%.3f mg".format(currentMg), textColor)
                    if (fractionOfPeak > 0) StatChip("%.0f%% of peak".format(fractionOfPeak * 100), textColor)
                    hoursUntilNextDose?.let { h ->
                        val d = (h / 24).toInt(); val hrs = (h % 24).toInt()
                        val label = if (d > 0) "Next: ${d}d ${hrs}h" else "Next: ${hrs}h"
                        StatChip(label, textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficDot(active: Boolean, color: Color, activeSize: Dp) {
    val size = if (active) activeSize else (activeSize.value * 0.6f).dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) color else color.copy(alpha = 0.15f))
    )
}

@Composable
private fun StatChip(text: String, textColor: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = textColor.copy(alpha = 0.65f)
    )
}

package com.scilog.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scilog.app.domain.usecase.shot.DosageInsight
import com.scilog.app.domain.usecase.shot.EscalationStatus

@Composable
fun DosageInsightCard(
    insight: DosageInsight,
    onOpenCatchUpCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor, icon) = when (insight.escalationStatus) {
        EscalationStatus.CONSIDER_ESCALATION ->
            Triple(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                Icons.Outlined.TrendingUp
            )
        EscalationStatus.ON_TRACK ->
            Triple(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                Icons.Outlined.CheckCircle
            )
        EscalationStatus.AT_MAX_DOSE ->
            Triple(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                Icons.Outlined.Star
            )
        EscalationStatus.TOO_EARLY ->
            Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Outlined.Schedule
            )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
                Text(
                    "Dosage Outlook",
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
            }

            // Stat chips row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightChip(
                    label = "Weeks at dose",
                    value = "${insight.weeksAtCurrentDose}w",
                    contentColor = contentColor
                )
                val lossStr = insight.weightChangePctSinceDoseChange
                    ?.let { "%.1f%%".format(-it) } ?: "—"
                InsightChip(
                    label = "Body wt lost",
                    value = lossStr,
                    contentColor = contentColor
                )
                if (insight.dosesUntilSS > 0) {
                    InsightChip(
                        label = "SS progress",
                        value = "${insight.currentDoseNumber}/${insight.dosesUntilSS} doses",
                        contentColor = contentColor
                    )
                }
            }

            Text(
                insight.message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.85f)
            )

            if (insight.escalationStatus == EscalationStatus.CONSIDER_ESCALATION) {
                TextButton(
                    onClick = onOpenCatchUpCalculator,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Catch-up Calculator →",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightChip(label: String, value: String, contentColor: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium, color = contentColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.65f))
    }
}

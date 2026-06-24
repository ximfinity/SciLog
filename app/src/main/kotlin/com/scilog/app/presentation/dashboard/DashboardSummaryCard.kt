package com.scilog.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scilog.app.domain.usecase.shot.DosageInsight
import com.scilog.app.domain.usecase.shot.EscalationStatus
import com.scilog.app.domain.usecase.weight.GetWeightGuidanceUseCase
import com.scilog.app.presentation.theme.ErrorRed
import com.scilog.app.presentation.theme.LocalAppIsDark
import com.scilog.app.presentation.theme.SuccessGreen
import com.scilog.app.presentation.theme.WarningAmber

@Composable
fun DashboardSummaryCard(
    stoplight: StoplightLevel,
    stoplightLabel: String,
    stoplightDescription: String,
    currentMg: Double,
    fractionOfPeak: Double,
    hoursUntilNextDose: Double?,
    dosageInsight: DosageInsight?,
    weightGuidance: GetWeightGuidanceUseCase.GuidanceResult?,
    latestWeightLbs: Double?,
    onNavigateToCatchUpDose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            // ── Medication Status ──────────────────────────────────────────
            MedicationStatusRow(
                stoplight            = stoplight,
                stoplightLabel       = stoplightLabel,
                stoplightDescription = stoplightDescription,
                currentMg            = currentMg,
                fractionOfPeak       = fractionOfPeak,
                hoursUntilNextDose   = hoursUntilNextDose
            )

            // ── Dosage Outlook ─────────────────────────────────────────────
            if (dosageInsight != null) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                DosageOutlookRow(
                    insight              = dosageInsight,
                    onOpenCatchUpCalc    = onNavigateToCatchUpDose
                )
            }

            // ── Weight Progress ────────────────────────────────────────────
            if (weightGuidance != null &&
                weightGuidance.guidance != GetWeightGuidanceUseCase.Guidance.INSUFFICIENT_DATA) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                WeightProgressRow(
                    guidance        = weightGuidance,
                    latestWeightLbs = latestWeightLbs
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Medication Status row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MedicationStatusRow(
    stoplight: StoplightLevel,
    stoplightLabel: String,
    stoplightDescription: String,
    currentMg: Double,
    fractionOfPeak: Double,
    hoursUntilNextDose: Double?
) {
    val isDark = LocalAppIsDark.current
    val (bgColor, textColor) = when (stoplight) {
        StoplightLevel.GREEN ->
            if (isDark) Color(0xFF0D2B1E) to SuccessGreen
            else Color(0xFFDCFCE7) to Color(0xFF14532D)
        StoplightLevel.YELLOW ->
            if (isDark) Color(0xFF2B2000) to WarningAmber
            else Color(0xFFFEF9C3) to Color(0xFF713F12)
        StoplightLevel.RED ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status dot
        val dotColor = when (stoplight) {
            StoplightLevel.GREEN  -> SuccessGreen
            StoplightLevel.YELLOW -> WarningAmber
            StoplightLevel.RED    -> ErrorRed
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Medication Status",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.65f)
            )
            Text(
                stoplightLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
            Text(
                stoplightDescription,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f)
            )
        }

        // Quick stats column
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("%.3f mg".format(currentMg), style = MaterialTheme.typography.labelSmall, color = textColor)
            if (fractionOfPeak > 0) {
                Text("%.0f%% peak".format(fractionOfPeak * 100), style = MaterialTheme.typography.labelSmall, color = textColor.copy(0.75f))
            }
            hoursUntilNextDose?.let { h ->
                val d = (h / 24).toInt(); val hrs = (h % 24).toInt()
                val label = if (d > 0) "${d}d ${hrs}h" else "${hrs}h"
                Text("Next: $label", style = MaterialTheme.typography.labelSmall, color = textColor.copy(0.75f))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dosage Outlook row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DosageOutlookRow(
    insight: DosageInsight,
    onOpenCatchUpCalc: () -> Unit
) {
    val (accent, icon) = when (insight.escalationStatus) {
        EscalationStatus.CONSIDER_ESCALATION -> MaterialTheme.colorScheme.error to Icons.Outlined.TrendingUp
        EscalationStatus.ON_TRACK            -> MaterialTheme.colorScheme.secondary to Icons.Outlined.CheckCircle
        EscalationStatus.AT_MAX_DOSE         -> MaterialTheme.colorScheme.tertiary to Icons.Outlined.Star
        EscalationStatus.TOO_EARLY           -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Outlined.Schedule
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Dosage Outlook",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStatCol("Wk ${insight.weeksAtCurrentDose}", "at dose")
                insight.weightChangePctSinceDoseChange?.let { pct ->
                    SummaryStatCol("%.1f%%".format(-pct), "body wt lost")
                }
                if (insight.dosesUntilSS > 0) {
                    SummaryStatCol("${insight.currentDoseNumber}/${insight.dosesUntilSS}", "SS doses")
                }
            }
            Text(
                insight.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (insight.escalationStatus == EscalationStatus.CONSIDER_ESCALATION) {
                TextButton(onClick = onOpenCatchUpCalc, contentPadding = PaddingValues(0.dp)) {
                    Text("Catch-up Calculator →", style = MaterialTheme.typography.labelSmall, color = accent)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weight Progress row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WeightProgressRow(
    guidance: GetWeightGuidanceUseCase.GuidanceResult,
    latestWeightLbs: Double?
) {
    val (accent, icon) = when (guidance.guidance) {
        GetWeightGuidanceUseCase.Guidance.ON_TRACK            -> MaterialTheme.colorScheme.secondary to Icons.Outlined.TrendingDown
        GetWeightGuidanceUseCase.Guidance.CONSIDER_DOSE_INCREASE -> MaterialTheme.colorScheme.error to Icons.Outlined.Warning
        GetWeightGuidanceUseCase.Guidance.WEIGHT_LOSS_TOO_RAPID  -> MaterialTheme.colorScheme.tertiary to Icons.Outlined.TrendingDown
        else -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Outlined.FitnessCenter
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp).padding(top = 2.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Weight Progress",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                latestWeightLbs?.let { SummaryStatCol("%.1f lbs".format(it), "current") }
                guidance.weeklyLossLbs?.let { wl ->
                    SummaryStatCol("%.1f lbs/wk".format(wl), "avg loss")
                }
            }
            Text(
                guidance.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryStatCol(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
    }
}

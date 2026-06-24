package com.scilog.app.presentation.import_

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val SHOTS_PROMPT = """
I need to format my GLP-1 injection history as CSV for import into a tracking app.

Please output ONLY a CSV with these exact column headers (first row):
date,dose_mg,medication,notes

Rules:
- date: YYYY-MM-DD HH:MM:SS format (e.g. 2025-06-01 09:00:00)
- dose_mg: number in milligrams (e.g. 2.5)
- medication: must be exactly TIRZEPATIDE, SEMAGLUTIDE, or CUSTOM
- notes: optional, leave blank if none

Example:
date,dose_mg,medication,notes
2025-06-01 09:00:00,2.5,TIRZEPATIDE,Started new pen
2025-06-08 09:15:00,5.0,TIRZEPATIDE,

Here is my injection data:
[describe your injections — dates, doses, medication name]
""".trimIndent()

private val WEIGHTS_PROMPT = """
I need to format my weight log as CSV for import into a tracking app.

Please output ONLY a CSV with these exact column headers (first row):
date,weight_lbs,notes

Rules:
- date: YYYY-MM-DD HH:MM:SS format (e.g. 2025-06-01 07:30:00)
- weight_lbs: weight in pounds (e.g. 225.5)
- notes: optional, leave blank if none

Example:
date,weight_lbs,notes
2025-06-01 07:30:00,245.0,Morning weigh-in
2025-06-08 07:45:00,243.5,

Here is my weight data:
[describe your weigh-ins — dates and weights]
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(onBack: () -> Unit, viewModel: ImportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var showPromptDialog by remember { mutableStateOf<String?>(null) }  // null=hidden, "shots"/"weights"
    var pendingSharePrompt by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pendingSharePrompt?.let { promptText ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, promptText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share with AI"))
            }
            pendingSharePrompt = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Data") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Paste your injection or weight data in CSV format. Don't have it formatted yet? Use the AI prompt generator below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
            )

            // AI prompt buttons
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Generate with AI",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        "Copy a prompt below and paste it into ChatGPT or any AI chatbot. The AI will format your data into the correct CSV — then paste it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.8f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showPromptDialog = "shots" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Vaccines, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Shots Prompt", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { showPromptDialog = "weights" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.FitnessCenter, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Weights Prompt", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            when (val s = state) {
                is ImportUiState.Idle, is ImportUiState.Error -> {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Paste CSV here") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                        minLines = 8
                    )
                    if (s is ImportUiState.Error) {
                        Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { viewModel.parseContent(inputText, false) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = inputText.isNotBlank()
                    ) { Text("Preview Import") }
                }
                is ImportUiState.Processing -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ImportUiState.Preview -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ready to Import", style = MaterialTheme.typography.titleMedium)
                            Text("${s.shots} injections", style = MaterialTheme.typography.bodyMedium)
                            Text("${s.weights} weight entries", style = MaterialTheme.typography.bodyMedium)
                            if (s.errors.isNotEmpty()) {
                                Text("${s.errors.size} rows skipped (parse errors)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                s.errors.take(3).forEach { err ->
                                    Text("• $err", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(onClick = { viewModel.confirmImport() }, modifier = Modifier.weight(1f)) { Text("Confirm Import") }
                    }
                }
                is ImportUiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Import Complete", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("${s.shotsImported} shots and ${s.weightsImported} weights added.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
                }
            }
        }
    }

    // AI prompt dialog
    showPromptDialog?.let { type ->
        val promptText = if (type == "shots") SHOTS_PROMPT else WEIGHTS_PROMPT
        val title = if (type == "shots") "Shots Import Prompt" else "Weights Import Prompt"
        AlertDialog(
            onDismissRequest = { showPromptDialog = null },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Copy this prompt and paste it into ChatGPT, or tap 'Share with Screenshot' to attach a screen capture from another app and send both to your AI chatbot.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        SelectionContainer {
                            Text(
                                promptText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Import Prompt", promptText))
                    showPromptDialog = null
                }) { Text("Copy & Close") }
            },
            dismissButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            pendingSharePrompt = promptText
                            showPromptDialog = null
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Image, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share with Screenshot")
                    }
                    TextButton(
                        onClick = { showPromptDialog = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Close") }
                }
            }
        )
    }
}

package com.mathieu.blocker.ui.screens

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mathieu.blocker.data.BlockerPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToAppList: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isServiceEnabled by remember { mutableStateOf(false) }
    var blockedAppCount by remember { mutableIntStateOf(0) }
    var timerSeconds by remember { mutableIntStateOf(10) }
    var sliderValue by remember { mutableFloatStateOf(10f) }

    // Poll service status (no broadcast available for accessibility service state)
    LaunchedEffect(Unit) {
        while (true) {
            isServiceEnabled = isAccessibilityServiceEnabled(context)
            delay(2000L)
        }
    }

    // Observe preferences
    LaunchedEffect(Unit) {
        BlockerPreferences.getBlockedApps(context).collect { apps ->
            blockedAppCount = apps.size
        }
    }
    LaunchedEffect(Unit) {
        BlockerPreferences.getTimerSeconds(context).collect { seconds ->
            timerSeconds = seconds
            sliderValue = seconds.toFloat()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Blocker") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Service status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isServiceEnabled) Icons.Default.CheckCircle
                        else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isServiceEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isServiceEnabled) "Service actif" else "Service inactif",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isServiceEnabled)
                                "Blocker surveille les ouvertures d'apps"
                            else
                                "Active le service d'accessibilit\u00e9 pour commencer",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (!isServiceEnabled) {
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Text("Ouvrir les param\u00e8tres d'accessibilit\u00e9")
                    }
                }
            }

            // Timer configuration
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "D\u00e9lai d'attente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${sliderValue.roundToInt()} secondes",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            val newValue = sliderValue.roundToInt()
                            scope.launch {
                                BlockerPreferences.setTimerSeconds(context, newValue)
                            }
                        },
                        valueRange = 5f..60f,
                        steps = 10
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5s", style = MaterialTheme.typography.bodySmall)
                        Text("60s", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Blocked apps info
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$blockedAppCount app(s) bloqu\u00e9e(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "S\u00e9lectionne les apps que tu veux contr\u00f4ler",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                OutlinedButton(
                    onClick = onNavigateToAppList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Text("G\u00e9rer les applications")
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(
        AccessibilityServiceInfo.FEEDBACK_GENERIC
    )
    return enabledServices.any {
        it.resolveInfo.serviceInfo.packageName == context.packageName
    }
}

package com.mathieu.blocker.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import com.mathieu.blocker.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mathieu.blocker.data.BlockerPreferences
import com.mathieu.blocker.data.PlannerEntry
import com.mathieu.blocker.data.Profile
import com.mathieu.blocker.data.db.BlockerDatabase
import com.mathieu.blocker.data.db.UsageStatsEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateToAppList: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { dateFormat.format(java.util.Date()) }
    val sevenDaysAgo = remember {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -6)
        dateFormat.format(cal.time)
    }
    val dao = remember { BlockerDatabase.getInstance(context).usageStatsDao() }

    var profiles by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var activeProfileId by remember { mutableStateOf("default_home") }
    var plannerEntries by remember { mutableStateOf<List<PlannerEntry>>(emptyList()) }
    var dailyLimit by remember { mutableIntStateOf(0) }
    var dailyLimitSlider by remember { mutableFloatStateOf(0f) }
    var weeklyTotals by remember { mutableStateOf<UsageStatsEntity?>(null) }
    var showAddPlannerDialog by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    var editingPlannerEntry by remember { mutableStateOf<PlannerEntry?>(null) }

    LaunchedEffect(Unit) { BlockerPreferences.getProfiles(context).collect { profiles = it } }
    LaunchedEffect(Unit) { BlockerPreferences.getActiveProfileId(context).collect { activeProfileId = it } }
    LaunchedEffect(Unit) { BlockerPreferences.getPlanner(context).collect { plannerEntries = it } }
    LaunchedEffect(Unit) {
        BlockerPreferences.getDailyScrollLimit(context).collect { dailyLimit = it; dailyLimitSlider = it.toFloat() }
    }
    LaunchedEffect(Unit) {
        weeklyTotals = dao.getTotalsForRange(sevenDaysAgo, today)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Profiles management
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profils", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chaque profil a ses propres apps bloquées, délai d'attente.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                profiles.forEach { profile ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (profile.id == activeProfileId)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    profile.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${profile.blockedApps.size} app(s) · ${profile.timerSeconds}s" +
                                            if (profile.challengeEnabled) " · Challenge ${profile.challengeLength} car." else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (profile.id == activeProfileId) {
                                Text(
                                    "Actif", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { editingProfile = profile }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier")
                            }
                            if (profile.id != "default_home") {
                                IconButton(onClick = {
                                    scope.launch { BlockerPreferences.deleteProfile(context, profile.id) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAddProfileDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nouveau profil")
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onNavigateToAppList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gérer les apps du profil actif")
                }
            }
        }

        // Planner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Planificateur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bascule automatiquement entre les profils selon l'heure et le jour.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (plannerEntries.isEmpty()) {
                    Text(
                        text = "Aucune entrée. Ajoute des plages horaires pour activer le planificateur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                plannerEntries.forEach { entry ->
                    val profileName = profiles.find { it.id == entry.profileId }?.name ?: "?"
                    PlannerEntryItem(
                        entry = entry,
                        profileName = profileName,
                        onEdit = { editingPlannerEntry = entry },
                        onDelete = { scope.launch { BlockerPreferences.removePlannerEntry(context, entry.id) } }
                    )
                }

                OutlinedButton(
                    onClick = { showAddPlannerDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajouter une plage")
                }
            }
        }

        // Daily scroll limit
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Limite quotidienne de scrolls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    InfoIcon("Une fois cette limite atteinte, toutes les apps bloquées deviennent totalement inaccessibles jusqu'à minuit.")
                }
                Text(
                    text = if (dailyLimit == 0) "Pas de limite" else "$dailyLimit scrolls/jour",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = dailyLimitSlider,
                    onValueChange = { dailyLimitSlider = it },
                    onValueChangeFinished = {
                        scope.launch { BlockerPreferences.setDailyScrollLimit(context, dailyLimitSlider.roundToInt()) }
                    },
                    valueRange = 0f..1000f, steps = 19
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Illimité", style = MaterialTheme.typography.bodySmall)
                    Text("1000", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Objectif : sauver X heures/mois
        val weeklyTotalTimeMs = weeklyTotals?.scrollTimeMs ?: 0L
        val weeklyTotalScrolls = weeklyTotals?.scrollCount ?: 0
        if (weeklyTotalScrolls > 0 && weeklyTotalTimeMs > 0) {
            SaveHoursGoalCard(
                weeklyTotalTimeMs = weeklyTotalTimeMs,
                weeklyTotalScrolls = weeklyTotalScrolls,
                currentLimit = dailyLimit,
                onSetLimit = { limit ->
                    scope.launch { BlockerPreferences.setDailyScrollLimit(context, limit) }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAddPlannerDialog) {
        AddPlannerDialog(
            profiles = profiles,
            onDismiss = { showAddPlannerDialog = false },
            onAdd = { entry ->
                scope.launch { BlockerPreferences.addPlannerEntry(context, entry) }
                showAddPlannerDialog = false
            }
        )
    }

    if (showAddProfileDialog) {
        AddProfileDialog(
            onDismiss = { showAddProfileDialog = false },
            onAdd = { name ->
                scope.launch { BlockerPreferences.addProfile(context, Profile(name = name)) }
                showAddProfileDialog = false
            }
        )
    }

    editingProfile?.let { profile ->
        EditProfileDialog(
            profile = profile,
            onDismiss = { editingProfile = null },
            onSave = { updated ->
                scope.launch { BlockerPreferences.updateProfile(context, updated) }
                editingProfile = null
            }
        )
    }

    editingPlannerEntry?.let { entry ->
        AddPlannerDialog(
            profiles = profiles,
            initialEntry = entry,
            onDismiss = { editingPlannerEntry = null },
            onAdd = { updated ->
                scope.launch { BlockerPreferences.updatePlannerEntry(context, updated.copy(id = entry.id)) }
                editingPlannerEntry = null
            }
        )
    }
}

@Composable
private fun PlannerEntryItem(entry: PlannerEntry, profileName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dayNames = mapOf(1 to "Lu", 2 to "Ma", 3 to "Me", 4 to "Je", 5 to "Ve", 6 to "Sa", 7 to "Di")
    val daysText = entry.daysOfWeek.sorted().mapNotNull { dayNames[it] }.joinToString(", ")

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "%02d:%02d - %02d:%02d".format(entry.startHour, entry.startMinute, entry.endHour, entry.endMinute),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
                )
                Text("Profil : $profileName", style = MaterialTheme.typography.bodySmall)
                Text(
                    daysText, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Modifier")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

@Composable
private fun AddPlannerDialog(
    profiles: List<Profile>,
    initialEntry: PlannerEntry? = null,
    onDismiss: () -> Unit,
    onAdd: (PlannerEntry) -> Unit
) {
    var selectedProfileId by remember { mutableStateOf(initialEntry?.profileId ?: profiles.firstOrNull()?.id ?: "") }
    var startHour by remember { mutableStateOf(initialEntry?.startHour?.toString() ?: "7") }
    var startMinute by remember { mutableStateOf(initialEntry?.startMinute?.toString() ?: "0") }
    var endHour by remember { mutableStateOf(initialEntry?.endHour?.toString() ?: "18") }
    var endMinute by remember { mutableStateOf(initialEntry?.endMinute?.toString() ?: "0") }
    var selectedDays by remember { mutableStateOf(initialEntry?.daysOfWeek ?: setOf(1, 2, 3, 4, 5)) }

    val dayNames = listOf(1 to "Lu", 2 to "Ma", 3 to "Me", 4 to "Je", 5 to "Ve", 6 to "Sa", 7 to "Di")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEntry != null) "Modifier la plage" else "Nouvelle plage du planificateur") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Profil", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    profiles.forEach { profile ->
                        FilterChip(
                            selected = profile.id == selectedProfileId,
                            onClick = { selectedProfileId = profile.id },
                            label = { Text(profile.name) }
                        )
                    }
                }

                Text("Heure de début", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startHour, onValueChange = { startHour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("H") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    OutlinedTextField(
                        value = startMinute, onValueChange = { startMinute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                }

                Text("Heure de fin", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endHour, onValueChange = { endHour = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("H") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    OutlinedTextField(
                        value = endMinute, onValueChange = { endMinute = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Min") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                }

                Text("Jours", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    dayNames.forEach { (day, name) ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                            },
                            label = { Text(name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sh = startHour.toIntOrNull()?.coerceIn(0, 23) ?: 7
                    val sm = startMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    val eh = endHour.toIntOrNull()?.coerceIn(0, 23) ?: 18
                    val em = endMinute.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    onAdd(
                        PlannerEntry(
                            profileId = selectedProfileId,
                            startHour = sh, startMinute = sm,
                            endHour = eh, endMinute = em,
                            daysOfWeek = selectedDays
                        )
                    )
                },
                enabled = selectedProfileId.isNotEmpty()
            ) { Text(if (initialEntry != null) "Enregistrer" else "Ajouter") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun EditProfileDialog(profile: Profile, onDismiss: () -> Unit, onSave: (Profile) -> Unit) {
    var name by remember { mutableStateOf(profile.name) }
    var timerSlider by remember { mutableFloatStateOf(profile.timerSeconds.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le profil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Délai d'attente : ${timerSlider.roundToInt()} secondes",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = timerSlider,
                    onValueChange = { timerSlider = it },
                    valueRange = 5f..60f, steps = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            profile.copy(
                                name = name.trim(),
                                timerSeconds = timerSlider.roundToInt()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun SaveHoursGoalCard(
    weeklyTotalTimeMs: Long,
    weeklyTotalScrolls: Int,
    currentLimit: Int,
    onSetLimit: (Int) -> Unit
) {
    // Moyenne journalière réelle sur 7 jours
    val avgDailyTimeMs = weeklyTotalTimeMs / 7L
    val avgDailyScrolls = (weeklyTotalScrolls / 7).coerceAtLeast(1)

    val monthlyProjectionMs = avgDailyTimeMs * 30L
    val monthlyProjectionH = (monthlyProjectionMs / 3_600_000L).toInt().coerceAtLeast(1)
    var savingsHours by remember(monthlyProjectionH) { mutableFloatStateOf(monthlyProjectionH / 2f) }

    val targetMonthlyMs = monthlyProjectionMs - (savingsHours * 3_600_000L).toLong()
    val targetDailyMs = (targetMonthlyMs / 30L).coerceAtLeast(0L)
    val msPerScroll = if (avgDailyScrolls > 0) avgDailyTimeMs / avgDailyScrolls else 0L
    val suggestedLimit = if (msPerScroll > 0) (targetDailyMs / msPerScroll).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Objectif", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                InfoIcon("Basé sur ta moyenne des 7 derniers jours, cet outil calcule combien de scrolls par jour tu dois viser pour économiser X heures par mois.\n\nGlisse le curseur pour choisir ton objectif d'économie, puis applique la limite suggérée.")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sur les 7 derniers jours : ${weeklyTotalTimeMs / 3_600_000L}h${(weeklyTotalTimeMs % 3_600_000L / 60_000L).toString().padStart(2, '0')} au total, soit ~${monthlyProjectionH}h/mois.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Sauver ${savingsHours.toInt()}h/mois",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = savingsHours,
                onValueChange = { savingsHours = it },
                valueRange = 0f..monthlyProjectionH.toFloat(),
                steps = (monthlyProjectionH - 1).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (suggestedLimit > 0) {
                Text(
                    "Limite suggérée : $suggestedLimit scrolls/jour",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                if (currentLimit != suggestedLimit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onSetLimit(suggestedLimit) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Définir comme limite journalière")
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Limite déjà appliquée ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    "Réduis l'objectif pour obtenir une suggestion.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoIcon(text: String) {
    var showInfo by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { showInfo = true },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "?", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("Compris") } }
        )
    }
}

@Composable
private fun AddProfileDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau profil") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du profil") },
                placeholder = { Text("Ex: Boulot, Études, Weekend...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Créer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

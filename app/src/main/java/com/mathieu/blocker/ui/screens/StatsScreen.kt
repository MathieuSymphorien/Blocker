package com.mathieu.blocker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mathieu.blocker.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mathieu.blocker.data.BlockerPreferences
import com.mathieu.blocker.data.db.BlockerDatabase
import com.mathieu.blocker.data.db.UsageStatsEntity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class DayStatus { PERFECT, GOOD, EXCEEDED, NO_DATA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = remember { dateFormat.format(java.util.Date()) }

    var todayStats by remember { mutableStateOf<UsageStatsEntity?>(null) }
    var weeklyData by remember { mutableStateOf<List<UsageStatsEntity>>(emptyList()) }
    var weeklyAvg by remember { mutableStateOf<UsageStatsEntity?>(null) }
    var selectedDate by remember { mutableStateOf<String?>(today) }
    var selectedDateTotals by remember { mutableStateOf<UsageStatsEntity?>(null) }
    var selectedDateApps by remember { mutableStateOf<List<UsageStatsEntity>>(emptyList()) }
    var strictStreak by remember { mutableIntStateOf(0) }
    var limitStreak by remember { mutableIntStateOf(0) }
    var dailyLimit by remember { mutableIntStateOf(0) }
    var autoThreshold by remember { mutableIntStateOf(0) }
    var recentData by remember { mutableStateOf<List<UsageStatsEntity>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf("Semaine") }
    var perAppData by remember { mutableStateOf<List<UsageStatsEntity>>(emptyList()) }
    var weeklyTotals by remember { mutableStateOf<UsageStatsEntity?>(null) }
    var monthlyTotals by remember { mutableStateOf<UsageStatsEntity?>(null) }
    var yearlyTotals by remember { mutableStateOf<UsageStatsEntity?>(null) }
    val nowCal = remember { Calendar.getInstance() }
    var calendarDisplayYear by remember { mutableStateOf(nowCal.get(Calendar.YEAR)) }
    var calendarDisplayMonth by remember { mutableStateOf(nowCal.get(Calendar.MONTH)) }
    var calendarMonthData by remember { mutableStateOf<List<UsageStatsEntity>>(emptyList()) }

    val sevenDaysAgo = remember {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -6)
        dateFormat.format(cal.time)
    }
    val thirtyOneDaysAgo = remember {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -30)
        dateFormat.format(cal.time)
    }
    val twentyNineDaysAgo = remember {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -29)
        dateFormat.format(cal.time)
    }
    val threeHundredSixtyFourDaysAgo = remember {
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -364)
        dateFormat.format(cal.time)
    }

    val dao = remember { BlockerDatabase.getInstance(context).usageStatsDao() }

    LaunchedEffect(Unit) { dao.getDailyTotals(today).collect { todayStats = it } }
    LaunchedEffect(Unit) { dao.getDailyTotalsRange(sevenDaysAgo, today).collect { weeklyData = it } }
    LaunchedEffect(Unit) { dao.getAveragesForRange(sevenDaysAgo, today).collect { weeklyAvg = it } }
    LaunchedEffect(Unit) {
        recentData = dao.getDailyTotalsForStreak(thirtyOneDaysAgo)
        dailyLimit = BlockerPreferences.getDailyScrollLimit(context).first()
    }
    LaunchedEffect(Unit) {
        BlockerPreferences.getDailyScrollLimit(context).collect { dailyLimit = it }
    }
    LaunchedEffect(recentData, weeklyAvg, dailyLimit) {
        val threshold = if (dailyLimit > 0) dailyLimit else (weeklyAvg?.scrollCount ?: 0)
        autoThreshold = threshold
        strictStreak = computeStrictStreak(recentData, dateFormat)
        limitStreak = if (threshold > 0) computeLimitStreak(recentData, threshold, dateFormat) else 0
    }
    LaunchedEffect(selectedDate) {
        val date = selectedDate
        if (date != null) {
            selectedDateTotals = dao.getDailyTotals(date).first()
            dao.getStatsPerAppForDate(date).collect { selectedDateApps = it }
        } else {
            selectedDateTotals = null
            selectedDateApps = emptyList()
        }
    }
    LaunchedEffect(Unit) {
        weeklyTotals = dao.getTotalsForRange(sevenDaysAgo, today)
        monthlyTotals = dao.getTotalsForRange(twentyNineDaysAgo, today)
        yearlyTotals = dao.getTotalsForRange(threeHundredSixtyFourDaysAgo, today)
    }
    LaunchedEffect(selectedPeriod) {
        val (start, end) = when (selectedPeriod) {
            "Mois" -> twentyNineDaysAgo to today
            "Année" -> threeHundredSixtyFourDaysAgo to today
            else -> sevenDaysAgo to today
        }
        perAppData = dao.getPerAppTotalsForRange(start, end)
    }
    LaunchedEffect(calendarDisplayYear, calendarDisplayMonth) {
        val startCal = Calendar.getInstance().apply { set(calendarDisplayYear, calendarDisplayMonth, 1) }
        val endCal = Calendar.getInstance().apply {
            set(calendarDisplayYear, calendarDisplayMonth, 1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        calendarMonthData = dao.getDailyTotalsRange(
            dateFormat.format(startCal.time),
            dateFormat.format(endCal.time)
        ).first()
    }

    // Remplir les 7 jours (y compris les jours sans données)
    val filledWeeklyData = remember(weeklyData) {
        val map = weeklyData.associateBy { it.date }
        (6 downTo 0).map { daysBack ->
            val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysBack) }
            val dateStr = dateFormat.format(dayCal.time)
            map[dateStr] ?: UsageStatsEntity(date = dateStr, packageName = "")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 4.dp)
        ) {
            Text(
                stringResource(R.string.stats_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.stats_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ── 1. Aujourd'hui + Résistance ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Aujourd'hui", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    InfoIcon("Scrolls : Nombre de scroll sur les apps bloquées.\n\nTemps : Durée passée dans les apps bloquées.\n\nTentatives : Nombre de fois où tu as essayé d'ouvrir les apps bloquées.\n\nRetours : Nombre de fois où tu as choisi de ne pas entrer sur les apps bloquées.\n\nRésistance : Métrique qui mesure à quel point tu résistes une fois que tu as ouvert une app bloquée.\nLe calcul : Retours / Tentatives × 100.\nPlus le % est élevé, plus tu résistes à l'envie d'ouvrir l'app bloquée.")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem(
                        value = "${todayStats?.scrollCount ?: 0}",
                        label = "Scrolls"
                    )
                    StatItem(
                        value = formatDuration(todayStats?.scrollTimeMs ?: 0),
                        label = "Temps"
                    )

                    StatItem(
                        value = "${todayStats?.openCount ?: 0}",
                        label = "Tentatives"

                    )
                    StatItem(
                        value = "${todayStats?.returnCount ?: 0}",
                        label = "Retours"
                    )
                }
                val openCount = todayStats?.openCount ?: 0
                if (openCount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    val returnCount = todayStats?.returnCount ?: 0
                    val pct = (returnCount * 100 / openCount).coerceIn(0, 100)
                    val (resistLabel, resistColor) = when {
                        pct == 100 -> "Invincible !" to Color(0xFF4CAF50)
                        pct >= 75 -> "Excellent !" to Color(0xFF4CAF50)
                        pct >= 50 -> "Bonne résistance" to Color(0xFFFFC107)
                        pct >= 25 -> "Tu résistes parfois" to Color(0xFFFF9800)
                        pct > 0 -> "Tu cèdes souvent" to MaterialTheme.colorScheme.error
                        else -> "Aucune résistance" to MaterialTheme.colorScheme.error
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Résistance", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "$pct%", style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold, color = resistColor
                            )
                            Text(resistLabel, style = MaterialTheme.typography.bodySmall, color = resistColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    ResistanceBar(pct)
                }
            }
        }

        // ── 2. 7 derniers jours + détail sélectionné ────────────────────────
        if (filledWeeklyData.any { it.scrollCount > 0 || it.openCount > 0 }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "7 derniers jours", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedDate != null && selectedDate != today) {
                            Text(
                                "✕ Aujourd'hui", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { selectedDate = today })
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ClickableWeeklyBarChart(
                        data = filledWeeklyData,
                        selectedDate = selectedDate,
                        onDateSelected = { date -> selectedDate = if (selectedDate == date) today else date }
                    )
                    // Détail inline du jour sélectionné
                    val totals = selectedDateTotals
                    val showDate = selectedDate
                    if (showDate != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            formatDisplayDate(showDate, dateFormat),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(value = "${totals?.scrollCount ?: 0}", label = "Scrolls")
                            StatItem(value = formatDuration(totals?.scrollTimeMs ?: 0), label = "Temps")
                            StatItem(value = "${totals?.openCount ?: 0}", label = "Tentatives")
                            StatItem(value = "${totals?.returnCount ?: 0}", label = "Retours")
                        }
                        if (selectedDateApps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val pm = context.packageManager
                            selectedDateApps.forEach { app ->
                                val appName = try {
                                    pm.getApplicationLabel(pm.getApplicationInfo(app.packageName, 0)).toString()
                                } catch (_: Exception) {
                                    app.packageName.substringAfterLast(".")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        appName,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${app.scrollCount} scr · ${formatDuration(app.scrollTimeMs)} · ${app.returnCount} ret.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (totals == null || (totals.scrollCount == 0 && totals.openCount == 0)) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Aucune activité ce jour.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Séries + Calendrier ───────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        "Calendrier", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    StreakItem(
                        count = strictStreak,
                        emoji = if (strictStreak > 0) "🧘" else "—",
                        label = "Jours parfaits",
                        subtitle = "0 scroll & 0 ouverture",
                    )
                    StreakItem(
                        count = limitStreak,
                        emoji = if (limitStreak > 0) "🔥" else "—",
                        label = "Sous la limite",
                        subtitle = if (autoThreshold > 0) "< $autoThreshold scrolls/j"
                        else "fixe une limite",
                    )
                    InfoIcon("Jours parfaits : Jours consécutifs (jusqu'à aujourd'hui) sans aucun scroll ni tentative d'ouverture d'une app bloquée..\n\nSous la limite : Jours consécutifs où tu es resté sous ta limite de scrolls quotidienne (réglable dans Réglages).")
                }
                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))
                MonthCalendar(
                    data = calendarMonthData,
                    autoThreshold = autoThreshold,
                    today = today,
                    dateFormat = dateFormat,
                    selectedDate = selectedDate,
                    onDateClick = { date -> selectedDate = if (selectedDate == date) today else date },
                    displayYear = calendarDisplayYear,
                    displayMonth = calendarDisplayMonth,
                    isCurrentMonth = calendarDisplayYear == nowCal.get(Calendar.YEAR) &&
                            calendarDisplayMonth == nowCal.get(Calendar.MONTH),
                    onPrevMonth = {
                        val c = Calendar.getInstance().apply {
                            set(calendarDisplayYear, calendarDisplayMonth, 1)
                            add(Calendar.MONTH, -1)
                        }
                        calendarDisplayYear = c.get(Calendar.YEAR)
                        calendarDisplayMonth = c.get(Calendar.MONTH)
                    },
                    onNextMonth = {
                        val c = Calendar.getInstance().apply {
                            set(calendarDisplayYear, calendarDisplayMonth, 1)
                            add(Calendar.MONTH, 1)
                        }
                        calendarDisplayYear = c.get(Calendar.YEAR)
                        calendarDisplayMonth = c.get(Calendar.MONTH)
                    }
                )
            }
        }


        // ── 5. Tendance & Analyse ────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Tendance & Analyse", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Semaine", "Mois", "Année").forEach { label ->
                        FilterChip(
                            selected = selectedPeriod == label,
                            onClick = { selectedPeriod = label },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                val (periodTotals, periodLabel) = when (selectedPeriod) {
                    "Semaine" -> weeklyTotals to "7 derniers jours"
                    "Mois" -> monthlyTotals to "30 derniers jours"
                    else -> yearlyTotals to "365 derniers jours"
                }

                if (periodTotals != null && (periodTotals.scrollCount > 0 || periodTotals.openCount > 0)) {
                    Text(
                        periodLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem(value = "${periodTotals.scrollCount}", label = "Scrolls")
                        StatItem(value = formatDuration(periodTotals.scrollTimeMs), label = "Temps")
                        StatItem(value = "${periodTotals.openCount}", label = "Tentatives")
                        StatItem(value = "${periodTotals.returnCount}", label = "Retours")
                    }
                    val appsWithOpens = perAppData.filter { it.openCount > 0 }
                    if (appsWithOpens.isNotEmpty()) {
                        val resistRate = (appsWithOpens.sumOf { it.returnCount.toDouble() * 100 / it.openCount } / appsWithOpens.size).toInt().coerceIn(0, 100)
                        val (resistLabel, resistColor) = when {
                            resistRate >= 75 -> "Excellente résistance" to Color(0xFF4CAF50)
                            resistRate >= 50 -> "Bonne résistance" to Color(0xFFFFC107)
                            resistRate >= 25 -> "Résistance partielle" to Color(0xFFFF9800)
                            else -> "Résistance faible" to MaterialTheme.colorScheme.error
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Résistance", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "$resistRate%", style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold, color = resistColor
                                )
                                Text(resistLabel, style = MaterialTheme.typography.bodySmall, color = resistColor)
                            }
                        }
                    }
                } else {
                    Text(
                        "Pas encore assez de données pour cette période.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }


                // Détail par application
                if (perAppData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Par application",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val pm = context.packageManager
                    perAppData.forEach { app ->
                        val appName = try {
                            pm.getApplicationLabel(pm.getApplicationInfo(app.packageName, 0)).toString()
                        } catch (_: Exception) {
                            app.packageName.substringAfterLast(".")
                        }
                        val resistRate = if (app.openCount > 0) app.returnCount * 100 / app.openCount else 0
                        val resistColor = when {
                            app.openCount == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            resistRate >= 75 -> Color(0xFF4CAF50)
                            resistRate >= 50 -> Color(0xFFFFC107)
                            resistRate >= 25 -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.error
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    appName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${app.scrollCount} scr · ${formatDuration(app.scrollTimeMs)} · ${app.openCount} tent. · ${app.returnCount} ret.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (app.openCount > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$resistRate%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = resistColor
                                    )
                                    Text(
                                        "résistance",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 4. Anecdote — distance du pouce ─────────────────────────────────
        val allScrolls = weeklyTotals?.scrollCount ?: 0
        if (allScrolls > 0) {
            ScrollDistanceCard(scrollCount = allScrolls, label = "cette semaine")
        } else if ((todayStats?.scrollCount ?: 0) > 0) {
            ScrollDistanceCard(scrollCount = todayStats!!.scrollCount, label = "aujourd'hui")
        }
    }
}

// ── Scroll distance fun fact ──────────────────────────────────────────────────

@Composable
private fun ScrollDistanceCard(scrollCount: Int, label: String) {
    val cm = scrollCount * 3
    val m = cm / 100.0
    val displayDist = when {
        m >= 1000 -> "${"%.1f".format(m / 1000)}km"
        m >= 1 -> "${"%.0f".format(m)}m"
        else -> "${cm}cm"
    }
    val (comparison, emoji) = when {
        m < 1 -> "à peine plus que la longueur de ta main" to "👋"
        m < 4 -> "la longueur d'une voiture" to "🚗"
        m < 10 -> "une ruelle de ${m.toInt()}m" to "🏠"
        m < 30 -> "la hauteur d'un immeuble de ${(m / 3).toInt()} étages" to "🏢"
        m < 96 -> "${m.toInt()}m — tu gravis Notre-Dame de Paris (96m)" to "⛪"
        m < 330 -> "${m.toInt()}m — la moitié de la Tour Eiffel !" to "🗼"
        m < 660 -> "la hauteur de la Tour Eiffel (330m) !" to "🗼"
        m < 1000 -> "${"%.0f".format(m / 330)}× la hauteur de la Tour Eiffel..." to "🗼"
        m < 4810 -> "${"%.1f".format(m / 1000)}km — presque le sommet du Mont Blanc !" to "⛰️"
        else -> "${"%.0f".format(m / 1000)}km — tu as grimpé plusieurs Everest 🫠" to "🏔️"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Le savais-tu ?", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineMedium)
                Column {
                    Text(
                        "Ton pouce a parcouru $displayDist $label",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "$comparison ($scrollCount scrolls × 3cm)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResistanceBar(percentage: Int) {
    val barColor = when {
        percentage >= 75 -> Color(0xFF4CAF50)
        percentage >= 50 -> Color(0xFFFFC107)
        percentage >= 25 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.error
    }
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
            val w = size.width
            val h = size.height
            val r = h / 2f
            drawRoundRect(color = surfaceVariant, size = Size(w, h), cornerRadius = CornerRadius(r))
            if (percentage > 0) {
                drawRoundRect(
                    color = barColor,
                    size = Size((w * percentage / 100f).coerceAtMost(w), h),
                    cornerRadius = CornerRadius(r)
                )
            }
            listOf(0.25f, 0.50f, 0.75f).forEach { frac ->
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(w * frac, 0f), end = Offset(w * frac, h),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "25%", "50%", "75%", "100%").forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
            }
        }
    }
}


// ── Calendar ─────────────────────────────────────────────────────────────────

@Composable
private fun MonthCalendar(
    data: List<UsageStatsEntity>,
    autoThreshold: Int,
    today: String,
    dateFormat: SimpleDateFormat,
    selectedDate: String?,
    onDateClick: (String) -> Unit,
    displayYear: Int,
    displayMonth: Int,
    isCurrentMonth: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val dataMap = data.associateBy { it.date }

    val monthStartCal = Calendar.getInstance().apply {
        set(displayYear, displayMonth, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val firstIsoDow = monthStartCal.get(Calendar.DAY_OF_WEEK).let {
        if (it == Calendar.SUNDAY) 7 else it - 1
    }
    val daysInMonth = monthStartCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells: List<String?> = List(firstIsoDow - 1) { null } +
            (1..daysInMonth).map { day ->
                Calendar.getInstance().apply { set(displayYear, displayMonth, day) }
                    .let { dateFormat.format(it.time) }
            }
    val rows = cells.chunked(7)

    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
        .format(monthStartCal.time).replaceFirstChar { it.uppercase() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "←", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onPrevMonth() }.padding(horizontal = 8.dp)
            )
            Text(
                monthLabel, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isCurrentMonth) {
                Text(
                    "→", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNextMonth() }.padding(horizontal = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(32.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("L", "Ma", "Me", "J", "V", "S", "D").forEach { label ->
                Text(
                    label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val data = dataMap[date]
                        val isToday = date == today
                        val isFuture = date > today
                        val isSelected = date == selectedDate
                        val dayNum = date.substringAfterLast("-").trimStart('0').ifEmpty { "0" }
                        val status = if (isFuture) DayStatus.NO_DATA else getDayStatus(data, autoThreshold)
                        DayCell(
                            dayNum = dayNum, status = status, isToday = isToday,
                            isFuture = isFuture, isSelected = isSelected,
                            onClick = { if (!isFuture) onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendDot(Color(0xFF4CAF50), "Parfait")
            LegendDot(MaterialTheme.colorScheme.primary, "Sous moy.")
            LegendDot(MaterialTheme.colorScheme.error, "Dépassé")
            LegendDot(MaterialTheme.colorScheme.surfaceVariant, "Aucune donnée")
        }
    }
}

@Composable
private fun DayCell(
    dayNum: String, status: DayStatus, isToday: Boolean, isFuture: Boolean,
    isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val bgColor = when {
        isFuture -> Color.Transparent
        status == DayStatus.PERFECT -> Color(0xFF4CAF50)
        status == DayStatus.GOOD -> MaterialTheme.colorScheme.primary
        status == DayStatus.EXCEEDED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        status == DayStatus.NO_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.White
    }
    Box(modifier = modifier.padding(2.dp).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.fillMaxSize().clip(CircleShape).background(bgColor)
                .then(
                    if (isToday) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.onSurface,
                        CircleShape
                    ) else Modifier
                )
                .then(
                    if (isSelected) Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.tertiary,
                        CircleShape
                    ) else Modifier
                )
                .then(if (!isFuture) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayNum, style = MaterialTheme.typography.labelSmall, color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun getDayStatus(data: UsageStatsEntity?, threshold: Int): DayStatus {
    if (data == null || (data.scrollCount == 0 && data.openCount == 0)) return DayStatus.PERFECT
    if (threshold > 0 && data.scrollCount < threshold) return DayStatus.GOOD
    if (threshold > 0 && data.scrollCount >= threshold) return DayStatus.EXCEEDED
    return DayStatus.GOOD
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

@Composable
private fun ClickableWeeklyBarChart(
    data: List<UsageStatsEntity>, selectedDate: String?,
    onDateSelected: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val maxScrolls = (data.maxOfOrNull { it.scrollCount } ?: 1).coerceAtLeast(1)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        data.forEach { stats ->
            val isSelected = stats.date == selectedDate
            val fraction = stats.scrollCount.toFloat() / maxScrolls
            Column(
                modifier = Modifier.weight(1f).clickable { onDateSelected(stats.date) }.padding(horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp)).background(surfaceVariant)
                    )
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier.fillMaxWidth(0.7f)
                                .fillMaxHeight(fraction.coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) primary else primaryContainer)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    getDayLabel(stats.date), style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${stats.scrollCount}", style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Streak helpers ────────────────────────────────────────────────────────────

@Composable
private fun StreakItem(count: Int, emoji: String, label: String, subtitle: String, info: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (count > 0) "$count $emoji" else emoji, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            if (info.isNotEmpty()) InfoIcon(info)
        }
        Text(
            subtitle, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
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
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun computeStrictStreak(recentDays: List<UsageStatsEntity>, dateFormat: SimpleDateFormat): Int {
    val dirtyDates = recentDays.filter { it.scrollCount > 0 || it.openCount > 0 }.map { it.date }.toSet()
    return countStreak(dirtyDates, dateFormat)
}

private fun computeLimitStreak(recentDays: List<UsageStatsEntity>, threshold: Int, dateFormat: SimpleDateFormat): Int {
    val exceededDates = recentDays.filter { it.scrollCount >= threshold }.map { it.date }.toSet()
    return countStreak(exceededDates, dateFormat)
}

private fun countStreak(badDates: Set<String>, dateFormat: SimpleDateFormat): Int {
    var streak = 0
    val cal = Calendar.getInstance()
    repeat(31) {
        val date = dateFormat.format(cal.time)
        if (date !in badDates) {
            streak++; cal.add(Calendar.DAY_OF_YEAR, -1)
        } else return streak
    }
    return streak
}

// ── Utils ─────────────────────────────────────────────────────────────────────

private fun getDayLabel(date: String): String {
    return try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
        val cal = Calendar.getInstance().apply { time = d!! }
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "L"; Calendar.TUESDAY -> "Ma"; Calendar.WEDNESDAY -> "Me"
            Calendar.THURSDAY -> "J"; Calendar.FRIDAY -> "V"; Calendar.SATURDAY -> "S"
            Calendar.SUNDAY -> "D"; else -> "?"
        }
    } catch (_: Exception) {
        "?"
    }
}

private fun formatDisplayDate(date: String, fmt: SimpleDateFormat): String {
    return try {
        val d = fmt.parse(date)
        val cal = Calendar.getInstance().apply { time = d!! }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val months = listOf("jan", "fév", "mar", "avr", "mai", "juin", "juil", "aoû", "sep", "oct", "nov", "déc")
        "$day ${months[cal.get(Calendar.MONTH)]}"
    } catch (_: Exception) {
        date
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        days >= 1 -> "${days}j ${hours}h"
        hours > 0 -> "${hours}h${minutes.toString().padStart(2, '0')}"
        minutes > 0 -> "${minutes}min"
        totalSeconds > 0 -> "${totalSeconds}s"
        else -> "—"
    }
}

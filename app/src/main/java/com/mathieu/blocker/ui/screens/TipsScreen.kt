package com.mathieu.blocker.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mathieu.blocker.R

private data class Tip(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val category: String
)

private data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@Composable
fun TipsScreen() {
    val tips = listOf(
        Tip(
            Icons.Default.Notifications,
            stringResource(R.string.tip_1_title),
            stringResource(R.string.tip_1_desc),
            stringResource(R.string.tip_1_category)
        ),
        Tip(
            Icons.Default.AccessTime,
            stringResource(R.string.tip_2_title),
            stringResource(R.string.tip_2_desc),
            stringResource(R.string.tip_2_category)
        ),
        Tip(
            Icons.Default.PhoneAndroid,
            stringResource(R.string.tip_3_title),
            stringResource(R.string.tip_3_desc),
            stringResource(R.string.tip_3_category)
        ),
        Tip(
            Icons.Default.WbSunny,
            stringResource(R.string.tip_4_title),
            stringResource(R.string.tip_4_desc),
            stringResource(R.string.tip_4_category)
        ),
        Tip(
            Icons.Default.SelfImprovement,
            stringResource(R.string.tip_5_title),
            stringResource(R.string.tip_5_desc),
            stringResource(R.string.tip_5_category)
        ),
        Tip(
            Icons.Default.Psychology,
            stringResource(R.string.tip_6_title),
            stringResource(R.string.tip_6_desc),
            stringResource(R.string.tip_6_category)
        ),
        Tip(
            Icons.Default.Visibility,
            stringResource(R.string.tip_7_title),
            stringResource(R.string.tip_7_desc),
            stringResource(R.string.tip_7_category)
        ),
        Tip(
            Icons.Default.BatteryChargingFull,
            stringResource(R.string.tip_8_title),
            stringResource(R.string.tip_8_desc),
            stringResource(R.string.tip_8_category)
        ),
        Tip(
            Icons.Default.FitnessCenter,
            stringResource(R.string.tip_9_title),
            stringResource(R.string.tip_9_desc),
            stringResource(R.string.tip_9_category)
        ),
        Tip(
            Icons.Default.Lightbulb,
            stringResource(R.string.tip_10_title),
            stringResource(R.string.tip_10_desc),
            stringResource(R.string.tip_10_category)
        ),
    )

    val features = listOf(
        Feature(
            Icons.Default.Person,
            stringResource(R.string.guide_feat_profiles_title),
            stringResource(R.string.guide_feat_profiles_desc)
        ),
        Feature(
            Icons.Default.Timer,
            stringResource(R.string.guide_feat_timer_title),
            stringResource(R.string.guide_feat_timer_desc)
        ),
        Feature(
            Icons.Default.Schedule,
            stringResource(R.string.guide_feat_schedule_title),
            stringResource(R.string.guide_feat_schedule_desc)
        ),
        Feature(
            Icons.Default.ShowChart,
            stringResource(R.string.guide_feat_stats_title),
            stringResource(R.string.guide_feat_stats_desc)
        ),
        Feature(
            Icons.Default.AccessTime,
            stringResource(R.string.guide_feat_limit_title),
            stringResource(R.string.guide_feat_limit_desc)
        ),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 4.dp)) {
                Text(
                    text = stringResource(R.string.guide_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.guide_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // ── À propos ──────────────────────────────────────────────────────────
        item {
            SectionHeader(
                title = stringResource(R.string.guide_about_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
        item {
            AboutCard(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // ── Fonctionnalités ───────────────────────────────────────────────────
        item {
            SectionHeader(
                title = stringResource(R.string.guide_features_title),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp)
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    features.forEachIndexed { index, feature ->
                        FeatureRow(feature = feature)
                        if (index < features.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // ── Conseils ──────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp)) {
                SectionHeader(title = stringResource(R.string.guide_tips_section_title))
                Text(
                    text = stringResource(R.string.guide_tips_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        itemsIndexed(tips) { index, tip ->
            TipCard(tip = tip, modifier = Modifier.padding(horizontal = 16.dp))
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ── About composables ─────────────────────────────────────────────────────────

private data class AboutItem(
    val icon: ImageVector,
    val title: String,
    val body: String
)

@Composable
private fun AboutCard(modifier: Modifier = Modifier) {
    val items = listOf(
        AboutItem(Icons.Default.Favorite,    stringResource(R.string.guide_about_project_title),     stringResource(R.string.guide_about_project_body)),
        AboutItem(Icons.Default.Lock,        stringResource(R.string.guide_about_privacy_title),     stringResource(R.string.guide_about_privacy_body)),
        AboutItem(Icons.Default.Settings,    stringResource(R.string.guide_about_permissions_title), stringResource(R.string.guide_about_permissions_body)),
        AboutItem(Icons.Default.Lightbulb,   stringResource(R.string.guide_about_feedback_title),    stringResource(R.string.guide_about_feedback_body)),
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            items.forEachIndexed { index, item ->
                AboutRow(item = item)
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutRow(item: AboutItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

@Composable
private fun FeatureRow(feature: Feature) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipCard(tip: Tip, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tip.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tip.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

package com.mathieu.blocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BlockerDarkColors = darkColorScheme(
    // ── Primary ──────────────────────────────────────────────────────────────
    primary              = Indigo70,
    onPrimary            = Neutral5,
    primaryContainer     = Indigo20,
    onPrimaryContainer   = Indigo90,
    inversePrimary       = Indigo40,

    // ── Secondary (teal — succès/progrès) ────────────────────────────────────
    secondary            = Teal80,
    onSecondary          = Teal20,
    secondaryContainer   = Teal30,
    onSecondaryContainer = Teal90,

    // ── Tertiary (ambre — modéré/neutre) ─────────────────────────────────────
    tertiary             = Amber80,
    onTertiary           = Amber20,
    tertiaryContainer    = Amber30,
    onTertiaryContainer  = Amber90,

    // ── Error ─────────────────────────────────────────────────────────────────
    error                = Red80,
    onError              = Red20,
    errorContainer       = Red30,
    onErrorContainer     = Red90,

    // ── Fond ─────────────────────────────────────────────────────────────────
    background           = Neutral5,
    onBackground         = White95,

    // ── Surfaces ─────────────────────────────────────────────────────────────
    surface              = Neutral15,
    onSurface            = White85,
    surfaceVariant       = Neutral20,
    onSurfaceVariant     = White60,
    surfaceTint          = Indigo60,

    // ── Inverses ─────────────────────────────────────────────────────────────
    inverseSurface       = NeutralV90,
    inverseOnSurface     = Neutral15,

    // ── Contours ─────────────────────────────────────────────────────────────
    outline              = Neutral40,
    outlineVariant       = NeutralV30,

    // ── Divers ───────────────────────────────────────────────────────────────
    scrim                = Neutral5,
)

@Composable
fun BlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlockerDarkColors,
        typography  = Typography,
        content     = content
    )
}

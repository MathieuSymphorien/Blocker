package com.mathieu.blocker.ui.overlay

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CountdownOverlayScreen(
    appName: String,
    totalSeconds: Int,
    challengeEnabled: Boolean = false,
    challengeString: String = "",
    limitReached: Boolean = false,
    onFinished: () -> Unit,
    onGoBack: () -> Unit,
    onNeedKeyboard: () -> Unit = {}
) {
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var progress by remember { mutableFloatStateOf(1f) }
    var timerDone by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "countdown_progress"
    )

    LaunchedEffect(Unit) {
        if (limitReached) return@LaunchedEffect
        for (i in totalSeconds downTo 1) {
            remainingSeconds = i
            progress = i.toFloat() / totalSeconds
            delay(1000L)
        }
        remainingSeconds = 0
        progress = 0f
        delay(300L)
        timerDone = true
        if (challengeEnabled) {
            onNeedKeyboard()
        }
    }

    // ── Couleurs hardcodées (overlay système, pas de MaterialTheme disponible) ──
    val bgColor       = Color(0xFF0B0C15)   // Neutral5
    val surfaceColor  = Color(0xFF161727)   // Neutral15
    val primaryColor  = Color(0xFF9597F0)   // Indigo70
    val primaryDim    = Color(0xFF2E30B0)   // Indigo30 (container)
    val errorColor    = Color(0xFFFF897D)   // Red80
    val textPrimary   = Color(0xFFEFF0FF)   // White95
    val textSecondary = Color(0xFFD8DAF4)   // White85
    val textMuted     = Color(0xFF9FA2C4)   // White60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 36.dp)
        ) {
            if (limitReached) {
                // ── Badge limite ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .background(Color(0xFF3D1A1A), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Limite quotidienne", color = errorColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "C'est tout pour aujourd'hui",
                    color = textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tu as atteint ta limite de scrolls.",
                    color = textSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Reviens demain. Ton cerveau te remerciera.",
                    color = textMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(36.dp))
                Button(
                    onClick = onGoBack,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("J'ai compris", color = bgColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                return@Column
            }

            // ── Timer ─────────────────────────────────────────────────────────
            if (!timerDone) {
                Text(
                    text = "Attends un instant",
                    color = textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tu veux vraiment ouvrir",
                    color = textSecondary,
                    fontSize = 15.sp
                )
                Text(
                    text = appName,
                    color = primaryColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(28.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                        drawArc(
                            color = Color(0xFF1C1D30), startAngle = 0f, sweepAngle = 360f,
                            useCenter = false, topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor, startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false, topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$remainingSeconds",
                        color = textPrimary, fontSize = 52.sp, fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Respire. Est-ce que tu en as vraiment besoin ?",
                    color = textMuted, fontSize = 13.sp, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                // ── Timer terminé ─────────────────────────────────────────────
                Text(
                    text = "Tu peux y aller",
                    color = textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (challengeEnabled) "Tape le texte ci-dessous pour continuer"
                           else "Ou alors, reconsidère…",
                    color = textSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (challengeEnabled && challengeString.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = challengeString,
                            color = primaryColor, fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it; inputError = false },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tape le texte ci-dessus…", color = textMuted) },
                        isError = inputError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor   = textPrimary, unfocusedTextColor = textPrimary,
                            focusedBorderColor = primaryColor, unfocusedBorderColor = Color(0xFF2E30B0)
                        ),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )

                    if (inputError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Le texte ne correspond pas", color = errorColor, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (userInput == challengeString) onFinished()
                            else { inputError = true; userInput = "" }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Ouvrir $appName", color = bgColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Ouvrir $appName", color = bgColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Bouton retour ─────────────────────────────────────────────────
            Button(
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = surfaceColor,
                    contentColor   = textSecondary
                )
            ) {
                Text("Non, je reviens", fontSize = 15.sp)
            }
        }
    }
}

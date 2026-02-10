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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CountdownOverlayScreen(
    appName: String,
    totalSeconds: Int,
    onFinished: () -> Unit,
    onGoBack: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var progress by remember { mutableFloatStateOf(1f) }
    var timerDone by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "countdown_progress"
    )

    LaunchedEffect(Unit) {
        for (i in totalSeconds downTo 1) {
            remainingSeconds = i
            progress = i.toFloat() / totalSeconds
            delay(1000L)
        }
        remainingSeconds = 0
        progress = 0f
        delay(300L)
        timerDone = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1035)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (timerDone) "Tu peux y aller" else "Attends un peu...",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (timerDone) "Ou alors..." else "Tu veux vraiment ouvrir",
                color = Color(0xFFDDDDFF),
                fontSize = 16.sp
            )

            if (!timerDone) {
                Text(
                    text = appName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (!timerDone) {
                // Circular countdown
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )

                        drawArc(
                            color = Color(0xFF333344),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        drawArc(
                            color = Color(0xFF6666FF),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = "$remainingSeconds",
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "Respire. Est-ce que tu en as vraiment besoin ?",
                    color = Color(0xFFDDDDFF),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (timerDone) {
                // Timer finished: show the access button
                Button(
                    onClick = onFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = "Ouvrir $appName",
                        color = Color(0xFF0F1035),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onGoBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6666DD)
                )
            ) {
                Text(
                    text = "Non, je reviens",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

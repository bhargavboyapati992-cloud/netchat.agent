package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var loadingStatusText by remember { mutableStateOf("Initializing Computer Networks Engine...") }
    var progress by remember { mutableFloatStateOf(0.12f) }

    // Flower Logo Continuous Rotation & Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_flower_rotation")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flower_spin"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flower_pulse"
    )

    LaunchedEffect(Unit) {
        delay(600)
        loadingStatusText = "Loading Computer Networks Syllabus & Protocols..."
        progress = 0.40f

        delay(700)
        loadingStatusText = "Setting up AI Assistant & Wireshark Analyzer..."
        progress = 0.75f

        delay(700)
        loadingStatusText = "Ready! Opening NetChat..."
        progress = 1.0f

        delay(400)
        onSplashFinished()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                // Custom Canvas Flower Rotating Logo Component
                RotatingFlowerLogoView(
                    rotationAngle = rotationAngle,
                    pulseScale = pulseScale,
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // App Title
                Text(
                    text = "NetChat",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Computer Networks AI Virtual Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Loading Progress Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = loadingStatusText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Skip button to enter immediately if desired
                TextButton(
                    onClick = onSplashFinished,
                    modifier = Modifier.testTag("skip_splash_button")
                ) {
                    Text(
                        text = "Skip Loading →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun RotatingFlowerLogoView(
    rotationAngle: Float,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Rotating Flower Petals Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
        ) {
            val centerPt = this.center
            val petalRadius = size.minDimension / 2.2f
            val petalWidth = petalRadius * 0.48f
            val petalHeight = petalRadius * 0.88f

            // Rotate the entire flower petal layer
            rotate(degrees = rotationAngle, pivot = centerPt) {
                for (i in 0 until 8) {
                    rotate(degrees = i * 45f, pivot = centerPt) {
                        val petalPath = Path().apply {
                            moveTo(centerPt.x, centerPt.y - 10f)
                            cubicTo(
                                centerPt.x - petalWidth, centerPt.y - (petalHeight * 0.45f),
                                centerPt.x - (petalWidth * 0.85f), centerPt.y - petalHeight,
                                centerPt.x, centerPt.y - petalHeight
                            )
                            cubicTo(
                                centerPt.x + (petalWidth * 0.85f), centerPt.y - petalHeight,
                                centerPt.x + petalWidth, centerPt.y - (petalHeight * 0.45f),
                                centerPt.x, centerPt.y - 10f
                            )
                            close()
                        }

                        val petalColor = when (i % 3) {
                            0 -> primaryColor
                            1 -> secondaryColor
                            else -> tertiaryColor
                        }

                        drawPath(
                            path = petalPath,
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    petalColor.copy(alpha = 0.9f),
                                    petalColor.copy(alpha = 0.35f)
                                ),
                                center = centerPt,
                                radius = petalRadius
                            )
                        )
                    }
                }
            }
        }

        // Inner glowing core container
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            containerColor,
                            primaryColor.copy(alpha = 0.25f)
                        )
                    )
                )
                .border(2.5.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Rotating Flower App Logo Image inside
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = "Rotating Flower App Logo",
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        rotationZ = rotationAngle
                    }
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

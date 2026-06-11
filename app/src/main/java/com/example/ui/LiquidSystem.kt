package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Modern iOS-style viscoelastic liquid-jelly bouncy press action.
 * Instead of simple scaling, it squishes the x-axis and y-axis in opposing phases on touch,
 * and wiggles/vibrates organically on release mimicking high-viscosity fluid surface tension.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.liquidJellyPress(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Out-of-phase viscoelastic scale parameters
    val scaleXTarget = if (isPressed && enabled) 1.15f else 1.0f
    val scaleYTarget = if (isPressed && enabled) 0.85f else 1.0f

    // Highly responsive, slightly underdamped springs to recreate organic jelly fleshiness
    val scaleX by animateFloatAsState(
        targetValue = scaleXTarget,
        animationSpec = spring(
            dampingRatio = 0.45f, // low damping ratio for wiggly liquid jell-o bounce
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_jelly_x"
    )

    val scaleY by animateFloatAsState(
        targetValue = scaleYTarget,
        animationSpec = spring(
            dampingRatio = 0.40f, // different damping ratio for out-of-phase oscillation
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_jelly_y"
    )

    // Touch release wiggle angle rotation to simulate liquid slide displacement
    val rotationTarget = if (isPressed && enabled) -4f else 0f
    val rotationZ by animateFloatAsState(
        targetValue = rotationTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "liquid_wobble_rot"
    )

    return this
        .graphicsLayer {
            this.scaleX = scaleX
            this.scaleY = scaleY
            this.rotationZ = rotationZ
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null, // Disable industrial gray highlight
            onLongClick = onLongClick,
            onClick = onClick
        )
}

/**
 * Interactive iOS Liquid Island status / player hub.
 * A gorgeous floaty black glass capsule that sits at the top of the workspace.
 * It features dynamic expands, active fluid waveforms, squishy bubble particle spray on touch,
 * and custom-controlled live bento notification feeds.
 */
@Composable
fun LiquidIsland(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val batteryPct by viewModel.batteryPct.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val weatherTemp by viewModel.currentTemp.collectAsState()
    val weatherCondition by viewModel.weatherCondition.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    var activePage by remember { mutableIntStateOf(0) } // 0: Live Beats Tracker, 1: Power & Temp Nodes
    var musicPlaying by remember { mutableStateOf(true) }

    // Multi-staged dimensions for visceral bubble morphing
    val islandWidthTarget = if (isExpanded) 340.dp else 180.dp
    val islandHeightTarget = if (isExpanded) 110.dp else 36.dp
    val cornerRadiusTarget = if (isExpanded) 28.dp else 18.dp

    val animatedWidth by animateDpAsState(
        targetValue = islandWidthTarget,
        animationSpec = spring(
            dampingRatio = 0.58f, // custom organic bounce curve
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_island_width"
    )

    val animatedHeight by animateDpAsState(
        targetValue = islandHeightTarget,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_island_height"
    )

    val animatedRadius by animateDpAsState(
        targetValue = cornerRadiusTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_island_radius"
    )

    // Animated content transition alphas
    val contentAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 200, delayMillis = if (isExpanded) 100 else 0),
        label = "liquid_content_alpha"
    )

    val miniAlpha by animateFloatAsState(
        targetValue = if (!isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "liquid_mini_alpha"
    )

    // Dynamic wave simulation ticker
    var sinePhase by remember { mutableStateOf(0f) }
    LaunchedEffect(musicPlaying) {
        if (musicPlaying) {
            while (true) {
                sinePhase += 0.15f
                delay(30)
            }
        }
    }

    // Interactive Bubble/Droplet Particle Generator state
    val bubbleList = remember { mutableStateListOf<LiquidDroplet>() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .padding(top = 10.dp)
            .width(animatedWidth)
            .height(animatedHeight)
            .clip(RoundedCornerShape(animatedRadius))
            .background(Color(0xFF0C0E10).copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(animatedRadius)
            )
            .clickable {
                isExpanded = !isExpanded
                // Launch droplet spray when interacting
                coroutineScope.launch {
                    repeat(6) { index ->
                        bubbleList.add(
                            LiquidDroplet(
                                startX = if (isExpanded) 150f + (index * 40f) else 100f + (index * 20f),
                                startY = 30f,
                                size = (8..18).random().toFloat(),
                                speedY = -(3..6).random().toFloat(),
                                driftX = (-3..3).random().toFloat()
                            )
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {

        // Particle Spray Canvas underneath content
        Canvas(modifier = Modifier.fillMaxSize()) {
            val iterator = bubbleList.iterator()
            while (iterator.hasNext()) {
                val droplet = iterator.next()
                droplet.update()
                if (droplet.alpha <= 0.05f) {
                    iterator.remove()
                } else {
                    drawCircle(
                        color = Color(0xFF90CAF9).copy(alpha = droplet.alpha),
                        radius = droplet.size,
                        center = androidx.compose.ui.geometry.Offset(droplet.x, droplet.y)
                    )
                }
            }
        }

        // --- MINI STATE (Collapsed pill) ---
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .graphicsLayer { alpha = miniAlpha },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mini Dynamic Indicator (Heartbeat pulse scale)
                val pulseScale by rememberInfiniteTransition(label = "").animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "jelly_pulsating"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63)) // Dynamic Pink Heartbeat
                    )
                    Text(
                        text = "LIVE",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Interactive Audio waves drawing in real-time inside mini island
                Canvas(
                    modifier = Modifier
                        .width(42.dp)
                        .height(14.dp)
                ) {
                    val barWidth = 3.dp.toPx()
                    val barSpacing = 2.dp.toPx()
                    val count = 5
                    for (i in 0 until count) {
                        val factor = sin(sinePhase + (i * 1.1f))
                        val heightMultiplier = if (musicPlaying) (factor * 0.4f + 0.6f) else 0.2f
                        val barHeight = size.height * heightMultiplier
                        val x = i * (barWidth + barSpacing) + 4.dp.toPx()
                        val y = (size.height - barHeight) / 2f
                        drawRoundRect(
                            color = Color(0xFF90CAF9),
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx())
                        )
                    }
                }

                // Mini Battery Bubble
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$batteryPct%",
                        color = Color(0xFF81C784),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        // --- EXPANDED MULTI-TIER STATE ---
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .graphicsLayer { alpha = contentAlpha },
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE INTERACTION",
                            fontSize = 10.sp,
                            color = Color(0xFF90CAF9),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.clickable { activePage = 0 }
                        )

                        Text(
                            text = "SYS NODES",
                            fontSize = 10.sp,
                            color = if (activePage == 1) Color.White else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.clickable { activePage = 1 }
                        )
                    }

                    // Compact dismiss trigger
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Collapse",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { isExpanded = false }
                    )
                }

                if (activePage == 0) {
                    // Page 1: Liquid Sound Waves & Play controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "iOS Liquid Synthesis",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Visco-elastic Waveforms Active",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play/Pause Bubble trigger
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { musicPlaying = !musicPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (musicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Fluid Wave visualizer ribbon
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    ) {
                        val baseHeight = size.height / 2f
                        val wavePoints = 50
                        val stepX = size.width / wavePoints
                        val path = androidx.compose.ui.graphics.Path()
                        path.moveTo(0f, baseHeight)

                        for (i in 0..wavePoints) {
                            val x = i * stepX
                            val freq = if (musicPlaying) 0.08f else 0.02f
                            val factor = sin((x * freq) + sinePhase)
                            val amplitude = if (musicPlaying) 12.dp.toPx() else 2.dp.toPx()
                            val y = baseHeight + (factor * amplitude)
                            path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFFE91E63),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                } else {
                    // Page 2: System Fluid Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Temp Capsule Node
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Thermostat,
                                contentDescription = null,
                                tint = Color(0xFFFFA726),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("WEATHER", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                Text("$weatherTemp°C $weatherCondition", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Charging status Capsule Node
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFF66BB6A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("POWER", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                Text(if (isCharging) "Charging" else "On Battery ($batteryPct%)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Represent state for a single liquid droplet floating when Island is tapped.
 */
private class LiquidDroplet(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val speedY: Float,
    val driftX: Float
) {
    var x = startX
    var y = startY
    var alpha = 1.0f

    fun update() {
        x += driftX
        y += speedY
        alpha = (alpha - 0.038f).coerceAtLeast(0.0f)
    }
}

package com.example.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

sealed class WallpaperTheme(val id: String, val name: String) {
    object CosmicAurora : WallpaperTheme("cosmic_aurora", "Cosmic Aurora")
    object OriginForest : WallpaperTheme("origin_forest", "Origin Forest")
    object MutedSlate : WallpaperTheme("muted_slate", "Muted Slate")

    companion object {
        fun fromId(id: String): WallpaperTheme {
            return when (id) {
                "cosmic_aurora" -> CosmicAurora
                "origin_forest" -> OriginForest
                "muted_slate" -> MutedSlate
                else -> CosmicAurora
            }
        }
    }
}

@Composable
fun ParallaxWallpaper(
    wallpaperId: String,
    dragOffset: Offset,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val theme = WallpaperTheme.fromId(wallpaperId)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val brush = when (theme) {
                    WallpaperTheme.CosmicAurora -> {
                        // Premium Bento Grid linear-to-tr gradient matching the spec exactly
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1A3C40), // Dark Cyan/Teal (#1A3C40)
                                Color(0xFF1A1C1E), // Charcoal Dark (#1A1C1E)
                                Color(0xFF4A1A40)  // Plum Violet (#4A1A40)
                            ),
                            start = Offset(0f + (dragOffset.x * 0.1f), size.height + (dragOffset.y * 0.1f)),
                            end = Offset(size.width + (dragOffset.x * 0.1f), 0f + (dragOffset.y * 0.1f))
                        )
                    }
                    WallpaperTheme.OriginForest -> {
                        // Deep nature emerald gradients
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0D3227), // Lush emerald core
                                Color(0xFF061B15), // Leafy background shadow
                                Color(0xFF020907)  // Deep forest dark
                            ),
                            center = Offset(
                                size.width / 3f + (dragOffset.x * 0.12f),
                                size.height * 0.6f + (dragOffset.y * 0.12f)
                            ),
                            radius = size.width * 1.5f
                        )
                    }
                    WallpaperTheme.MutedSlate -> {
                        // Tech titanium industrial slate
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF262930),
                                Color(0xFF131518)
                            ),
                            start = Offset(dragOffset.x * 0.08f, dragOffset.y * 0.08f),
                            end = Offset(size.width + dragOffset.x * 0.08f, size.height + dragOffset.y * 0.08f)
                        )
                    }
                }
                drawRect(brush = brush)
            }
    ) {
        // Multi-layered visual depth elements simulating parallax background particles
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = dragOffset.x * 0.05f
                    translationY = dragOffset.y * 0.05f
                }
                .drawBehind {
                    // Draw secondary glowing nodes to enhance simulated 3D parallax layers
                    when (theme) {
                        WallpaperTheme.CosmicAurora -> {
                            // Magenta spark glow node
                            drawCircle(
                                color = Color(0x33FF007F),
                                radius = size.width * 0.35f,
                                center = Offset(size.width * 0.8f, size.height * 0.2f)
                            )
                            // Cyan secondary nebula node
                            drawCircle(
                                color = Color(0x1E00E5FF),
                                radius = size.width * 0.45f,
                                center = Offset(size.width * 0.15f, size.height * 0.75f)
                            )
                        }
                        WallpaperTheme.OriginForest -> {
                            // Warm sun rays gold glow
                            drawCircle(
                                color = Color(0x28FFD700),
                                radius = size.width * 0.3f,
                                center = Offset(size.width * 0.7f, size.height * 0.1f)
                            )
                        }
                        else -> {}
                    }
                }
        )

        content()
    }
}

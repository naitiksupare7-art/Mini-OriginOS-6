package com.example.ui

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.LauncherViewModel

@Composable
fun ControlCenter(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Quick tiles states
    var wifiEnabled by remember { mutableStateOf(true) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var flashlightEnabled by remember { mutableStateOf(false) }

    // Volume Manager
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxMusicVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }
    var currentVolume by remember {
        mutableStateOf(
            audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        )
    }

    // Brightness Manager (Local State mockup since writing Settings.System needs WRITE_SETTINGS package permissions)
    var brightnessLevel by remember { mutableStateOf(0.6f) }

    // Safe physical flashlight activation via CameraManager
    LaunchedEffect(flashlightEnabled) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(cameraId, flashlightEnabled)
            }
        } catch (e: Exception) {
            Log.e("ControlCenter", "Torch could not be initialized or toggled", e)
        }
    }

    // Swipe down overlay container
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { viewModel.controlCenterExpanded.value = false },
        contentAlignment = Alignment.TopCenter
    ) {
        // Frosted Control Panel block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                .background(Color(0xE01C1B1F)) // High-opacity dark tone for glassmorphism
                .clickable(enabled = false) {} // block click through
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Indicator line
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Control Center",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    TextButton(
                        onClick = { viewModel.controlCenterExpanded.value = false }
                    ) {
                        Text("Collapse", color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grid of toggle cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Wi-Fi Tile
                    ControlTile(
                        label = "Wi-Fi",
                        status = if (wifiEnabled) "Connected" else "Off",
                        icon = Icons.Rounded.Wifi,
                        isActive = wifiEnabled,
                        onClick = { wifiEnabled = !wifiEnabled },
                        modifier = Modifier.weight(1f)
                    )

                    // Bluetooth Tile
                    ControlTile(
                        label = "Bluetooth",
                        status = if (bluetoothEnabled) "On" else "Off",
                        icon = Icons.Rounded.Bluetooth,
                        isActive = bluetoothEnabled,
                        onClick = { bluetoothEnabled = !bluetoothEnabled },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flashlight Tile
                    ControlTile(
                        label = "Torch",
                        status = if (flashlightEnabled) "Active" else "Disabled",
                        icon = Icons.Rounded.FlashlightOn,
                        isActive = flashlightEnabled,
                        onClick = { flashlightEnabled = !flashlightEnabled },
                        modifier = Modifier.weight(1f)
                    )

                    // DND Dummy Tile
                    ControlTile(
                        label = "Mute",
                        status = "Normal Mode",
                        icon = Icons.Rounded.NotificationsActive,
                        isActive = false,
                        onClick = {
                            // Quick volume mute shortcut
                            currentVolume = 0
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brightness Slider Card
                SliderCard(
                    title = "Screen Brightness",
                    value = brightnessLevel,
                    icon = Icons.Rounded.WbSunny,
                    onValueChange = { brightnessLevel = it },
                    valueLabel = "${(brightnessLevel * 100).toInt()}%"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // System Volume Slider Card (Bound directly to Hardware Music level)
                SliderCard(
                    title = "Media Volume",
                    value = currentVolume.toFloat() / maxMusicVolume,
                    icon = Icons.Rounded.VolumeUp,
                    onValueChange = { ratio ->
                        val targetVolume = (ratio * maxMusicVolume).toInt()
                        currentVolume = targetVolume
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                    },
                    valueLabel = "$currentVolume/$maxMusicVolume"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Close Gesture Pull Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clickable { viewModel.controlCenterExpanded.value = false },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Swipe up gesture bar",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "SWIPE UP TO CLOSE",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlTile(
    label: String,
    status: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                else Color.White.copy(alpha = 0.1f)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color.White.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = status,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SliderCard(
    title: String,
    value: Float,
    icon: ImageVector,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = valueLabel,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    thumbColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

package com.example

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.animations.pressBounce
import com.example.models.DesktopItemEntity
import com.example.ui.*
import com.example.viewmodel.LauncherViewModel
import com.example.wallpaper.ParallaxWallpaper
import com.example.widgets.ClockWidget
import com.example.widgets.MusicWidget
import com.example.widgets.WeatherWidget
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LauncherViewModel = viewModel()
            val themeMode by viewModel.darkThemeMode.collectAsState()

            val isDarkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                HomeScreenContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val desktopItems by viewModel.desktopItems.collectAsState()
    val folderItems by viewModel.folderItems.collectAsState()
    val colsCount by viewModel.gridCols.collectAsState()
    val rowsCount by viewModel.gridRows.collectAsState()
    val iconScaleMultiplier by viewModel.iconScale.collectAsState()
    val rawWallpaperId by viewModel.selectedWallpaperUri.collectAsState()

    // Motion states
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    
    val activeFolder by viewModel.activeFolder.collectAsState()
    val controlCenterExpanded by viewModel.controlCenterExpanded.collectAsState()

    // Live Widgets flow values
    val batteryPercent by viewModel.batteryPct.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val currentTemp by viewModel.currentTemp.collectAsState()
    val weatherCondition by viewModel.weatherCondition.collectAsState()
    val weatherCity by viewModel.weatherCity.collectAsState()

    // Drag-And-Drop / Reorganization dialog states
    var selectedItemForAction by remember { mutableStateOf<DesktopItemEntity?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    // Swipe vertical gestures detection values
    val verticalDragState = rememberDraggableState { delta ->
        // Swipe Up to expose Drawers
        if (delta < -22f && !showAppDrawer && !controlCenterExpanded) {
            showAppDrawer = true
        }
        // Swipe Down to pull Control Center
        if (delta > 22f && !showAppDrawer && !controlCenterExpanded) {
            viewModel.controlCenterExpanded.value = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                orientation = Orientation.Vertical,
                state = verticalDragState
            )
    ) {
        // --- NEW iOS 27 DYNAMIC SOLID-STATUS LIQUID ISLAND OVERLAY ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            LiquidIsland(viewModel = viewModel)
        }

        // 1. Wallpaper Parallax canvas
        ParallaxWallpaper(
            wallpaperId = rawWallpaperId,
            dragOffset = dragOffset
        ) {
            // Main Desktop Workspace Layout Grid
            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                ) {
                    val boardWidth = maxWidth
                    val boardHeight = maxHeight - 60.dp // leave some breathing room at the bottom dock space

                    val itemWidth = boardWidth / colsCount
                    val itemHeight = boardHeight / rowsCount

                    // Iterate over Cartesian placement items
                    desktopItems.forEach { item ->
                        val itemCellWidth = itemWidth * item.spanX
                        val itemCellHeight = itemHeight * item.spanY

                        val itemXOffset = itemWidth * item.cellX
                        val itemYOffset = itemHeight * item.cellY

                        Box(
                            modifier = Modifier
                                .offset(x = itemXOffset, y = itemYOffset)
                                .size(width = itemCellWidth, height = itemCellHeight)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (item.itemType) {
                                "WIDGET" -> {
                                    // Custom Widgets switcher
                                    when (item.label) {
                                        "CLOCK_WIDGET" -> ClockWidget()
                                        "WEATHER_WIDGET" -> WeatherWidget(
                                            temp = currentTemp,
                                            condition = weatherCondition,
                                            city = weatherCity,
                                            batteryPercent = batteryPercent,
                                            isCharging = isCharging
                                        )
                                        "MUSIC_WIDGET" -> MusicWidget()
                                    }
                                }
                                "FOLDER" -> {
                                    // Folders capsule items
                                    val nestedCount = folderItems.count { it.folderId == item.folderId }
                                    val nestedApps = folderItems.filter { it.folderId == item.folderId }.take(4)
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pressBounce(
                                                onLongClick = {
                                                    selectedItemForAction = item
                                                    showActionMenu = true
                                                },
                                                onClick = {
                                                    viewModel.activeFolder.value = item
                                                }
                                            ),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // Frosted mini grid container
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp * iconScaleMultiplier)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.15f))
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (nestedCount == 0) {
                                                Icon(
                                                    imageVector = Icons.Rounded.FolderOpen,
                                                    contentDescription = item.label,
                                                    tint = Color.White.copy(alpha = 0.6f)
                                                )
                                            } else {
                                                // 2x2 grid representing folder apps preview
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f)))
                                                        Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f)))
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f)))
                                                        Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f)))
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = item.label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                "APP" -> {
                                    val colorBase = Color(item.packageName.hashCode() or 0xFF000000.toInt()).copy(alpha = 0.95f).compositeOverLight()
                                    val colorSecondary = Color((item.packageName.hashCode() * 37) or 0xFF000000.toInt()).copy(alpha = 0.95f).compositeOverLight()
                                    // Custom visual container simulating standard app icons
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pressBounce(
                                                onLongClick = {
                                                    selectedItemForAction = item
                                                    showActionMenu = true
                                                },
                                                onClick = {
                                                    viewModel.launchAppShortcut(item.packageName, item.className)
                                                }
                                            ),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp * iconScaleMultiplier)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(colorBase, colorSecondary)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.label.firstOrNull()?.toString()?.uppercase() ?: "",
                                                color = Color.White,
                                                fontSize = (22 * iconScaleMultiplier).sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = item.label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Static bottom dock drawer shortcut trigger styled as a premium Bento Dock
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .clickable { showAppDrawer = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "SWIPE UP FOR APPS",
                                tint = Color(0xFF90CAF9), // light-blue bento accent
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SWIPE UP OR TAP FOR ALL APPLICATIONS",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Control Center Slide Drawer Overlays
        AnimatedVisibility(
            visible = controlCenterExpanded,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(stiffness = 300f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = spring(stiffness = 300f)
            ) + fadeOut()
        ) {
            ControlCenter(viewModel = viewModel)
        }

        // 3. Immersive Folder Grid overlays
        activeFolder?.let { activeFolderEntity ->
            AnimatedVisibility(
                visible = activeFolder != null,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                FolderOverlay(
                    viewModel = viewModel,
                    folder = activeFolderEntity,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 4. Swipe App Drawer over desktop
        AnimatedVisibility(
            visible = showAppDrawer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = 250f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = 250f)
            ) + fadeOut()
        ) {
            AppDrawer(
                viewModel = viewModel,
                onClose = { showAppDrawer = false }
            )
        }

        // 5. Settings Configuration Panel Overlay Screen
        AnimatedVisibility(
            visible = showSettingsScreen,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(stiffness = 200f)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(stiffness = 200f)
            )
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { showSettingsScreen = false }
            )
        }
    }

    // LONG PRESS APP DIALOG MENU options list
    selectedItemForAction?.let { item ->
        AlertDialog(
            onDismissRequest = {
                showActionMenu = false
                selectedItemForAction = null
            },
            title = {
                Text(
                    text = "Desktop Option: ${item.label}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Option 1: Move Grid position
                    Button(
                        onClick = {
                            showMoveDialog = true
                            showActionMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.OpenWith, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Relocate Grid coordinate")
                        }
                    }

                    // Option 2: Delete shortcut
                    Button(
                        onClick = {
                            viewModel.removeItem(item)
                            selectedItemForAction = null
                            showActionMenu = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete from Desktop")
                        }
                    }

                    // Option 3: Merge folders (if itemType is APP)
                    if (item.itemType == "APP") {
                        Button(
                            onClick = {
                                showCreateFolderDialog = true
                                showActionMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Merge into New Folder")
                            }
                        }
                    }

                    // Option 4: Open launcher settings
                    TextButton(
                        onClick = {
                            showSettingsScreen = true
                            selectedItemForAction = null
                            showActionMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launcher Settings")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedItemForAction = null
                        showActionMenu = false
                    }
                ) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Grid Coordinates Relocation Selector Dialog
    if (showMoveDialog && selectedItemForAction != null) {
        val context = LocalContext.current
        val item = selectedItemForAction!!
        var tempX by remember { mutableStateOf(item.cellX.toString()) }
        var tempY by remember { mutableStateOf(item.cellY.toString()) }

        AlertDialog(
            onDismissRequest = {
                showMoveDialog = false
                selectedItemForAction = null
            },
            title = { Text("Relocate ${item.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Set cell positions matching cols (0..${colsCount - 1}) and rows (0..${rowsCount - 1})", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tempX,
                            onValueChange = { tempX = it },
                            label = { Text("Cell X") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tempY,
                            onValueChange = { tempY = it },
                            label = { Text("Cell Y") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalX = tempX.toIntOrNull() ?: item.cellX
                        val finalY = tempY.toIntOrNull() ?: item.cellY
                        
                        if (finalX in 0 until colsCount && finalY in 0 until rowsCount) {
                            viewModel.moveDesktopItem(item, finalX, finalY)
                        } else {
                            Toast.makeText(context, "Out of boundary cells!", Toast.LENGTH_SHORT).show()
                        }
                        showMoveDialog = false
                        selectedItemForAction = null
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMoveDialog = false
                        selectedItemForAction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Merge folder creation selection dialog
    if (showCreateFolderDialog && selectedItemForAction != null) {
        val app1 = selectedItemForAction!!
        var selectedPartnerApp by remember { mutableStateOf<DesktopItemEntity?>(null) }
        var folderLabel by remember { mutableStateOf("New Folder") }

        // Find available candidate apps on screen
        val partnerCandidates = remember(desktopItems) {
            desktopItems.filter { it.itemType == "APP" && it.id != app1.id }
        }

        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                selectedItemForAction = null
            },
            title = { Text("Create Folder") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = folderLabel,
                        onValueChange = { folderLabel = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Merge '${app1.label}' with which desktop application?", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    if (partnerCandidates.isEmpty()) {
                        Text("No other standalone apps available on desktop", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        partnerCandidates.forEach { partner ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedPartnerApp?.id == partner.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedPartnerApp = partner }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPartnerApp?.id == partner.id,
                                    onClick = { selectedPartnerApp = partner }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(partner.label, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedPartnerApp != null && folderLabel.trim().isNotEmpty(),
                    onClick = {
                        val partner = selectedPartnerApp!!
                        viewModel.mergeAppsIntoFolder(app1, partner, folderLabel.trim())
                        showCreateFolderDialog = false
                        selectedItemForAction = null
                    }
                ) {
                    Text("Create Folder")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFolderDialog = false
                        selectedItemForAction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

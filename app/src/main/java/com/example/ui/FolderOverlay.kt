package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animations.pressBounce
import com.example.models.DesktopItemEntity
import com.example.models.FolderItemEntity
import com.example.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderOverlay(
    viewModel: LauncherViewModel,
    folder: DesktopItemEntity,
    modifier: Modifier = Modifier
) {
    val folderItems by viewModel.folderItems.collectAsState()
    val nestedApps = remember(folderItems, folder) {
        folderItems.filter { it.folderId == folder.folderId }
    }
    val coroutineScope = rememberCoroutineScope()

    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(folder.label) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Click outside handler box
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = { viewModel.activeFolder.value = null }),
        contentAlignment = Alignment.Center
    ) {
        // Inner Glass Folder Card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.6f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable(enabled = false) { /* Prevent click through */ }
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Folder Title Header (Click to Rename)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            tempName = folder.label
                            showEditDialog = true
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = folder.label,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename Folder",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (nestedApps.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Empty Folder\nDrag apps here from the grid or swipe-up to add",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Responsive nested applications grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(nestedApps) { app ->
                            Box(
                                contentAlignment = Alignment.TopEnd,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Launcher App Icon Card
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressBounce {
                                            viewModel.launchAppShortcut(app.packageName, app.className)
                                            viewModel.activeFolder.value = null // dismiss folder on launch
                                        }
                                        .padding(8.dp)
                                ) {
                                    // Custom visual container simulating standard app icons
                                    val colorBase = Color(app.packageName.hashCode() or 0xFF000000.toInt()).copy(alpha = 0.95f).compositeOverLight()
                                    val colorSecondary = Color((app.packageName.hashCode() * 37) or 0xFF000000.toInt()).copy(alpha = 0.95f).compositeOverLight()
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(colorBase, colorSecondary)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.label.firstOrNull()?.toString()?.uppercase() ?: "",
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = app.label,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Remove shortcut handler button
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.removeFolderItem(app)
                                            // Put back on desktop pool
                                            viewModel.addDrawerAppToDesktop(
                                                app = com.example.repository.InstalledApp(
                                                    label = app.label,
                                                    packageName = app.packageName,
                                                    className = app.className
                                                ),
                                                cellX = (0..3).random(), // find first empty space or randomly allocate
                                                cellY = (4..5).random()
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.RemoveCircle,
                                        contentDescription = "Remove From Folder",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Close Button
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    onClick = { viewModel.activeFolder.value = null },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close folder",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Interactive Rename Dialog UI
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.trim().isNotEmpty()) {
                            viewModel.renameFolder(folder, tempName.trim())
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper to ensure colors generated from package hashes remain light and accessible
fun Color.compositeOverLight(): Color {
    val red = (this.red * 0.7f) + 0.3f
    val green = (this.green * 0.7f) + 0.3f
    val blue = (this.blue * 0.7f) + 0.3f
    return Color(red, green, blue, this.alpha)
}

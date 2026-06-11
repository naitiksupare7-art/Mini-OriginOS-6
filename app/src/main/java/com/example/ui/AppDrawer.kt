package com.example.ui

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.AddHome
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animations.pressBounce
import com.example.repository.InstalledApp
import com.example.viewmodel.LauncherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    viewModel: LauncherViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.drawerSearchQuery.collectAsState()
    val apps by viewModel.filteredDrawerApps.collectAsState()
    val desktopItems by viewModel.desktopItems.collectAsState()

    var selectedAppForPin by remember { mutableStateOf<InstalledApp?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF2121212)) // Dark, high-contrast, eye-safe backdrop drawer overlay
            .statusBarsPadding()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            .clickable(enabled = false) {} // block click through
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Search Input Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.drawerSearchQuery.value = it },
                    placeholder = { Text("Search installed applications...", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.drawerSearchQuery.value = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close Drawer", tint = Color.White)
                }
            }

            // Quick App List status labels
            Text(
                text = "${apps.size} APPS INSTALLED",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )

            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No applications match your search query.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Responsive Apps Grid (4 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(apps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressBounce(
                                    onLongClick = {
                                        selectedAppForPin = app
                                    },
                                    onClick = {
                                        viewModel.launchAppShortcut(app.packageName, app.className)
                                        onClose()
                                    }
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            // Custom App Icon container
                            val colorBase = Color(app.packageName.hashCode() or 0xFF000000.toInt()).copy(alpha = 0.85f).compositeOverLight()
                            val colorSecondary = Color((app.packageName.hashCode() * 37) or 0xFF000000.toInt()).copy(alpha = 0.85f).compositeOverLight()
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
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Pin option Context Dialog
    selectedAppForPin?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForPin = null },
            title = { Text("Application Action") },
            text = { Text("Would you like to pin '${app.label}' directly to your main home workspace grid?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Find first vacant cell position coordinate
                        val cols = viewModel.gridCols.value
                        val rows = viewModel.gridRows.value
                        var foundVacancy = false
                        var targetX = 0
                        var targetY = 3 // start checking vacancy after top widgets rows

                        outer@ for (y in 3 until rows) {
                            for (x in 0 until cols) {
                                val occupied = desktopItems.any { it.cellX == x && it.cellY == y }
                                if (!occupied) {
                                    targetX = x
                                    targetY = y
                                    foundVacancy = true
                                    break@outer
                                }
                            }
                        }

                        if (!foundVacancy) {
                            // Fallback to random coordinate
                            targetX = (0 until cols).random()
                            targetY = (3 until rows).random()
                        }

                        viewModel.addDrawerAppToDesktop(app, targetX, targetY)
                        selectedAppForPin = null
                        onClose()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.AddHome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pin to Screen")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Immediately launch as alternative action
                        viewModel.launchAppShortcut(app.packageName, app.className)
                        selectedAppForPin = null
                        onClose()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Launch")
                    }
                }
            }
        )
    }
}

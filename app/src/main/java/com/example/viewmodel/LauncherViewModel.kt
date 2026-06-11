package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.models.DesktopItemEntity
import com.example.models.FolderItemEntity
import com.example.repository.InstalledApp
import com.example.repository.LauncherDatabase
import com.example.repository.LauncherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LauncherDatabase.getDatabase(application)
    private val repository = LauncherRepository(application, db.launcherDao())

    // App Drawer Query State
    val drawerSearchQuery = MutableStateFlow("")

    // Raw installed apps
    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    // Filtered drawer apps
    val filteredDrawerApps: StateFlow<List<InstalledApp>> = combine(
        _installedApps,
        drawerSearchQuery
    ) { apps, query ->
        if (query.trim().isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database UI States
    val desktopItems: StateFlow<List<DesktopItemEntity>> = repository.desktopItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderItems: StateFlow<List<FolderItemEntity>> = repository.folderItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings States (Grid, Icon, Animations)
    private val _gridCols = MutableStateFlow(4)
    val gridCols = _gridCols.asStateFlow()

    private val _gridRows = MutableStateFlow(6)
    val gridRows = _gridRows.asStateFlow()

    private val _iconScale = MutableStateFlow(1.0f) // multiplier: 0.8f (Small), 1.0f (Medium), 1.2f (Large)
    val iconScale = _iconScale.asStateFlow()

    private val _animationSpeed = MutableStateFlow(1.0f) // multiplier: 0.5f (Relaxed), 1.0f (Medium), 1.5f (Fast)
    val animationSpeed = _animationSpeed.asStateFlow()

    private val _darkThemeMode = MutableStateFlow("system") // "light", "dark", "system"
    val darkThemeMode = _darkThemeMode.asStateFlow()

    private val _selectedWallpaperUri = MutableStateFlow<String>("default_wallpaper")
    val selectedWallpaperUri = _selectedWallpaperUri.asStateFlow()

    // Active Screens/UI state
    val activeFolder = MutableStateFlow<DesktopItemEntity?>(null)
    val controlCenterExpanded = MutableStateFlow(false)

    // Dynamic Widgets Live Data
    val batteryPct = MutableStateFlow(100)
    val isCharging = MutableStateFlow(false)
    val currentTemp = MutableStateFlow(24) // mock weather initialized at standard temp
    val weatherCondition = MutableStateFlow("Partly Cloudy")
    val weatherCity = MutableStateFlow("Beijing")

    init {
        // Hydrate settings
        viewModelScope.launch {
            repository.settingsFlow.collect { settingEntities ->
                settingEntities.forEach { setting ->
                    when (setting.key) {
                        "grid_cols" -> _gridCols.value = setting.value.toIntOrNull() ?: 4
                        "grid_rows" -> _gridRows.value = setting.value.toIntOrNull() ?: 6
                        "icon_scale" -> _iconScale.value = setting.value.toFloatOrNull() ?: 1.0f
                        "animation_speed" -> _animationSpeed.value = setting.value.toFloatOrNull() ?: 1.0f
                        "theme_mode" -> _darkThemeMode.value = setting.value
                        "wallpaper_uri" -> _selectedWallpaperUri.value = setting.value
                    }
                }
            }
        }

        // Fetch installed apps
        refreshInstalledApps()

        // Sync initial widgets layout and default items when database completes loading
        viewModelScope.launch {
            desktopItems.collect { items ->
                if (items.isEmpty() && _installedApps.value.isNotEmpty()) {
                    setupDefaultLayout()
                }
            }
        }

        // Live battery stats
        updateBatteryStats()
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _installedApps.value = apps
            
            // Check if db layout needs population
            if (desktopItems.value.isEmpty() && apps.isNotEmpty()) {
                setupDefaultLayout()
            }
        }
    }

    private fun updateBatteryStats() {
        val application = getApplication<Application>()
        try {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                application.registerReceiver(null, filter)
            }
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isChargingStatus = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPctValue = (level * 100 / scale.toFloat()).toInt()

            batteryPct.value = if (batteryPctValue >= 0) batteryPctValue else 85
            isCharging.value = isChargingStatus
        } catch (e: Exception) {
            Log.e("LauncherViewModel", "Failed to retrieve battery status", e)
            batteryPct.value = 75
            isCharging.value = false
        }
    }

    // Populate a gorgeous standard launcher screen on pristine installations
    private suspend fun setupDefaultLayout() {
        val apps = _installedApps.value
        if (apps.isEmpty()) return

        Log.d("LauncherViewModel", "Initializing beautiful default layout...")

        // WIDGET 1: Modern Large Clock Widget (cellX=0, cellY=0, spanX=2, spanY=2)
        repository.addDesktopItem(
            DesktopItemEntity(
                itemType = "WIDGET",
                label = "CLOCK_WIDGET",
                cellX = 0,
                cellY = 0,
                spanX = 2,
                spanY = 2
            )
        )

        // WIDGET 2: Weather and Battery Capsule Widget (cellX=2, cellY=0, spanX=2, spanY=2)
        repository.addDesktopItem(
            DesktopItemEntity(
                itemType = "WIDGET",
                label = "WEATHER_WIDGET",
                cellX = 2,
                cellY = 0,
                spanX = 2,
                spanY = 2
            )
        )

        // WIDGET 3: Device Info & Music Card (cellX=0, cellY=2, spanX=4, spanY=1)
        repository.addDesktopItem(
            DesktopItemEntity(
                itemType = "WIDGET",
                label = "MUSIC_WIDGET",
                cellX = 0,
                cellY = 2,
                spanX = 4,
                spanY = 1
            )
        )

        // Place initial app shortcut items on the next grid lines
        var appIdx = 0
        val rowsToFill = 3..5
        val colsToFill = 0..3

        outer@ for (y in rowsToFill) {
            for (x in colsToFill) {
                if (appIdx >= apps.size) break@outer
                
                // Let's create a beautiful folder at cellX=0, cellY=4 to demonstrate folder nesting
                if (x == 0 && y == 4 && apps.size >= 3) {
                    val folderId = UUID.randomUUID().toString()
                    val folderEntity = DesktopItemEntity(
                        itemType = "FOLDER",
                        label = "Social Apps",
                        folderId = folderId,
                        cellX = x,
                        cellY = y,
                        spanX = 1,
                        spanY = 1
                    )
                    repository.addDesktopItem(folderEntity)

                    // Nest 3 apps in this folder
                    for (i in 0..2) {
                        if (appIdx < apps.size) {
                            val nestedApp = apps[appIdx++]
                            repository.addFolderItem(
                                FolderItemEntity(
                                    folderId = folderId,
                                    label = nestedApp.label,
                                    packageName = nestedApp.packageName,
                                    className = nestedApp.className
                                )
                            )
                        }
                    }
                } else {
                    // Place a regular app shortcut
                    val app = apps[appIdx++]
                    repository.addDesktopItem(
                        DesktopItemEntity(
                            itemType = "APP",
                            label = app.label,
                            packageName = app.packageName,
                            className = app.className,
                            cellX = x,
                            cellY = y,
                            spanX = 1,
                            spanY = 1
                        )
                    )
                }
            }
        }
    }

    // Actions
    fun launchAppShortcut(packageName: String, className: String) {
        repository.launchApp(packageName, className)
    }

    fun updateGridSize(cols: Int, rows: Int) {
        viewModelScope.launch {
            _gridCols.value = cols
            _gridRows.value = rows
            repository.saveSetting("grid_cols", cols.toString())
            repository.saveSetting("grid_rows", rows.toString())
        }
    }

    fun updateIconScale(scale: Float) {
        viewModelScope.launch {
            _iconScale.value = scale
            repository.saveSetting("icon_scale", scale.toString())
        }
    }

    fun updateAnimationSpeed(speed: Float) {
        viewModelScope.launch {
            _animationSpeed.value = speed
            repository.saveSetting("animation_speed", speed.toString())
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            _darkThemeMode.value = mode
            repository.saveSetting("theme_mode", mode)
        }
    }

    fun changeWallpaper(uri: String) {
        viewModelScope.launch {
            _selectedWallpaperUri.value = uri
            repository.saveSetting("wallpaper_uri", uri)
        }
    }

    // Folder Mutation: Add App into Folder
    fun dragAppIntoFolder(app: InstalledApp, folder: DesktopItemEntity) {
        viewModelScope.launch {
            repository.addFolderItem(
                FolderItemEntity(
                    folderId = folder.folderId,
                    label = app.label,
                    packageName = app.packageName,
                    className = app.className
                )
            )
            // If the app was currently a standalone desktop item, we delete it from desktop
            desktopItems.value.find { it.packageName == app.packageName }?.let { desktopItem ->
                repository.deleteDesktopItem(desktopItem)
            }
        }
    }

    // Create New Folder from pairing two standalone apps
    fun mergeAppsIntoFolder(app1: DesktopItemEntity, app2: DesktopItemEntity, folderName: String) {
        viewModelScope.launch {
            val folderId = UUID.randomUUID().toString()
            val finalFolderName = folderName.ifEmpty { "New Folder" }
            
            // Create Folder item on Desktop at app1's position
            val folderEntity = DesktopItemEntity(
                itemType = "FOLDER",
                label = finalFolderName,
                folderId = folderId,
                cellX = app1.cellX,
                cellY = app1.cellY,
                spanX = 1,
                spanY = 1
            )
            repository.addDesktopItem(folderEntity)

            // Nest app1
            repository.addFolderItem(
                FolderItemEntity(
                    folderId = folderId,
                    label = app1.label,
                    packageName = app1.packageName,
                    className = app1.className
                )
            )

            // Nest app2
            repository.addFolderItem(
                FolderItemEntity(
                    folderId = folderId,
                    label = app2.label,
                    packageName = app2.packageName,
                    className = app2.className
                )
            )

            // Delete standalone app shortcuts
            repository.deleteDesktopItem(app1)
            repository.deleteDesktopItem(app2)
        }
    }

    // Drag-And-Drop: Move Desktop Item coordinates
    fun moveDesktopItem(item: DesktopItemEntity, targetX: Int, targetY: Int) {
        viewModelScope.launch {
            viewModelScope.launch {
                val updated = item.copy(cellX = targetX, cellY = targetY)
                repository.updateDesktopItem(updated)
            }
        }
    }

    // Add Standalone App to Desktop from drawer
    fun addDrawerAppToDesktop(app: InstalledApp, cellX: Int, cellY: Int) {
        viewModelScope.launch {
            // Check if there is already an item at cellX/cellY to avoid overlaps
            val overlaps = desktopItems.value.any { it.cellX == cellX && it.cellY == cellY }
            if (!overlaps) {
                repository.addDesktopItem(
                    DesktopItemEntity(
                        itemType = "APP",
                        label = app.label,
                        packageName = app.packageName,
                        className = app.className,
                        cellX = cellX,
                        cellY = cellY
                    )
                )
            }
        }
    }

    // Remove Item from Desktop cleanly
    fun removeItem(item: DesktopItemEntity) {
        viewModelScope.launch {
            repository.deleteDesktopItem(item)
        }
    }

    fun removeFolderItem(item: FolderItemEntity) {
        viewModelScope.launch {
            repository.removeFolderItem(item)
        }
    }

    fun renameFolder(folder: DesktopItemEntity, newName: String) {
        viewModelScope.launch {
            val updated = folder.copy(label = newName)
            repository.updateDesktopItem(updated)
            if (activeFolder.value?.folderId == folder.folderId) {
                activeFolder.value = updated
            }
        }
    }
}

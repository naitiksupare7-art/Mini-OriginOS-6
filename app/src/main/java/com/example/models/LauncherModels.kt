package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "launcher_settings")
data class LauncherSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "desktop_items")
data class DesktopItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemType: String, // "APP", "FOLDER", "WIDGET"
    val label: String,
    val packageName: String = "",
    val className: String = "",
    val folderId: String = "",
    val cellX: Int = -1,
    val cellY: Int = -1,
    val spanX: Int = 1,
    val spanY: Int = 1
)

@Entity(tableName = "folder_items")
data class FolderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: String,
    val label: String,
    val packageName: String,
    val className: String
)

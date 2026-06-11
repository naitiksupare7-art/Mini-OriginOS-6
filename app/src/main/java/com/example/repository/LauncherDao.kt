package com.example.repository

import androidx.room.*
import com.example.models.DesktopItemEntity
import com.example.models.FolderItemEntity
import com.example.models.LauncherSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {

    // Settings
    @Query("SELECT * FROM launcher_settings")
    fun getSettingsFlow(): Flow<List<LauncherSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: LauncherSettingEntity)

    // Desktop Items
    @Query("SELECT * FROM desktop_items")
    fun getDesktopItemsFlow(): Flow<List<DesktopItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesktopItem(item: DesktopItemEntity): Long

    @Update
    suspend fun updateDesktopItem(item: DesktopItemEntity)

    @Delete
    suspend fun deleteDesktopItem(item: DesktopItemEntity)

    @Query("DELETE FROM desktop_items WHERE id = :id")
    suspend fun deleteDesktopItemById(id: Int)

    // Folder Items
    @Query("SELECT * FROM folder_items")
    fun getFolderItemsFlow(): Flow<List<FolderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderItem(item: FolderItemEntity): Long

    @Delete
    suspend fun deleteFolderItem(item: FolderItemEntity)

    @Query("DELETE FROM folder_items WHERE folderId = :folderId")
    suspend fun deleteFolderItemsByFolderId(folderId: String)
}

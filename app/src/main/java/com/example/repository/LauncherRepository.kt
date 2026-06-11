package com.example.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import com.example.models.DesktopItemEntity
import com.example.models.FolderItemEntity
import com.example.models.LauncherSettingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class InstalledApp(
    val label: String,
    val packageName: String,
    val className: String,
    val iconDrawable: Drawable? = null
)

class LauncherRepository(
    private val context: Context,
    private val launcherDao: LauncherDao
) {
    private val packageManager: PackageManager = context.packageManager

    val settingsFlow: Flow<List<LauncherSettingEntity>> = launcherDao.getSettingsFlow()
    val desktopItemsFlow: Flow<List<DesktopItemEntity>> = launcherDao.getDesktopItemsFlow()
    val folderItemsFlow: Flow<List<FolderItemEntity>> = launcherDao.getFolderItemsFlow()

    // Query all launchable apps
    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val myPackageName = context.packageName

        resolveInfoList.mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            // Exclude our own launcher from the drawer app list
            if (packageName == myPackageName) return@mapNotNull null

            val className = info.activityInfo.name
            val label = info.loadLabel(packageManager).toString()
            
            // Safe icon loading
            val icon = try {
                info.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }

            InstalledApp(
                label = label,
                packageName = packageName,
                className = className,
                iconDrawable = icon
            )
        }.sortedBy { it.label.lowercase() }
    }

    // Launch app safely
    fun launchApp(packageName: String, className: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(packageName, className)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("LauncherRepository", "Failed to launch app $packageName / $className", e)
            // Fallback launch intent
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    true
                } else false
            } catch (ex: Exception) {
                false
            }
        }
    }

    // Settings helpers
    suspend fun saveSetting(key: String, value: String) {
        launcherDao.insertSetting(LauncherSettingEntity(key, value))
    }

    // Desktop items helpers
    suspend fun addDesktopItem(item: DesktopItemEntity): Long {
        return launcherDao.insertDesktopItem(item)
    }

    suspend fun updateDesktopItem(item: DesktopItemEntity) {
        launcherDao.updateDesktopItem(item)
    }

    suspend fun deleteDesktopItem(item: DesktopItemEntity) {
        launcherDao.deleteDesktopItem(item)
        if (item.itemType == "FOLDER") {
            launcherDao.deleteFolderItemsByFolderId(item.folderId)
        }
    }

    suspend fun deleteDesktopItemById(id: Int) {
        launcherDao.deleteDesktopItemById(id)
    }

    // Folder helpers
    suspend fun addFolderItem(item: FolderItemEntity): Long {
        return launcherDao.insertFolderItem(item)
    }

    suspend fun removeFolderItem(item: FolderItemEntity) {
        launcherDao.deleteFolderItem(item)
    }

    suspend fun clearFolderItems(folderId: String) {
        launcherDao.deleteFolderItemsByFolderId(folderId)
    }
}

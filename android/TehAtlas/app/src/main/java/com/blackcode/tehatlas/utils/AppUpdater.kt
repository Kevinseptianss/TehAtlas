package com.blackcode.tehatlas.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.runtime.*
import com.blackcode.tehatlas.network.AppVersionDto
import com.blackcode.tehatlas.network.RetrofitClient
import com.blackcode.tehatlas.ui.UpdateDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AppUpdater {
    var showUpdateDialog by mutableStateOf(false)
        private set

    // Remote version info from API
    var remoteVersion by mutableStateOf<AppVersionDto?>(null)
        private set

    var currentVersionName by mutableStateOf("")
        private set

    private const val FILE_NAME = "tehatlas-update.apk"
    private const val TAG = "AppUpdater"

    fun showAlert() {
        showUpdateDialog = true
    }

    fun dismissAlert() {
        showUpdateDialog = false
    }

    @Composable
    fun Component() {
        if (showUpdateDialog && remoteVersion != null) {
            UpdateDialog(
                currentVersion = currentVersionName,
                remoteVersion = remoteVersion!!,
                onDismiss = { dismissAlert() }
            )
        }
    }

    /**
     * Checks for updates by calling the API and comparing versionCode.
     * Automatically shows the update dialog if a newer version is available.
     * @param showToastIfUpToDate If true, shows a toast if no update is needed.
     */
    suspend fun checkForUpdate(context: Context, showToastIfUpToDate: Boolean = false) {
        try {
            // Get current app version
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            currentVersionName = packageInfo.versionName ?: "1.0.0"

            Log.d(TAG, "Current version: $currentVersionName (code $currentVersionCode)")

            // Call API
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.getApiService().checkAppVersion()
            }

            if (response.isSuccessful && response.body()?.success == true) {
                val versionData = response.body()?.data
                if (versionData != null && versionData.versionCode > currentVersionCode) {
                    Log.d(TAG, "Update available: ${versionData.versionName} (code ${versionData.versionCode})")
                    remoteVersion = versionData
                    showUpdateDialog = true
                } else {
                    Log.d(TAG, "App is up to date")
                    if (showToastIfUpToDate) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Versi aplikasi paling baru, belum ada update.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Log.w(TAG, "Version check failed: ${response.code()}")
                if (showToastIfUpToDate) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal memeriksa update. Coba lagi nanti.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            if (showToastIfUpToDate) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun downloadApk(context: Context, downloadUrl: String): Long {
        Log.d(TAG, "Starting download from: $downloadUrl")
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)

        // Use internal external directory (no permission needed)
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadFolder, FILE_NAME)
        if (apkFile.exists()) {
            val deleted = apkFile.delete()
            Log.d(TAG, "Existing APK deleted: $deleted")
        }

        try {
            val request = DownloadManager.Request(uri)
                .setTitle("Updating TehAtlas")
                .setDescription("Downloading latest version...")
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                request.setRequiresCharging(false)
                request.setRequiresDeviceIdle(false)
            }

            val downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "Download enqueued with ID: $downloadId")

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        Log.d(TAG, "Download $id complete notification received")
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}
                    }
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            return downloadId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue download", e)
            Toast.makeText(context, "Gagal memulai unduhan: ${e.message}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }

    fun getDownloadProgress(context: Context, downloadId: Long): Float {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

            cursor.close()
            if (bytesTotal > 0) {
                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                if (progress > 0.95f) {
                    Log.d(TAG, "Progress: $progress ($bytesDownloaded / $bytesTotal), Status: $status")
                }
                return progress
            }
        }
        return 0f
    }

    fun isDownloadFinished(context: Context, downloadId: Long): Boolean {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        var finished = false
        if (cursor != null && cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            
            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    finished = true
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    Log.e(TAG, "Download failed with reason: $reason")
                    finished = true
                }
                DownloadManager.STATUS_PAUSED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    Log.d(TAG, "Download paused with reason: $reason")
                }
            }
            cursor.close()
        }
        return finished
    }

    fun isDownloadSuccessful(context: Context, downloadId: Long): Boolean {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        var successful = false
        if (cursor != null && cursor.moveToFirst()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            successful = status == DownloadManager.STATUS_SUCCESSFUL
            cursor.close()
        }
        return successful
    }

    fun installApk(context: Context) {
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadFolder, FILE_NAME)

        if (apkFile.exists()) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "com.blackcode.tehatlas.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                setDataAndType(contentUri, "application/vnd.android.package-archive")
            }
            context.startActivity(installIntent)
        } else {
            Toast.makeText(context, "Gagal menemukan file update", Toast.LENGTH_SHORT).show()
        }
    }
}

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
     */
    suspend fun checkForUpdate(context: Context) {
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
                }
            } else {
                Log.w(TAG, "Version check failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
        }
    }

    fun downloadApk(context: Context, downloadUrl: String): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)

        // Clear existing file if any
        val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(downloadFolder, FILE_NAME)
        if (apkFile.exists()) {
            apkFile.delete()
        }

        val request = DownloadManager.Request(uri)
            .setTitle("Updating TehAtlas")
            .setDescription("Downloading latest version...")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
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
    }

    fun getDownloadProgress(context: Context, downloadId: Long): Float {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor != null && cursor.moveToFirst()) {
            val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

            cursor.close()
            if (bytesTotal > 0) {
                return bytesDownloaded.toFloat() / bytesTotal.toFloat()
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
            finished = status == DownloadManager.STATUS_SUCCESSFUL
            cursor.close()
        }
        return finished
    }

    fun installApk(context: Context) {
        val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
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

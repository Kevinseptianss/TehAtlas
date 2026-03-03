package com.blackcode.tehatlas.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.runtime.*
import com.blackcode.tehatlas.ui.UpdateDialog
import java.io.File

object AppUpdater {
    var showUpdateDialog by mutableStateOf(false)
        private set

    fun showAlert() {
        showUpdateDialog = true
    }

    fun dismissAlert() {
        showUpdateDialog = false
    }

    @Composable
    fun Component() {
        if (showUpdateDialog) {
            UpdateDialog(onDismiss = { dismissAlert() })
        }
    }

    private const val APK_URL = "http://api.tehatlas.my.id/app-debug.apk"
    private const val FILE_NAME = "tehatlas-update.apk"

    fun downloadApk(context: Context): Long {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(APK_URL)

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
                    // We don't auto-install here if we want the dialog to handle it, 
                    // but the user said "after download it prompt open the apk file we download"
                    // So we can keep the auto-install or trigger it from UI.
                    // For now, let's keep it as is or refine based on UI state.
                    installApk(context)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                    }
                }
            }
        }
        
        context.registerReceiver(
            onComplete, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_EXPORTED
        )
        
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

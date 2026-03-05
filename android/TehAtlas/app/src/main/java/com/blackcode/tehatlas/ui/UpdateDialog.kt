package com.blackcode.tehatlas.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.blackcode.tehatlas.network.AppVersionDto
import com.blackcode.tehatlas.ui.theme.*
import com.blackcode.tehatlas.utils.AppUpdater
import kotlinx.coroutines.delay

// ─── Premium color palette ─────────────────────────────────────────────
private val GradientStart = Color(0xFF667EEA)
private val GradientEnd = Color(0xFF764BA2)
private val GradientAccent = Color(0xFF6C63FF)
private val SuccessGreen = Color(0xFF00D68F)
private val SuccessGreenDark = Color(0xFF00B87C)
private val CardBg = Color(0xFFFDFDFF)
private val SubtleBg = Color(0xFFF4F6FC)
private val TextDark = Color(0xFF1B1D2E)
private val TextMedium = Color(0xFF6B7280)
private val TextLight = Color(0xFF9CA3AF)
private val ShimmerBase = Color(0xFFE8ECFF)
private val ShimmerHighlight = Color(0xFFF8F9FF)

@Composable
fun UpdateDialog(
    currentVersion: String,
    remoteVersion: AppVersionDto,
    onDismiss: () -> Unit
) {
    var isDownloading by remember { mutableStateOf(false) }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isFinished by remember { mutableStateOf(false) }
    var isDownloadSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Smooth animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Pulsing animation for the icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Shimmer offset for progress bar
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Rotating gradient angle for icon background
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientRotation"
    )

    // Track download progress
    LaunchedEffect(isDownloading, downloadId) {
        if (isDownloading && downloadId != -1L) {
            android.util.Log.d("AppUpdater", "Dialog tracking progress for ID: $downloadId")
            while (!isFinished) {
                progress = AppUpdater.getDownloadProgress(context, downloadId)
                isFinished = AppUpdater.isDownloadFinished(context, downloadId)
                
                if (isFinished) {
                    isDownloadSuccess = AppUpdater.isDownloadSuccessful(context, downloadId)
                    android.util.Log.d("AppUpdater", "Download finished. Success: $isDownloadSuccess")
                    if (isDownloadSuccess) {
                        progress = 1f
                    }
                    break
                }
                delay(500)
            }
        }
    }

    // Auto-install after download finishes successfully
    LaunchedEffect(isFinished, isDownloadSuccess) {
        if (isFinished && isDownloadSuccess) {
            delay(1200) // Longer pause to show 100% and success state
            AppUpdater.installApk(context)
        }
    }

    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = GradientAccent.copy(alpha = 0.15f),
                    spotColor = GradientAccent.copy(alpha = 0.2f)
                ),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ─── Animated Icon Header ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                        .shadow(12.dp, CircleShape, ambientColor = GradientStart.copy(alpha = 0.3f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isFinished) listOf(SuccessGreen, SuccessGreenDark)
                                else listOf(GradientStart, GradientEnd),
                                start = Offset(
                                    x = kotlin.math.cos(Math.toRadians(gradientAngle.toDouble())).toFloat() * 100f,
                                    y = kotlin.math.sin(Math.toRadians(gradientAngle.toDouble())).toFloat() * 100f
                                ),
                                end = Offset(
                                    x = kotlin.math.cos(Math.toRadians((gradientAngle + 180).toDouble())).toFloat() * 100f + 100f,
                                    y = kotlin.math.sin(Math.toRadians((gradientAngle + 180).toDouble())).toFloat() * 100f + 100f
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isFinished && isDownloadSuccess -> Icons.Filled.CheckCircle
                            isFinished && !isDownloadSuccess -> Icons.Filled.Error
                            isDownloading -> Icons.Filled.CloudDownload
                            else -> Icons.Filled.RocketLaunch
                        },
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Phase: Initial (Update Available) ────────────────────
                AnimatedVisibility(
                    visible = !isDownloading && !isFinished,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Update Tersedia! ✨",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                shadow = Shadow(
                                    color = GradientAccent.copy(alpha = 0.1f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            ),
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Version badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            GradientStart.copy(alpha = 0.08f),
                                            GradientEnd.copy(alpha = 0.08f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            // Current version
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "v$currentVersion",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextMedium
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                Icons.Filled.ArrowForward,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = GradientAccent
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // New version
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "v${remoteVersion.versionName}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Release notes header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GradientAccent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = GradientAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Apa yang baru",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.sp
                                ),
                                color = TextDark
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Release notes from API
                        val notes = remoteVersion.releaseNotes ?: listOf("Update terbaru tersedia")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SubtleBg)
                                .padding(16.dp)
                        ) {
                            notes.forEachIndexed { index, note ->
                                PremiumFeatureItem(note)
                                if (index < notes.lastIndex) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Action buttons
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Skip button
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    width = 1.5.dp
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextMedium
                                )
                            ) {
                                Text(
                                    "Nanti Saja",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Update button with gradient
                            Button(
                                onClick = {
                                    android.util.Log.d("AppUpdater", "Update button clicked. URL: ${remoteVersion.downloadUrl}")
                                    isDownloading = true
                                    downloadId = AppUpdater.downloadApk(context, remoteVersion.downloadUrl)
                                    android.util.Log.d("AppUpdater", "Download initiated, ID: $downloadId")
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(GradientStart, GradientEnd)
                                            ),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.CloudDownload,
                                            null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Update Sekarang",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Phase: Downloading ───────────────────────────────────
                AnimatedVisibility(
                    visible = isDownloading && !isFinished,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Mengunduh Update",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = when {
                                progress > 0.99f -> "Menyelesaikan unduhan..."
                                progress > 0.95f -> "Hampir selesai..."
                                else -> "Mohon tunggu sejenak..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMedium
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Premium progress bar with shimmer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(SubtleBg)
                        ) {
                            // Progress fill with gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(GradientStart, GradientAccent, GradientEnd)
                                        )
                                    )
                            )

                            // Shimmer overlay on progress
                            if (animatedProgress > 0.05f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                ),
                                                startX = shimmerOffset * 500f - 200f,
                                                endX = shimmerOffset * 500f + 200f
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Percentage with animated value
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "v${remoteVersion.versionName}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextLight
                            )
                            Text(
                                text = "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = GradientAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Info text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SubtleBg)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = GradientAccent.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Proses ini tidak akan memakan waktu lama.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLight
                            )
                        }
                    }
                }

                // ─── Phase: Download Finished (Success or Failure) ─────────
                AnimatedVisibility(
                    visible = isFinished,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isDownloadSuccess) "Unduhan Selesai! 🎉" else "Unduhan Gagal ❌",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = if (isDownloadSuccess) TextDark else Color(0xFFE53935)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isDownloadSuccess)
                                "File update siap dipasang.\nMembuka installer..."
                            else
                                "Gagal mengunduh update.\nSilakan coba lagi nanti.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        if (isDownloadSuccess) {
                            // Install button with success gradient
                            Button(
                                onClick = {
                                    AppUpdater.installApk(context)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(SuccessGreen, SuccessGreenDark)
                                            ),
                                            RoundedCornerShape(16.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.InstallMobile,
                                            null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Pasang Sekarang",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        } else {
                            // Retry or Close button
                            Button(
                                onClick = { onDismiss() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SubtleBg
                                )
                            ) {
                                Text(
                                    "Tutup",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(GradientStart, GradientEnd))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextDark.copy(alpha = 0.8f)
        )
    }
}

package com.blackcode.tehatlas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackcode.tehatlas.network.*
import com.blackcode.tehatlas.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.blackcode.tehatlas.utils.AppUpdater
import com.blackcode.tehatlas.ui.UpdateDialog

// ─── Main Login Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val coroutineScope = rememberCoroutineScope()

    val performLogin: () -> Unit = {
        focusManager.clearFocus()
        isLoading = true
        coroutineScope.launch {
            val result = repository.login(username, password)
            isLoading = false
            when (result) {
                is Resource.Success -> onLoginSuccess(result.data.user.role)
                is Resource.Error -> { errorMessage = "Kredensial tidak valid. Silakan coba lagi." }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        val isTablet = maxWidth > 600.dp

        if (isTablet) {
            // ── Tablet: side-by-side layout ──────────────────────────
            Row(modifier = Modifier.fillMaxSize()) {
                // Left branded panel
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(900)) + slideInHorizontally(
                        initialOffsetX = { -it / 2 },
                        animationSpec = tween(900, easing = EaseOutCubic)
                    ),
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                ) {
                    BrandedPanel(isCompact = false)
                }

                // Right login form
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(900, delayMillis = 200)) + slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = tween(900, delayMillis = 200, easing = EaseOutCubic)
                    ),
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoginFormCard(
                            username = username,
                            onUsernameChange = { username = it; errorMessage = null },
                            password = password,
                            onPasswordChange = { password = it; errorMessage = null },
                            passwordVisible = passwordVisible,
                            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                            rememberMe = rememberMe,
                            onRememberMeChange = { rememberMe = it },
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onLogin = performLogin,
                            focusManager = focusManager,
                            maxCardWidth = 440.dp
                        )
                    }
                }
            }
        } else {
            // ── Phone: stacked layout ────────────────────────────────
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(800)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(800, easing = EaseOutCubic)
                ),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Compact branded header
                    BrandedPanel(
                        isCompact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )

                    // Login form overlapping the header slightly
                    LoginFormCard(
                        username = username,
                        onUsernameChange = { username = it; errorMessage = null },
                        password = password,
                        onPasswordChange = { password = it; errorMessage = null },
                        passwordVisible = passwordVisible,
                        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                        rememberMe = rememberMe,
                        onRememberMeChange = { rememberMe = it },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onLogin = performLogin,
                        focusManager = focusManager,
                        modifier = Modifier.offset(y = (-32).dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer
                    val versionName = remember {
                        try {
                            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            packageInfo.versionName ?: "1.0.0"
                        } catch (e: Exception) {
                            "1.0.0"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "TehAtlas v$versionName",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { 
                                coroutineScope.launch {
                                    AppUpdater.checkForUpdate(context, showToastIfUpToDate = true)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Filled.SystemUpdate, null, modifier = Modifier.size(14.dp), tint = Primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Perbarui Aplikasi", style = MaterialTheme.typography.labelMedium, color = Primary)
                        }
                    }
                }
            }
        }
    }
}

// ─── Branded Panel ────────────────────────────────────────────────────────────

@Composable
private fun BrandedPanel(
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circles")
    val circleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.06f, targetValue = 0.14f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "c1"
    )
    val circleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.10f, targetValue = 0.04f,
        animationSpec = infiniteRepeatable(tween(5000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "c2"
    )
    val circleAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.04f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(3500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "c3"
    )

    Box(
        modifier = modifier
            .then(if (!isCompact) Modifier.fillMaxSize() else Modifier)
            .background(
                Brush.linearGradient(
                    colors = listOf(Primary, PrimaryLight, Secondary, Tertiary.copy(alpha = 0.8f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative floating circles
        Box(
            modifier = Modifier
                .size(if (isCompact) 200.dp else 320.dp)
                .offset(x = if (isCompact) 100.dp else 140.dp, y = if (isCompact) (-60).dp else (-100).dp)
                .alpha(circleAlpha1)
                .clip(CircleShape)
                .background(Accent)
        )
        Box(
            modifier = Modifier
                .size(if (isCompact) 160.dp else 240.dp)
                .offset(x = if (isCompact) (-80).dp else (-120).dp, y = if (isCompact) 80.dp else 140.dp)
                .alpha(circleAlpha2)
                .clip(CircleShape)
                .background(Tertiary)
        )
        Box(
            modifier = Modifier
                .size(if (isCompact) 100.dp else 180.dp)
                .offset(x = if (isCompact) 40.dp else 60.dp, y = if (isCompact) 60.dp else 100.dp)
                .alpha(circleAlpha3)
                .clip(CircleShape)
                .background(Secondary.copy(alpha = 0.6f))
        )

        // Content over circles
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(if (isCompact) 72.dp else 96.dp)
                    .clip(RoundedCornerShape(if (isCompact) 20.dp else 28.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 56.dp else 76.dp)
                        .clip(RoundedCornerShape(if (isCompact) 16.dp else 22.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.store_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(if (isCompact) 28.dp else 40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 24.dp))

            Text(
                text = "TehAtlas",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 28.sp else 36.sp,
                    letterSpacing = (-1).sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sistem Manajemen Bisnis",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            if (!isCompact) {
                Spacer(modifier = Modifier.height(32.dp))

                // Feature highlights for tablet
                FeatureRow(icon = Icons.Outlined.Inventory, text = "Gudang & Inventaris")
                Spacer(modifier = Modifier.height(12.dp))
                FeatureRow(icon = Icons.Outlined.PointOfSale, text = "Kasir & Transaksi")
                Spacer(modifier = Modifier.height(12.dp))
                FeatureRow(icon = Icons.Outlined.BarChart, text = "Laporan & Analisis")
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

// ─── Login Form Card ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginFormCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    modifier: Modifier = Modifier,
    maxCardWidth: Dp = Dp.Unspecified
) {
    val cardModifier = modifier
        .then(
            if (maxCardWidth != Dp.Unspecified) Modifier.widthIn(max = maxCardWidth)
            else Modifier.fillMaxWidth()
        )
        .padding(horizontal = 24.dp)

    Card(
        modifier = cardModifier
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Primary.copy(alpha = 0.08f),
                spotColor = Primary.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            // Header
            Text(
                text = "Selamat datang kembali",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Masuk untuk terus mengelola bisnis Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Username field ──
            Text(
                text = "Nama Pengguna",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Secondary,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = Secondary,
                    cursorColor = Secondary,
                    unfocusedContainerColor = SurfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Password field ──
            Text(
                text = "Kata Sandi",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (username.isNotBlank() && password.isNotBlank()) onLogin()
                    }
                ),
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Secondary,
                    unfocusedBorderColor = DividerColor,
                    focusedLabelColor = Secondary,
                    cursorColor = Secondary,
                    unfocusedContainerColor = SurfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Remember me & Forgot password ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = onRememberMeChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Secondary,
                            uncheckedColor = TextTertiary
                        )
                    )
                    Text(
                        "Ingat saya",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                TextButton(onClick = { }) {
                    Text(
                        "Lupa Kata Sandi?",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Accent
                    )
                }
            }

            // ── Error message ──
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Error.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = Error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Gradient Sign In Button ──
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = username.isNotBlank() && password.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if (username.isNotBlank() && password.isNotBlank() && !isLoading)
                                Brush.horizontalGradient(
                                    colors = listOf(Secondary, Tertiary, Accent)
                                )
                            else Brush.horizontalGradient(
                                colors = listOf(
                                    Secondary.copy(alpha = 0.3f),
                                    Tertiary.copy(alpha = 0.3f),
                                    Accent.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Masuk Sekarang",
                                style = MaterialTheme.typography.titleMedium.copy(
                                     fontWeight = FontWeight.SemiBold,
                                     letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = DividerColor
                )
                Text(
                    "Login Aman",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = DividerColor
                )
            }
        }
    }
}

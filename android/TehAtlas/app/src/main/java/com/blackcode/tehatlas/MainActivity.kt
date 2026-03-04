package com.blackcode.tehatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.blackcode.tehatlas.network.RetrofitClient
import com.blackcode.tehatlas.network.SessionManager
import androidx.compose.ui.platform.LocalContext
import com.blackcode.tehatlas.ui.theme.Background
import com.blackcode.tehatlas.ui.theme.TehAtlasTheme
import com.blackcode.tehatlas.utils.AppUpdater

sealed class Screen {
    object Login : Screen()
    object AdminDashboard : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize networking
        val sessionManager = SessionManager.getInstance(this)
        RetrofitClient.init(sessionManager)

        // ── Auto-login: route directly if session is already saved ────────
        if (sessionManager.isLoggedIn()) {
            when (sessionManager.getRole()) {
                "cashier" -> {
                    startActivity(android.content.Intent(this, CashierActivityClass::class.java))
                    finish()
                    return
                }
                "warehouse" -> {
                    startActivity(android.content.Intent(this, WarehouseActivityClass::class.java))
                    finish()
                    return
                }
                // "admin" falls through to setContent below
            }
        }

        setContent {
            TehAtlasTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                // Determine initial screen: admin with saved session goes straight to dashboard
                val initialScreen: Screen = if (sessionManager.isLoggedIn() && sessionManager.getRole() == "admin")
                    Screen.AdminDashboard else Screen.Login

                var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    AppUpdater.checkForUpdate(context)
                }

                when (currentScreen) {
                    Screen.Login -> LoginScreen(
                        onLoginSuccess = { role ->
                            when (role) {
                                "admin" -> currentScreen = Screen.AdminDashboard
                                "warehouse" -> {
                                    startActivity(android.content.Intent(this@MainActivity, WarehouseActivityClass::class.java))
                                    finish()
                                }
                                "cashier" -> {
                                    startActivity(android.content.Intent(this@MainActivity, CashierActivityClass::class.java))
                                    finish()
                                }
                                else -> currentScreen = Screen.Login
                            }
                        }
                    )
                    Screen.AdminDashboard -> AdminDashboard(
                        onLogout = {
                            sessionManager.clearSession()
                            RetrofitClient.refresh()
                            currentScreen = Screen.Login
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                AppUpdater.Component()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    TehAtlasTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
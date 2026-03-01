package com.blackcode.tehatlas.network

import android.content.Context
import android.content.SharedPreferences

/**
 * Stores JWT token and user session data using SharedPreferences.
 */
class SessionManager(val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("teh_atlas_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
        private const val KEY_OUTLET_ID = "outlet_id"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun saveSession(token: String, user: UserDto) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_ROLE, user.role)
            putString(KEY_OUTLET_ID, user.outletId)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun getOutletId(): String? = prefs.getString(KEY_OUTLET_ID, null)

    fun isLoggedIn(): Boolean = getToken() != null

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}

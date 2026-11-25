package com.example.data

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "walx_session_v1",
        Context.MODE_PRIVATE
    )

    fun saveSession(userId: String, rememberMe: Boolean) {
        if (rememberMe) {
            prefs.edit()
                .putString(KEY_ACTIVE_USER_ID, userId)
                .putBoolean(KEY_REMEMBER_ME, true)
                .apply()
        } else {
            prefs.edit()
                .remove(KEY_ACTIVE_USER_ID)
                .putBoolean(KEY_REMEMBER_ME, false)
                .apply()
        }
    }

    fun getRememberedUserId(): String? {
        if (!prefs.getBoolean(KEY_REMEMBER_ME, false)) return null
        return prefs.getString(KEY_ACTIVE_USER_ID, null)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACTIVE_USER_ID)
            .putBoolean(KEY_REMEMBER_ME, false)
            .apply()
    }

    companion object {
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_REMEMBER_ME = "remember_me"
    }
}

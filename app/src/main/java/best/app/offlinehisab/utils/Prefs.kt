package best.app.offlinehisab.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Prefs {
    const val  IS_BACKUP_RESTORE = "isBackupRestore"
    const val  IS_BACKUP_RESTORE_DATE = "isBackupUploadDate"
    const val SAVE_PIN = "savePin"
    private lateinit var prefs: SharedPreferences

    fun initPrefs(context: Context) {
        prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    fun setBooleanValue(value: Boolean, key: String) {
        prefs.edit { putBoolean(key, value) }
    }

    fun setStringValue(value: String?, key: String) {
        prefs.edit { putString(key, value) }
    }

    fun isBooleanValue(key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

    fun isStringValue(key: String): String? {
        return prefs.getString(key, null)
    }

    fun clearKey() {
        prefs.edit {
            remove(SAVE_PIN)
        }
    }
}
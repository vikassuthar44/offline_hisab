package best.app.offlinehisab.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import best.app.offlinehisab.backup.DriveBackupHelper
import best.app.offlinehisab.backup.Repository
import best.app.offlinehisab.di.DB_NAME
import best.app.offlinehisab.ui.state.AuthState
import best.app.offlinehisab.utils.Prefs
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SettingViewModel"

class SettingViewModel() : ViewModel() {

    private val _isBackupRestore = MutableStateFlow(false)
    val isBackupRestore: StateFlow<Boolean> = _isBackupRestore.asStateFlow()
    private val _backupUploadDate = MutableStateFlow<String?>(null)
    val backupUploadDate: StateFlow<String?> = _backupUploadDate.asStateFlow()

    val isLoading = mutableStateOf(false)

    init {
        checkBackUpState()
    }

    fun checkBackUpState() {
        viewModelScope.launch {
            _isBackupRestore.value = Prefs.isBooleanValue(Prefs.IS_BACKUP_RESTORE)
            _backupUploadDate.value = Prefs.isStringValue(Prefs.IS_BACKUP_RESTORE_DATE)
        }
    }

    fun setBackupUploadTime() {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy, hh:mm a", Locale("en", "IN"))
        val formattedDate = dateFormat.format(Date())
        Prefs.setStringValue(
            value = formattedDate,
            key = Prefs.IS_BACKUP_RESTORE_DATE
        )
        _backupUploadDate.value = formattedDate
    }

    fun setBackupRestore(value: Boolean) {
        Prefs.setBooleanValue(
            value = value,
            key = Prefs.IS_BACKUP_RESTORE
        )
        _isBackupRestore.value = value
    }


    fun uploadBackup(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        // upload (backup)
        viewModelScope.launch(Dispatchers.IO) {
            val isUpload = DriveBackupHelper.uploadDatabaseToDrive(
                context = context,
                dbName = DB_NAME
            )
            if (isUpload) {
                setBackupUploadTime()
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun restoreBackup(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        // restore (on first run or button)
        viewModelScope.launch {
            val isUpload = DriveBackupHelper.restoreDatabaseFromDrive(
                context = context,
                dbName = DB_NAME
            )
            if (isUpload) {
                setBackupRestore(true)
                onSuccess()
            } else {
                onFailure()
            }
        }
    }
}
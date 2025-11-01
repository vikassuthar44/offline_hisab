package best.app.offlinehisab.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import best.app.offlinehisab.viewmodel.SettingViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@SuppressLint("ViewModelConstructorInComposable")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenUI(
    navController: NavController,
) {
    val context = LocalContext.current
    val settingViewModel = viewModel<SettingViewModel>()

    val scope = rememberCoroutineScope()

    val isRestored by settingViewModel.isBackupRestore.collectAsState()
    val backupUploadDate by settingViewModel.backupUploadDate.collectAsState()

    val isStoreClick = remember { mutableStateOf(false) }


    // Step 1: Configure sign-in options
    val signInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE)) // Private app folder
            .build()
    }

    // Step 2: Create the client
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, signInOptions)
    }

    // Step 3: Handle sign-in result
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Log.d("TAG", "SettingScreenUI: singed as account ${account.email}")
                if (!isStoreClick.value) {
                    settingViewModel.uploadBackup(
                        context = context,
                        onSuccess = {
                            scope.launch(Dispatchers.Main) {
                                settingViewModel.isLoading.value = false
                                Toast.makeText(
                                    context,
                                    "Backup Upload Success",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onFailure = {
                            scope.launch(Dispatchers.Main) {
                                settingViewModel.isLoading.value = false
                                Log.d("TAG", "SettingScreenUI: upload failed $")
                                Toast.makeText(context, "Backup Upload Failed", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    )
                } else {
                    settingViewModel.restoreBackup(
                        context = context,
                        onSuccess = {
                            scope.launch(Dispatchers.Main) {
                                settingViewModel.isLoading.value = false
                                Toast.makeText(
                                    context,
                                    "Restore Success",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onFailure = {
                            scope.launch(Dispatchers.Main) {
                                settingViewModel.isLoading.value = false
                                Log.d("TAG", "SettingScreenUI: Restore failed $")
                                Toast.makeText(context, "Restore Failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

            }
        } catch (e: ApiException) {
            settingViewModel.isLoading.value = false
            e.printStackTrace()
            Toast.makeText(context, "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }


    LaunchedEffect(Unit) {
        settingViewModel.checkBackUpState()
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigateUp()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "back"
                        )
                    }
                },
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            SingleSettingView(
                title = "Upload Backup",
                description = if (backupUploadDate == null) "Tap here for backup" else "Last backup on $backupUploadDate"
            ) {
                scope.launch {
                    isStoreClick.value = false
                    settingViewModel.isLoading.value = true
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account != null) {
                        settingViewModel.uploadBackup(
                            context = context,
                            onSuccess = {
                                scope.launch(Dispatchers.Main) {
                                    settingViewModel.isLoading.value = false
                                    Toast.makeText(
                                        context,
                                        "Backup Upload Success",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onFailure = {
                                scope.launch(Dispatchers.Main) {
                                    settingViewModel.isLoading.value = false
                                    Log.d("TAG", "SettingScreenUI: upload failed $")
                                    Toast.makeText(
                                        context,
                                        "Backup Upload Failed",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    } else {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            if (!isRestored && backupUploadDate?.isNotBlank() != true) {
                SingleSettingView(
                    title = "Restore Backup",
                    description = "Tap here for backup restore",
                ) {
                    settingViewModel.isLoading.value = true
                    isStoreClick.value = true
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account != null) {
                        settingViewModel.restoreBackup(
                            context = context,
                            onSuccess = {
                                scope.launch(Dispatchers.Main) {
                                    settingViewModel.isLoading.value = false
                                    Toast.makeText(
                                        context,
                                        "Restore Success",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onFailure = {
                                scope.launch(Dispatchers.Main) {
                                    settingViewModel.isLoading.value = false
                                    Log.d("TAG", "SettingScreenUI: Restore failed $")
                                    Toast.makeText(context, "Restore Failed", Toast.LENGTH_LONG)
                                        .show()
                                }
                            }
                        )
                    } else {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent)
                    }
                }
            } else {
                Box(modifier = Modifier)
            }
        }
    }

    if (settingViewModel.isLoading.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {}, contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Box(modifier = Modifier)
    }

}

@Composable
fun SingleSettingView(
    title: String,
    description: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
            .background(
                shape = MaterialTheme.shapes.medium,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .clickable(
                onClick = {
                    onClick()
                }
            )
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "")
        }
    }
}
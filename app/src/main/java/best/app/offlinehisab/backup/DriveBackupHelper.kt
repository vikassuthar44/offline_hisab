package best.app.offlinehisab.backup

import android.content.Context
import android.util.Log
import best.app.offlinehisab.di.AppModule
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object DriveBackupHelper {

    fun createOrGetAppFolder(driveService: Drive): String {
        val folderName = "OfflineHisabBackup"

        // Try to find the folder first
        val existingFolders = driveService.files().list()
            .setQ("name = '$folderName' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .setFields("files(id, name)")
            .execute()
            .files

        if (!existingFolders.isNullOrEmpty()) {
            return existingFolders.first().id
        }

        // Folder not found → create it
        val metadata = com.google.api.services.drive.model.File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
        }

        val folder = driveService.files().create(metadata)
            .setFields("id")
            .execute()

        Log.d("DriveBackup", "Created folder: ${folder.name} (${folder.id})")
        return folder.id
    }


    fun getDriveService(context: Context): Drive {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("User not signed in")

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE) // 👈 Use this instead of DRIVE_APPDATA
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Offline Hisab") // change to your app name
            .build()
    }

    suspend fun uploadDatabaseToDrive(context: Context, dbName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(context)

            // Create or find folder
            val folderId = createOrGetAppFolder(driveService)

            // Close Room to flush WAL
            AppModule.destroyInstance()

            val dbFile = context.getDatabasePath(dbName)
            val walFile = File(dbFile.parent, "$dbName-wal")
            val shmFile = File(dbFile.parent, "$dbName-shm")

            val tempZip = File(context.cacheDir, "offline_hisab_backup.zip")
            FileZipper.zipFiles(listOf(dbFile, walFile, shmFile), tempZip)

            // Upload to visible Drive folder
            val metadata = com.google.api.services.drive.model.File().apply {
                name = tempZip.name
                parents = listOf(folderId) // 👈 goes inside visible folder
            }

            val media = com.google.api.client.http.FileContent("application/zip", tempZip)
            driveService.files().create(metadata, media)
                .setFields("id, parents")
                .execute()

            Log.d("DriveBackup", "Upload successful: ${tempZip.name}")
            tempZip.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }




    /*suspend fun uploadDatabaseToDrive(context: Context, dbName: String): Boolean = withContext(
        Dispatchers.IO) {
        try {
            val driveService = getDriveService(context)
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) return@withContext false

            // Delete old backup if exists
            val existing = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$dbName'")
                .execute()
            existing.files.forEach {
                driveService.files().delete(it.id).execute()
            }

            val metadata = com.google.api.services.drive.model.File().apply {
                name = dbName
                parents = listOf("appDataFolder")
            }

            val mediaContent = FileContent("application/x-sqlite3", dbFile)
            driveService.files().create(metadata, mediaContent)
                .setFields("id")
                .execute()

            Log.d("DriveBackup", "Upload successful!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }*/

    suspend fun restoreDatabaseFromDrive(context: Context, dbName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(context)

            // ✅ Step 1: Find or create visible folder
            val folderId = createOrGetAppFolder(driveService) // same function from upload

            // ✅ Step 2: Find the backup ZIP file inside the visible folder
            val files = driveService.files().list()
                .setQ("name = 'offline_hisab_backup.zip' and '$folderId' in parents and trashed = false")
                .setFields("files(id, name)")
                .execute()
                .files

            if (files.isNullOrEmpty()) {
                Log.e("DriveBackup", "No backup file found in Drive folder")
                return@withContext false
            }

            val fileId = files.first().id
            val zipFile = File(context.cacheDir, "restore.zip")

            // ✅ Step 3: Close Room DB before overwriting
            AppModule.destroyInstance()

            // ✅ Step 4: Download ZIP from Drive
            FileOutputStream(zipFile).use { output ->
                driveService.files().get(fileId).executeMediaAndDownloadTo(output)
            }

            // ✅ Step 5: Unzip into app's databases directory
            val dbDir = File(context.filesDir.parentFile, "databases")
            if (!dbDir.exists()) dbDir.mkdirs()

            FileZipper.unzip(zipFile, dbDir)
            zipFile.delete()

            // ✅ Step 6: Reopen Room DB and Repo after restore
            AppModule.provideDb(context)
            AppModule.provideRepo(context)

            Log.d("DriveBackup", "Restore successful from Drive visible folder!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DriveBackup", "Restore failed: ${e.message}")
            false
        }
    }


    /*suspend fun restoreDatabaseFromDrive(context: Context, dbName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(context)

            val files = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$dbName'")
                .execute()
                .files

            if (files.isNullOrEmpty()) {
                Log.e("DriveBackup", "No backup found")
                return@withContext false
            }

            val fileId = files.first().id
            val outputFile = context.getDatabasePath(dbName)

            // ✅ Step 1: Close & clear Room before replacing
            AppModule.destroyInstance()

            driveService.files().get(fileId).executeMediaAndDownloadTo(FileOutputStream(outputFile))

            // ✅ Step 3: Recreate Room + Repo for immediate usage
            AppModule.provideDb(context)
            AppModule.provideRepo(context)

            Log.d("DriveBackup", "Restore successful!")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }*/
}
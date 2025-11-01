package best.app.offlinehisab

import android.app.Application
import best.app.offlinehisab.utils.Prefs
import com.google.firebase.FirebaseApp

class HisabApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.initPrefs(context = this)
        FirebaseApp.initializeApp(this)
    }
}
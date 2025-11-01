package best.app.offlinehisab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import best.app.offlinehisab.ui.nav.Screen
import best.app.offlinehisab.ui.theme.OfflineHisabTheme

class MainActivity : ComponentActivity() {
    var navController: NavHostController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            navController = rememberNavController()
            OfflineHisabTheme {
                navController?.let {
                    HisabAppScreen(
                        navController = it
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        try {
            navController?.clearBackStack<NavHostController>()
            navController?.navigate(Screen.LockScreen)
        } catch (e: Exception) {
            try {
                navController?.navigate(Screen.LockScreen)
            } catch (e: Exception) {

            }
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OfflineHisabTheme {
        Greeting("Android")
    }
}
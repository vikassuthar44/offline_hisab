package best.app.offlinehisab

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import best.app.offlinehisab.ui.nav.NavGraph

@Composable
fun HisabAppScreen(navController: NavHostController) {
    NavGraph(navController)
}
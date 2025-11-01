package best.app.offlinehisab.ui.nav

import android.os.Build
import androidx.annotation.RequiresApi
import best.app.offlinehisab.ui.screens.AddCustomerScreen
import best.app.offlinehisab.ui.screens.AddTxnScreen
import best.app.offlinehisab.ui.screens.CustomerDetailScreen
import best.app.offlinehisab.ui.screens.HomeScreen
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import best.app.offlinehisab.MainActivity
import best.app.offlinehisab.ui.screens.LockScreen
import best.app.offlinehisab.ui.screens.SettingScreenUI
import best.app.offlinehisab.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {

    @Serializable
    object LockScreen : Screen()

    @Serializable
    object Home : Screen()

    @Serializable
    data class CustomerDetail(
        val customerId: String = "",
    ) : Screen()

    @Serializable
    data class AddCustomer(
        val isUpdate: Boolean = false,
    ) : Screen()

    @Serializable
    data object SettingScreen: Screen()


    @Serializable
    data class AddTxnScreen(
        val customerId: String = "",
        val isCredit: Boolean = false,
        val isUpdate: Boolean = false,
    ) : Screen()
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavGraph(navController: NavHostController) {
    val mainViewModel = viewModel<MainViewModel>()
    NavHost(navController = navController, startDestination = Screen.LockScreen) {
        composable<Screen.LockScreen> {
            LockScreen(
                mainViewModel = mainViewModel,
                navController = navController,
            )
        }
        composable<Screen.Home> {
            HomeScreen(
                navController = navController,
                vm = mainViewModel
            )
        }
        composable<Screen.CustomerDetail> { backStackEntry ->
            val id = backStackEntry.toRoute<Screen.CustomerDetail>().customerId
            CustomerDetailScreen(
                navController = navController,
                vm = mainViewModel,
                customerId = id
            )
        }
        composable<Screen.AddCustomer> {backStackEntry ->
            val isUpdate = backStackEntry.toRoute<Screen.AddTxnScreen>().isUpdate
            AddCustomerScreen(
                navController = navController,
                vm = mainViewModel,
                isUpdate = isUpdate
            )
        }
        composable<Screen.AddTxnScreen> { backStackEntry ->
            val customerId = backStackEntry.toRoute<Screen.AddTxnScreen>().customerId
            val isCredit = backStackEntry.toRoute<Screen.AddTxnScreen>().isCredit
            val isUpdate = backStackEntry.toRoute<Screen.AddTxnScreen>().isUpdate
            AddTxnScreen(
                vm = mainViewModel,
                navController = navController,
                customerId = customerId,
                isCredit = isCredit,
                isUpdate = isUpdate
            )
        }

        composable<Screen.SettingScreen> {backStackEntry ->
            SettingScreenUI(
                navController = navController,
            )
        }
    }
}

package best.app.offlinehisab.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import best.app.offlinehisab.ui.nav.Screen
import best.app.offlinehisab.ui.theme.OfflineHisabTheme
import best.app.offlinehisab.viewmodel.MainViewModel

@Preview
@Composable
fun LockScreenPreview() {
    OfflineHisabTheme {
        LockScreen(
            mainViewModel = viewModel(),
            navController = rememberNavController()
        )
    }
}

@Composable
fun LockScreen(
    mainViewModel: MainViewModel,
    navController: NavController) {
    var passCode by remember { mutableStateOf("") }

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Enter Your Pin",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {

                    PasscodeBoxes(
                        passCode = passCode
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "1"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("1")
                                }
                            }
                        }
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "2"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("2")
                                }
                            }
                        }
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "3"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("3")
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "4"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("4")
                                }
                            }
                        }
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "5"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("5")
                                }
                            }
                        }
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "6"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("6")
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "7"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("7")
                                }
                            }
                        }
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "8"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("8")
                                }
                            }
                        }
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "9"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("9")
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SingleButtonView(
                            modifier = Modifier.weight(1f),
                            value = "0"
                        ) {
                            if (passCode.length < 4) {
                                passCode = buildString {
                                    append(passCode)
                                    append("0")
                                }
                            }
                        }
                        SingleButtonView(
                            modifier = Modifier.weight(2f),
                            value = "Clear",
                            isSubmit = true,
                            style = MaterialTheme.typography.headlineSmall
                        ) {
                            mainViewModel.isClearClick.intValue++
                            passCode = ""
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable {
                            mainViewModel.verifyPin(
                                pin = passCode,
                                onFailure = { errorMsg ->
                                    mainViewModel.resetClearClick()
                                    Toast.makeText(
                                        navController.context,
                                        errorMsg,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onSuccess = {
                                    mainViewModel.resetClearClick()
                                    navController.navigate(Screen.Home) {
                                        popUpTo(Screen.LockScreen) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Submit",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SingleButtonView(
    modifier: Modifier,
    value: String,
    isSubmit: Boolean = false,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
            )
            .border(
                width = 1.dp,
                color = Color.LightGray,
            )
            .clickable {
                onClick()
            }
            .padding(vertical = if (isSubmit) 22.5.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = style.copy(
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
fun PasscodeBoxes(passCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .border(
                        width = 2.dp,
                        color = Color.Gray,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (passCode.getOrNull(index) != null) "*" else "",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                )
            }
        }
    }
}

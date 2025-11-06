package best.app.offlinehisab.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import best.app.offlinehisab.utils.CustomTextField
import best.app.offlinehisab.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    navController: NavController,
    vm: MainViewModel,
    isUpdate: Boolean,
) {
    val customer = vm.selectedCustomer.value
    var name by remember {
        mutableStateOf(
            TextFieldValue(
                (if (isUpdate) customer?.name ?: "" else "")
            )
        )
    }
    var phone by remember {
        mutableStateOf(
            TextFieldValue(
                if (isUpdate) customer?.phone ?: "" else ""
            )
        )
    }
    var note by remember {
        mutableStateOf(
            TextFieldValue(
                if (isUpdate) customer?.note ?: "" else ""
            )
        )
    }
    var showingError by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        navController.navigateUp()
                    }
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "back")
                }
            },
            title = {
                Text(
                    if (isUpdate) "Update Customer" else "Add Customer",
                    style = MaterialTheme.typography.titleLarge
                )
            },
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            CustomTextField(
                value = name,
                onValueChange = { name = it; showingError = false },
                placeholder = "Name",
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                maxCharacter = 40
            )
            Spacer(Modifier.height(8.dp))
            CustomTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "Phone (optional)",
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
                maxCharacter = 13
            )
            Spacer(Modifier.height(8.dp))
            CustomTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = "Note (optional)",
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )

            if (showingError) {
                Spacer(Modifier.height(8.dp))
                Text("Name is required", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(10f),
                horizontalArrangement = Arrangement.spacedBy(space = 20.dp)
            ) {
                Button(
                    onClick = {
                        if (isUpdate) {
                            if (name.text.isBlank()) {
                                showingError = true
                                return@Button
                            }
                            customer?.let {
                                vm.updateCustomer(
                                    customerId = it.id,
                                    name.text.trim(),
                                    phone.text.trim().ifEmpty { null },
                                    note.text.trim().ifEmpty { null })
                            }
                            navController.popBackStack()
                        } else {
                            if (name.text.isBlank()) {
                                showingError = true
                                return@Button
                            }
                            vm.addCustomer(
                                name.text.trim(),
                                phone.text.trim().ifEmpty { null },
                                note.text.trim().ifEmpty { null })
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.weight(8f)
                ) {
                    Text(if (isUpdate) "Update" else "Add Customer")
                }
                if (isUpdate) {
                    Button(
                        onClick = {
                            customer?.let {
                                vm.deleteCustomer(customerId = customer.id.toString())
                                navController.navigateUp()
                                navController.navigateUp()
                            }
                        },
                        modifier = Modifier.weight(8f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("Delete")
                    }
                } else {
                    Box(modifier = Modifier)
                }
            }
        }
    }
}

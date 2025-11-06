package best.app.offlinehisab.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import best.app.offlinehisab.data.db.Txn
import best.app.offlinehisab.data.db.TxnType
import best.app.offlinehisab.ui.theme.OfflineHisabTheme
import best.app.offlinehisab.utils.CustomTextField
import best.app.offlinehisab.viewmodel.MainViewModel
import java.util.Calendar

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddTxnScreenPreview() {
    OfflineHisabTheme {
        AddTxnScreen(
            vm = viewModel(),
            navController = rememberNavController(),
            customerId = "",
            isCredit = true
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTxnScreen(
    vm: MainViewModel,
    navController: NavController,
    customerId: String,
    isCredit: Boolean = false,
    isUpdate: Boolean = false,
) {
    val txn = vm.selectedTxn.value

    var amountStr by remember { mutableStateOf(TextFieldValue(text = (if (isUpdate) txn?.amount else "").toString())) }
    var note by remember {
        mutableStateOf(
            TextFieldValue(
                text = (if (isUpdate) txn?.note ?: "" else "")
            )
        )
    }
    var type by remember {
        mutableStateOf(
            if (isUpdate) txn?.type
                ?: TxnType.CREDIT else if (isCredit) TxnType.CREDIT else TxnType.DEBIT
        )
    }
    var showError by remember { mutableStateOf(false) }

    // ----- Date & Time handling -----
    val initialDateMillis = remember {
        if (isUpdate) txn?.date ?: System.currentTimeMillis()
        else System.currentTimeMillis()
    }
    var dateMillis by remember { mutableLongStateOf(initialDateMillis) }

    // Formatter: dd-MMM-yyyy, hh:mm a (e.g. 02-Nov-2025, 03:42 PM)
    val dateFormatter = remember {
        java.text.SimpleDateFormat("dd-MMM-yyyy, hh:mm a", java.util.Locale.getDefault())
    }
    val dateText = remember(dateMillis) { dateFormatter.format(java.util.Date(dateMillis)) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }

    val suggestions by vm.suggestions.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // DatePickerDialog (allows today & past only)
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                // Prepare a Calendar with selected date (day/month/year)
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Preserve existing time from dateMillis (if any), otherwise use current time
                val old = Calendar.getInstance().apply { timeInMillis = dateMillis }
                cal.set(Calendar.HOUR_OF_DAY, old.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, old.get(Calendar.MINUTE))
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                // Now open TimePickerDialog immediately so user picks time for the selected date
                val now = Calendar.getInstance()
                val timePicker = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        // When time picked, update calendar with chosen time and set dateMillis
                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        dateMillis = cal.timeInMillis
                    },
                    old.get(Calendar.HOUR_OF_DAY), // initial hour (prefer previously selected / saved)
                    old.get(Calendar.MINUTE),      // initial minute
                    false // 12-hour format; set true for 24-hour
                )
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = Calendar.getInstance().timeInMillis
        }
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
                        if (isUpdate) "Update Transaction" else "Add Transaction",
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
            CustomTextField(
                value = amountStr,
                onValueChange = { amountStr = it; showError = false },
                placeholder = "Amount",
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
                maxCharacter = 9
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                CustomTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        vm.onNoteTextChanged(it.text)
                        expanded = it.text.isNotBlank() && suggestions.isNotEmpty()
                    },
                    placeholder = "Note (optional)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }) {
                    suggestions.forEach { s ->
                        DropdownMenuItem(
                            onClick = {
                                note = TextFieldValue(s)
                                expanded = false
                            },
                            text = {
                                Text(s)
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // Date & Time field (read-only) with calendar + clock icons
            Column(
                modifier = Modifier
            ) {
                Text(
                    text = "Date & Time",
                    style = MaterialTheme.typography.titleMedium
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.75f
                            )
                        )
                        .clickable {
                            // 👇 Open DatePicker when user taps anywhere on the field
                            val cal =
                                Calendar.getInstance().apply { timeInMillis = dateMillis }
                            datePickerDialog.updateDate(
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            )
                            datePickerDialog.show()
                        }
                        .padding(
                            vertical = 15.dp
                        )
                        .padding(
                            start = 12.dp
                        )
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("Type", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Row {
                Row(
                    modifier = Modifier.clickable(
                        onClick = {
                            if (!isUpdate) {
                                type = TxnType.CREDIT
                            }
                        }
                    ),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = type == TxnType.DEBIT,
                        onCheckedChange = {
                            if (!isUpdate) {
                                type = TxnType.CREDIT
                            }
                        })
                    Spacer(Modifier.width(6.dp))
                    Text("You Paid")
                }
                Spacer(Modifier.width(16.dp))
                Row(
                    modifier = Modifier.clickable(
                        onClick = {
                            if (!isUpdate) {
                                type = TxnType.CREDIT
                            }
                        }
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = type == TxnType.CREDIT,
                        onCheckedChange = {
                            if (!isUpdate) {
                                type = TxnType.DEBIT
                            }
                        })
                    Spacer(Modifier.width(6.dp))
                    Text("You Received")
                }
            }

            if (showError) {
                Spacer(Modifier.height(8.dp))
                Text("Please enter a valid amount (> 0)", color = MaterialTheme.colorScheme.error)
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
                        val amt = amountStr.text.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            showError = true
                            return@Button
                        }
                        if (isUpdate) {
                            txn?.txnId?.let {
                                vm.updateTxn(
                                    customerId,
                                    txnId = it,
                                    amt,
                                    type,
                                    note.text.trim().ifEmpty { null },
                                    dateMillis = dateMillis
                                )
                            }
                        } else {
                            vm.addTxn(
                                customerId,
                                amt,
                                type,
                                note.text.trim().ifEmpty { null },
                                dateMillis = dateMillis
                            )
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(8f)
                ) {
                    Text(
                        if (isUpdate) "Update Transaction" else "Add Transaction"
                    )
                }
                if (isUpdate) {
                    Button(
                        onClick = {
                            txn?.let {
                                vm.deleteTxn(
                                    customerId,
                                    Txn(
                                        id = it.txnId,
                                        customerId = it.customerId,
                                        amount = it.amount,
                                        type = it.type,
                                    )
                                )
                                navController.popBackStack()
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



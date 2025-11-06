package best.app.offlinehisab.ui.screens

import FilterPopupModern
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import best.app.offlinehisab.R
import best.app.offlinehisab.data.db.Customer
import best.app.offlinehisab.data.db.TxnType
import best.app.offlinehisab.ui.nav.Screen
import best.app.offlinehisab.utils.generateCustomerPdfAndSaveModernUpdated
import best.app.offlinehisab.utils.sharePdfUri
import best.app.offlinehisab.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    navController: NavController,
    vm: MainViewModel,
    customerId: String,
) {
    val context = LocalContext.current

    // collect flows (live updates)
    val totals by vm.totalsForCustomerFlow(customerId).collectAsState(initial = Pair(0.0, 0.0))
    val txns by vm.txnsForCustomerFlow(customerId).collectAsState(initial = emptyList())

    // one-off fetch for name (suspend)
    var customerName by remember { mutableStateOf("Customer") }
    var customerMob: String? by remember { mutableStateOf("") }
    var customerData by remember { mutableStateOf<Customer?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(customerId) {
        scope.launch {
            vm.getCustomerById(customerId)?.let { customer ->
                customerData = customer
                customerName = customer.name
                customerMob = customer.phone
            }
        }
    }

    val (credit, debit) = totals
    val balance = credit - debit

    val balanceColorTarget = when {
        balance > 0.0 -> Color(0xFF2E7D32) // green = You will Pay
        balance < 0.0 -> Color(0xFFC62828) // red = You will Receive
        else -> Color.Gray
    }
    val balanceColor by animateColorAsState(
        targetValue = balanceColorTarget,
        animationSpec = tween(420)
    )

    val showDialog = remember { mutableStateOf(false) }
    val isShare = remember { mutableStateOf(false) }
    val selectedFilter by vm.selectedFilter.collectAsState()
    val filterTxns by vm.filteredTxns.collectAsState()

    if (showDialog.value) {
        FilterPopupModern(
            isShare = isShare.value,
            show = showDialog.value,
            currentSelection = selectedFilter,
            onSelect = { filter ->
                vm.selectFilter(filter)
                showDialog.value = false
            },
            onDismiss = { showDialog.value = false },
            onDownload = { filterType, isShare ->
                showDialog.value = false
                vm.selectFilter(
                    filter = filterType
                )
                if (isShare) {
                    scope.launch(Dispatchers.IO) {
                        vm.getCustomerById(
                            id = customerId
                        )?.let {
                            var previousAmount: Double? = vm.previousAmount(
                                filter = filterType
                            )
                            var recentAmount: Double? = vm.recentAmount(
                                filter = filterType
                            )
                            if (previousAmount == 0.0) {
                                previousAmount = null
                            }
                            if (recentAmount == 0.0) {
                                recentAmount = null
                            }
                            val uri = generateCustomerPdfAndSaveModernUpdated(
                                context = context,
                                customer = it,
                                txns = filterTxns,
                                filterOptionType = filterType,
                                recentBalance = recentAmount,
                                previousBalance = previousAmount,
                                totals = totals,
                            )
                            uri?.let { pdfUri ->
                                sharePdfUri(
                                    context = context,
                                    pdfUri = pdfUri
                                )
                            }
                        }
                    }
                } else {
                    scope.launch(Dispatchers.IO) {
                        vm.getCustomerById(
                            id = customerId
                        )?.let {
                            var previousAmount: Double? = vm.previousAmount(
                                filter = filterType
                            )
                            var recentAmount: Double? = vm.recentAmount(
                                filter = filterType
                            )
                            if (previousAmount == 0.0) {
                                previousAmount = null
                            }
                            if (recentAmount == 0.0) {
                                recentAmount = null
                            }
                            generateCustomerPdfAndSaveModernUpdated(
                                context = context,
                                customer = it,
                                txns = filterTxns,
                                previousBalance = previousAmount,
                                recentBalance = recentAmount,
                                filterOptionType = filterType,
                                totals = totals,
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "PDF saved to Documents/Hisab",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
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
                        text = customerName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    Row {
                        IconButton(
                            onClick = {
                                isShare.value = false
                                showDialog.value = true
                            }) {
                            Image(
                                painter = painterResource(R.drawable.pdf_icon),
                                contentDescription = "Export PDF"
                            )
                        }
                        IconButton(onClick = {
                            isShare.value = true
                            showDialog.value = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "share pdf"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Row {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(
                            Screen.AddTxnScreen(
                                customerId = customerId,
                                isCredit = false,
                                isUpdate = false
                            )
                        )
                    },
                    icon = { },
                    text = {
                        Text(
                            text = "You Paid",
                            color = Color.White
                        )
                    },
                    containerColor = Color(0xFFC62828),
                    shape = RoundedCornerShape(12.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(
                            Screen.AddTxnScreen(
                                customerId = customerId,
                                isCredit = true,
                                isUpdate = false
                            )
                        )
                    },
                    icon = { },
                    text = {
                        Text(
                            text = "You Received",
                            color = Color.White
                        )
                    },
                    containerColor = Color(0xFF2E7D32),
                    shape = RoundedCornerShape(12.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                )
            }

        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Summary card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            vm.selectedCustomer.value = customerData
                            navController.navigate(
                                Screen.AddCustomer(
                                    isUpdate = true
                                )
                            )
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Name: ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = customerName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mob No: ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = customerMob.toString(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Normal
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Total You Paid",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFC62828)
                                    )
                                )
                                Text(
                                    "₹${"%.2f".format(debit)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color(0xFFC62828)
                                    )
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Total You Received",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF2E7D32)
                                    ),
                                )
                                Text(
                                    "₹${"%.2f".format(credit)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color(0xFF2E7D32)
                                    )
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (balance >= 0) "You will Pay" else "You will Receive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "₹${"%.2f".format(balance).replace("+", "").replace("-", "")}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = balanceColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transactions header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transactions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${txns.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Transaction list
                if (txns.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier.padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transactions yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = Color.LightGray)
                                .padding(horizontal = 5.dp, vertical = 5.dp)
                        ) {
                            Text(
                                modifier = Modifier.weight(2f),
                                text = "Date",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textAlign = TextAlign.Start
                                )
                            )
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Received",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF2E7D32),
                                    textAlign = TextAlign.End
                                )
                            )
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Paid",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFFC62828),
                                    textAlign = TextAlign.End
                                )
                            )
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "Balance",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    textAlign = TextAlign.End
                                )
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(txns) { t ->
                                TransactionRow(
                                    txn = t,
                                    onClick = {
                                        vm.selectedTxn.value = it
                                        navController.navigate(
                                            Screen.AddTxnScreen(
                                                customerId = customerId,
                                                isCredit = false,
                                                isUpdate = true
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun TransactionRow(
    txn: Transaction,
    onClick: (Transaction) -> Unit,
) {
    val isCredit = txn.type == TxnType.CREDIT
    val icon = if (isCredit) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp
    val amountColor = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
    val date = remember(txn.date) {
        val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        fmt.format(Date(txn.date))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            )
            .clickable {
                onClick(txn)
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    modifier = Modifier.weight(2f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (isCredit) {
                        "+" + "₹${"%.2f".format(txn.amount)}"
                    } else "",
                    style = MaterialTheme.typography.titleSmall.copy(
                        textAlign = TextAlign.End
                    ),
                    color = amountColor
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (!isCredit) {
                        "-" + "₹${"%.2f".format(txn.amount)}"
                    } else "",
                    style = MaterialTheme.typography.titleSmall.copy(
                        textAlign = TextAlign.End
                    ),
                    color = amountColor
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = "₹${"%.2f".format(txn.remainingBalance)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        textAlign = TextAlign.End,
                        color = if (txn.remainingBalance >= 0) {
                            Color(0xFF2E7D32)
                        } else {
                            Color(0xFFC62828)
                        }
                    ),
                )
                /*Column(modifier = Modifier.weight(1f)) {
                    Text(
                        txn.note ?: if (isCredit) "You Received" else "You Paid",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }*/

                /*Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        (if (isCredit) "+" else "-") + "₹${"%.2f".format(txn.amount)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isCredit) "You Received" else "You Paid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }*/
            }
            Text(
                txn.note ?: if (isCredit) "You Received" else "You Paid",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

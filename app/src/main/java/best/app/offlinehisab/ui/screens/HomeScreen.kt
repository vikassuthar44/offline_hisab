package best.app.offlinehisab.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import best.app.offlinehisab.data.db.TxnType
import best.app.offlinehisab.ui.nav.Screen
import best.app.offlinehisab.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    vm: MainViewModel
) {
    val customers by vm.customers.collectAsState()
    val totals by vm.globalTotals.collectAsState()

    val (credit, debit) = totals
    val balance = credit - debit

    val balanceTargetColor = when {
        balance > 0.0 -> Color(0xFF2E7D32) // green = customer owes You
        balance < 0.0 -> Color(0xFFC62828) // red = You owe customer
        else -> Color.Gray
    }
    val animatedBalanceColor by animateColorAsState(
        targetValue = balanceTargetColor,
        animationSpec = tween(durationMillis = 420)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hisab", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                actions = {
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.SettingScreen)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Setting")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddCustomer(isUpdate = false)) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add customer") },
                text = { Text("Add customer") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {

                    // Top summary card
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
                                .padding(18.dp)
                        ) {
                            Column {
                                Text(
                                    "Overview",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
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
                                                Color(0xFFC62828)
                                            )
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "Total You Received",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF2E7D32)
                                            )
                                        )
                                        Text(
                                            "₹${"%.2f".format(credit)}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = Color(0xFF2E7D32)
                                            )
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (balance >= 0) "You will Pay" else "You will Receive",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "₹${
                                                "%.2f".format(balance).replace("+", "")
                                                    .replace("-", "")
                                            }",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = animatedBalanceColor
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "${customers.size} customers",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Section header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recent customers", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Pending & latest transaction",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))


                    Column(modifier = Modifier.fillMaxWidth()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(customers.sortedByDescending {
                                it.update
                            }) { c ->
                                RecentCustomerRow(
                                    customer = c,
                                    onClick = {
                                        navController.navigate(
                                            Screen.CustomerDetail(
                                                customerId = c.id.toString()
                                            )
                                        )
                                    },
                                    vm = vm
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
private fun RecentCustomerRow(
    customer: best.app.offlinehisab.data.db.Customer,
    onClick: () -> Unit,
    vm: MainViewModel,
) {
    // collect totals as State
    val totalsState by vm.totalsForCustomerFlow(customer.id.toString())
        .collectAsState(initial = Pair(0.0, 0.0))
    val txnsState by vm.latestTxnForCustomerFlow(customer.id.toString())
        .collectAsState(initial = null)

    val (credit, debit) = totalsState
    val pending = credit - debit

    val pendingColor = when {
        pending > 0.0 -> Color(0xFF2E7D32)
        pending < 0.0 -> Color(0xFFC62828)
        else -> Color.Gray
    }
    val animatedPendingColor by animateColorAsState(
        targetValue = pendingColor,
        animationSpec = tween(durationMillis = 360)
    )

    var latestTransColor by remember { mutableStateOf(Color(0xFF2E7D32)) }
    val latestLine = txnsState?.let { t ->
        latestTransColor = if (t.type == TxnType.CREDIT) Color(0xFF2E7D32) else Color(0xFFC62828)
        "₹${"%.2f".format(t.amount)} • ${t.note ?: if (t.type == TxnType.CREDIT) "You Received" else "You Paid"}"
    } ?: "No transactions"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            .border(
                width = 1.dp,
                color = pendingColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {

            // avatar / initials
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    customer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    latestLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = latestTransColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${"%.2f".format(pending).replace("+", "").replace("-", "")}",
                    style = MaterialTheme.typography.titleMedium,
                    color = animatedPendingColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (pending >= 0) "You will Pay" else "You will Receive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

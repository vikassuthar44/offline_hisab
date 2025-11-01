// FilterPopupModern.kt
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import best.app.offlinehisab.utils.FilterOption
import best.app.offlinehisab.utils.FilterOption.Companion.getFilterOptionTypeDescription
import best.app.offlinehisab.utils.FilterOptionType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterPopupModern(
    isShare: Boolean = false,
    show: Boolean,
    currentSelection: FilterOptionType,
    descriptionText: String = "Choose date range to filter transactions for PDF download.",
    onSelect: (FilterOptionType) -> Unit,
    onDismiss: () -> Unit,
    onDownload: (FilterOptionType, Boolean) -> Unit,
) {
    if (!show) return

    val options = FilterOption.getAllFilterOption()
    var selected by remember { mutableStateOf(currentSelection) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    Dialog(
        onDismissRequest =
            {
                onDismiss()
            }) {
        Box(

            modifier = Modifier
                .background(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .animateContentSize(animationSpec = tween(durationMillis = 220))
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Export Transactions",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Filter & download PDF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown (Exposed style)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selected.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand"
                            )
                        },
                        label = { Text("Select range") },
                        modifier = Modifier
                            .menuAnchor()
                            .background(
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            .clip(RoundedCornerShape(10.dp))
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier

                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option.displayName)
                                        // optional helper line for each option (e.g. actual range)
                                        val helper = getFilterOptionTypeDescription(option)
                                        Text(
                                            helper,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selected = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Buttons row: Cancel / Apply / Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Download is emphasized
                    Button(
                        onClick = {
                            // Ensure latest selection is applied before download
                            onSelect(selected)
                            // small coroutine for UX if needed
                            scope.launch {
                                // immediate callback to download — actual work should be in ViewModel
                                onDownload(selected, isShare)
                            }
                            onDismiss()
                        },
                    ) {
                        Text(if (isShare) " Share as PDF " else " Download PDF ")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

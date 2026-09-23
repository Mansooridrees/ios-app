package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.InvoiceWithItems
import com.example.ui.AppTab
import com.example.ui.InvoiceViewModel
import com.example.ui.StatusFilter
import com.example.ui.theme.AmberPending
import com.example.ui.theme.AmberPendingBg
import com.example.ui.theme.EmeraldPaid
import com.example.ui.theme.EmeraldPaidBg
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoseOverdue
import com.example.ui.theme.RoseOverdueBg
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics by viewModel.ledgerMetrics.collectAsStateWithLifecycle()
    val invoices by viewModel.filteredInvoices.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var paymentDialogInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var deleteConfirmInvoice by remember { mutableStateOf<InvoiceWithItems?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header & Total Metrics
            item {
                DashboardHeader(
                    totalBilled = metrics.totalBilled,
                    totalCollected = metrics.totalCollected,
                    totalPending = metrics.totalPending,
                    invoiceCount = metrics.totalCount,
                    onDeleteAll = if (metrics.totalCount > 0) { { showDeleteAllDialog = true } } else null,
                    isSyncing = isSyncing,
                    onSync = { viewModel.syncAllWithLiveBackend() }
                )
            }

            // Search Bar & Filter Chips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search by client name or invoice #...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search icon")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StatusFilter.values()) { filter ->
                            val selected = statusFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setStatusFilter(filter) },
                                label = {
                                    Text(
                                        when (filter) {
                                            StatusFilter.ALL -> "All (${metrics.totalCount})"
                                            StatusFilter.PENDING -> "Pending"
                                            StatusFilter.PAID -> "Paid"
                                            StatusFilter.OVERDUE -> "Overdue"
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            // Invoices List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invoice Ledger (${invoices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (invoices.isNotEmpty()) {
                            TextButton(
                                onClick = { showDeleteAllDialog = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = RoseOverdue),
                                modifier = Modifier.testTag("dashboard_delete_all_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete All")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        TextButton(
                            onClick = { viewModel.setTab(AppTab.IMPORT) },
                            modifier = Modifier.testTag("import_new_invoice_text_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Bill")
                        }
                    }
                }
            }

            // Empty State
            if (invoices.isEmpty()) {
                item {
                    EmptyLedgerView(
                        searchQuery = searchQuery,
                        onImportClick = { viewModel.setTab(AppTab.IMPORT) }
                    )
                }
            } else {
                items(invoices, key = { it.invoice.id }) { item ->
                    InvoiceLedgerCard(
                        invoiceWithItems = item,
                        onViewPdf = { viewModel.viewInvoicePdf(context, item) },
                        onRecordPayment = { paymentDialogInvoice = item },
                        onDelete = { deleteConfirmInvoice = item }
                    )
                }
            }
        }

        // Floating Action Button: New Bill
        FloatingActionButton(
            onClick = { viewModel.setTab(AppTab.IMPORT) },
            containerColor = PrimaryBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("dashboard_fab_import")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Bill")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Bill", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Payment Dialog
    paymentDialogInvoice?.let { inv ->
        RecordPaymentDialog(
            invoiceWithItems = inv,
            onDismiss = { paymentDialogInvoice = null },
            onConfirm = { amount ->
                viewModel.updatePaymentAmount(inv.invoice.id, amount, inv.invoice.grandTotal)
                paymentDialogInvoice = null
            },
            onMarkFull = {
                viewModel.markInvoiceAsPaid(inv.invoice.id, inv.invoice.grandTotal)
                paymentDialogInvoice = null
            }
        )
    }

    // Delete Confirm Dialog
    deleteConfirmInvoice?.let { inv ->
        AlertDialog(
            onDismissRequest = { deleteConfirmInvoice = null },
            title = { Text("Delete Invoice?") },
            text = { Text("Are you sure you want to remove invoice #${inv.invoice.invoiceNumber} for ${inv.invoice.clientName} from the ledger?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInvoice(inv.invoice.id)
                        deleteConfirmInvoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseOverdue)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmInvoice = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Invoices?") },
            text = { Text("Are you sure you want to delete all invoices and receipts from the ledger? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllInvoices()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseOverdue),
                    modifier = Modifier.testTag("confirm_delete_all_dialog_button")
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DashboardHeader(
    totalBilled: Double,
    totalCollected: Double,
    totalPending: Double,
    invoiceCount: Int,
    onDeleteAll: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    onSync: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Navy900),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FINANCIAL LEDGER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Billing Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onSync != null) {
                        IconButton(
                            onClick = onSync,
                            enabled = !isSyncing,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("header_sync_live_button")
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Live Sync with Backend",
                                tint = if (isSyncing) Color(0xFF94A3B8) else Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B)
                    ) {
                        Text(
                            text = "$invoiceCount Invoices",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (onDeleteAll != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = onDeleteAll,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("header_delete_all_icon_button")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete All Invoices",
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3 Summary Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    title = "Total Billed",
                    amount = totalBilled,
                    icon = Icons.Default.AttachMoney,
                    color = Color.White
                )

                MetricColumn(
                    title = "Collected",
                    amount = totalCollected,
                    icon = Icons.Filled.CheckCircle,
                    color = EmeraldPaid
                )

                MetricColumn(
                    title = "Balance Due",
                    amount = totalPending,
                    icon = Icons.Outlined.Pending,
                    color = AmberPending
                )
            }
        }
    }
}

@Composable
fun MetricColumn(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "$%,.2f", amount),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun InvoiceLedgerCard(
    invoiceWithItems: InvoiceWithItems,
    onViewPdf: () -> Unit,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    val inv = invoiceWithItems.invoice
    val isPaid = inv.status == "PAID"
    val isOverdue = inv.status == "OVERDUE"

    val statusColor = when {
        isPaid -> EmeraldPaid
        isOverdue -> RoseOverdue
        else -> AmberPending
    }
    val statusBg = when {
        isPaid -> EmeraldPaidBg
        isOverdue -> RoseOverdueBg
        else -> AmberPendingBg
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onViewPdf() }
            .testTag("invoice_card_${inv.invoiceNumber}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Invoice # & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = inv.invoiceNumber,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Date: ${inv.issueDate}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg
                ) {
                    Text(
                        text = inv.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Client Name & Items count
            Text(
                text = inv.clientName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${invoiceWithItems.items.size} line items • ${invoiceWithItems.itemsSummary()}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Summary Banner
            val currPrefix = when {
                inv.currency == "$" -> "$"
                inv.currency.equals("USD", ignoreCase = true) -> "$"
                inv.currency.isBlank() -> "PKR "
                else -> "${inv.currency} "
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format(Locale.US, "$currPrefix%,.2f", inv.grandTotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Balance Due", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format(Locale.US, "$currPrefix%,.2f", inv.balanceDue),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (inv.balanceDue > 0) AmberPending else EmeraldPaid
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_invoice_${inv.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Invoice",
                        tint = Color(0xFF94A3B8)
                    )
                }

                if (!isPaid) {
                    OutlinedButton(
                        onClick = onRecordPayment,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("pay_invoice_${inv.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Record Pay", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onViewPdf,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("view_pdf_${inv.id}")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View PDF", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun InvoiceWithItems.itemsSummary(): String {
    return items.joinToString(", ") { it.itemName }
}

@Composable
fun EmptyLedgerView(
    searchQuery: String,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isBlank()) "No Invoices in Ledger" else "No matching invoices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (searchQuery.isBlank())
                "Create a new bill by entering customer name, items, quantity, and rate."
            else
                "Try clearing your search query or adjusting the status filter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (searchQuery.isBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onImportClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Bill")
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    invoiceWithItems: InvoiceWithItems,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    onMarkFull: () -> Unit
) {
    val inv = invoiceWithItems.invoice
    var enteredAmount by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", inv.balanceDue))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for #${inv.invoiceNumber}") },
        text = {
            Column {
                Text(
                    text = "Client: ${inv.clientName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total: $${String.format(Locale.US, "%.2f", inv.grandTotal)}  •  Current Balance: $${String.format(Locale.US, "%.2f", inv.balanceDue)}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = enteredAmount,
                    onValueChange = { enteredAmount = it },
                    label = { Text("Amount Paid ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("record_payment_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onMarkFull,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Mark Fully Paid ($${String.format(Locale.US, "%.2f", inv.grandTotal)})")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = enteredAmount.toDoubleOrNull() ?: 0.0
                    onConfirm(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

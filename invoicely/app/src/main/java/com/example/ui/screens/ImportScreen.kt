package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import com.example.data.parser.BillTextParser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvoiceDraftItem
import com.example.ui.InvoiceViewModel
import com.example.ui.theme.EmeraldPaid
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import java.util.Locale
import java.util.UUID

data class BillItemState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "1",
    val rate: String = ""
) {
    val total: Double
        get() {
            val qty = quantity.toDoubleOrNull() ?: 0.0
            val price = rate.toDoubleOrNull() ?: 0.0
            return Math.round((qty * price) * 100.0) / 100.0
        }
}

@Composable
fun ImportScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var customerName by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var selectedLayout by remember { mutableStateOf("Master Tools") }
    var showOcrScanDialog by remember { mutableStateOf(false) }
    var rawOcrText by remember { mutableStateOf("") }
    var ocrStatusMessage by remember { mutableStateOf<String?>(null) }
    val items = remember {
        mutableStateListOf(
            BillItemState(name = "", quantity = "1", rate = "")
        )
    }

    val grandTotal by remember {
        derivedStateOf {
            items.sumOf { it.total }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_bill_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEW BILL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Create Sales Bill",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter Customer Name, Items, Quantity, and Rate to generate the bill immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.size(52.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_master_tech_logo),
                                contentDescription = "Master Tech Logo",
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bill Layout Selection Card (Layout 1 / Layout 2)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_layout_selection_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bill Layout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose bill print layout style",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedLayout == "Master Tools",
                            onClick = { selectedLayout = "Master Tools" },
                            label = {
                                Text(
                                    text = "Master Tools",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedLayout == "Master Tools") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_layout_master_tools")
                        )

                        FilterChip(
                            selected = selectedLayout == "XP Computers" || selectedLayout == "Layout 1",
                            onClick = { selectedLayout = "XP Computers" },
                            label = {
                                Text(
                                    text = "XP Computers",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedLayout == "XP Computers" || selectedLayout == "Layout 1") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_layout_1")
                        )

                        FilterChip(
                            selected = selectedLayout == "Master Tech" || selectedLayout == "Layout 2",
                            onClick = { selectedLayout = "Master Tech" },
                            label = {
                                Text(
                                    text = "Master Tech",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedLayout == "Master Tech" || selectedLayout == "Layout 2") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_layout_2")
                        )

                        FilterChip(
                            selected = selectedLayout == "Four" || selectedLayout == "Layout 4",
                            onClick = { selectedLayout = "Four" },
                            label = {
                                Text(
                                    text = "Four",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedLayout == "Four" || selectedLayout == "Layout 4") FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(0.7f)
                                .testTag("chip_layout_4")
                        )
                    }
                }
            }
        }

        // OCR Bill Scanner Card (Tesseract / Text OCR Parser)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ocr_scan_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPaid.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DocumentScanner,
                                    contentDescription = "OCR Scan Bill",
                                    tint = EmeraldPaid,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Scan Bill / OCR Auto-Fill",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Paste OCR text or scan bill image to auto-populate",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showOcrScanDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPaid),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("open_ocr_scan_button")
                    ) {
                        Text("Scan / Parse", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 1. Customer Details Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_info_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Customer Name",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enter client name or leave empty for Cash",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer / Client Name") },
                        placeholder = { Text("Cash / Muhammad Ali / Walk-in") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_customer_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = { Text("Bill / Receipt # (Optional)") },
                        placeholder = { Text("Auto-generated if left empty") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_invoice_number_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // Section Title: Items
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bill Items (${items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedButton(
                    onClick = {
                        items.add(BillItemState(name = "", quantity = "1", rate = ""))
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_item_top_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item", fontSize = 13.sp)
                }
            }
        }

        // 2. Items List Cards
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_item_card_$index"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Item #${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (items.size > 1) {
                            IconButton(
                                onClick = { items.removeAt(index) },
                                modifier = Modifier.size(28.dp).testTag("delete_item_button_$index")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Item",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Item Name
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { newName ->
                            items[index] = item.copy(name = newName)
                        },
                        label = { Text("Item Name") },
                        placeholder = { Text("e.g. Tenda Router AC6 / LCD Monitor") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("item_name_input_$index")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Row for Quantity & Rate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = item.quantity,
                            onValueChange = { newQty ->
                                items[index] = item.copy(quantity = newQty)
                            },
                            label = { Text("Quantity") },
                            placeholder = { Text("1") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_qty_input_$index")
                        )

                        OutlinedTextField(
                            value = item.rate,
                            onValueChange = { newRate ->
                                items[index] = item.copy(rate = newRate)
                            },
                            label = { Text("Rate / Price") },
                            placeholder = { Text("e.g. 7000") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("item_rate_input_$index")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val activeCurrency = if (selectedLayout == "Master Tools") "$" else "PKR"

                    // Real-time Item Total display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Item Total: $activeCurrency ${String.format(Locale.US, "%,.2f", item.total)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }

        // Add Another Item Button
        item {
            Button(
                onClick = {
                    items.add(BillItemState(name = "", quantity = "1", rate = ""))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_another_item_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Add Another Item", fontWeight = FontWeight.SemiBold)
            }
        }

        // 3. Bill Summary & Total
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_total_summary_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Items:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${items.size} item(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val activeCurrency = if (selectedLayout == "Master Tools") "$" else "PKR"
                        Text(
                            text = "$activeCurrency ${String.format(Locale.US, "%,.2f", grandTotal)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPaid
                        )
                    }
                }
            }
        }

        // 4. Primary Button: "Generate Bill"
        item {
            Button(
                onClick = {
                    val invoiceItems = items.map { item ->
                        val qty = item.quantity.toDoubleOrNull() ?: 1.0
                        val price = item.rate.toDoubleOrNull() ?: 0.0
                        val name = item.name.trim().ifBlank { "Item" }
                        InvoiceDraftItem(
                            name = name,
                            quantity = if (qty > 0) qty else 1.0,
                            unitPrice = if (price >= 0) price else 0.0
                        )
                    }
                    viewModel.createDirectBill(
                        customerName = customerName,
                        items = invoiceItems,
                        invoiceNumber = invoiceNumber,
                        layoutType = when (selectedLayout) {
                            "Master Tools" -> "MASTER_TOOLS"
                            "Four", "Layout 4" -> "LAYOUT_4"
                            "Master Tech", "Layout 2" -> "LAYOUT_2"
                            else -> "LAYOUT_1"
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_bill_button")
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Bill",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showOcrScanDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showOcrScanDialog = false
                ocrStatusMessage = null
            },
            icon = {
                Icon(
                    Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = EmeraldPaid,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "OCR Bill Scanner / Text Parser",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Paste raw OCR text (from Tesseract or camera scan). The system will automatically extract Date, Total, Items, Qty, and Rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = rawOcrText,
                        onValueChange = { rawOcrText = it },
                        placeholder = {
                            Text(
                                "Master Tech.\nDate: 07-Feb-2026\nSold To: Cash\nNo: 27248\n\nTenda Router AC6 1 7000.00 7000.00\nTotal PKR: 7000.00",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("ocr_raw_text_field"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (ocrStatusMessage != null) {
                        Text(
                            text = ocrStatusMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Pre-fill sample OCR button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                rawOcrText = "Master Tech.\nDate: 07-Feb-2026\nNo: 27248\nSold To: Cash\n\nDiscription Qty Rate Total\nTenda Router AC6 (Dual Band) 1 7000.00 7000.00\nTotal PKR 7000.00"
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Load Sample Receipt", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawOcrText.isNotBlank()) {
                            val parsed = BillTextParser.parseBillText(rawOcrText)
                            if (parsed.clientName.isNotBlank() && parsed.clientName != "Cash") {
                                customerName = parsed.clientName
                            }
                            if (parsed.invoiceNumber.isNotBlank()) {
                                invoiceNumber = parsed.invoiceNumber
                            }
                            if (parsed.vendorName.contains("Master Tech", ignoreCase = true)) {
                                selectedLayout = "Four"
                            } else if (parsed.vendorName.contains("XP Computer", ignoreCase = true)) {
                                selectedLayout = "XP Computers"
                            }

                            if (parsed.items.isNotEmpty()) {
                                items.clear()
                                parsed.items.forEach { item ->
                                    items.add(
                                        BillItemState(
                                            name = item.name,
                                            quantity = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                                            rate = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()
                                        )
                                    )
                                }
                            }
                            showOcrScanDialog = false
                            rawOcrText = ""
                            ocrStatusMessage = null
                        } else {
                            ocrStatusMessage = "कृपया OCR टेक्स्ट पेस्ट करें।"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPaid),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("apply_ocr_button")
                ) {
                    Text("Auto-Fill Bill Form", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showOcrScanDialog = false
                        ocrStatusMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

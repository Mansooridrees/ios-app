package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InvoiceDraft
import com.example.data.model.InvoiceDraftItem
import com.example.ui.AppTab
import com.example.ui.InvoiceViewModel
import com.example.ui.theme.AmberPending
import com.example.ui.theme.EmeraldPaid
import com.example.ui.theme.Navy100
import com.example.ui.theme.Navy900
import com.example.ui.theme.PrimaryBlue
import java.util.Locale

@Composable
fun InvoicePreviewEditScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val draft by viewModel.currentDraft.collectAsStateWithLifecycle()

    var showAddItemDialog by remember { mutableStateOf(false) }

    if (draft == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No Invoice Loaded",
                    style = MaterialTheme.typography.titleMedium,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.setTab(AppTab.IMPORT) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Go to Import")
                }
            }
        }
        return
    }

    val currentDraft = draft!!

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.setTab(AppTab.IMPORT) },
                            modifier = Modifier.testTag("preview_back_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Navy900)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = if (currentDraft.templateType == "CLASSIC_RECEIPT") "SALES RECEIPT PREVIEW" else "INVOICE PREVIEW & EDIT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${if (currentDraft.templateType == "CLASSIC_RECEIPT") "Receipt #" else "Invoice #"} ${currentDraft.invoiceNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }
                    }
                }
            }

            // PDF Template Selector
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Bill Layout",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val isMasterTools = currentDraft.templateType == "MASTER_TOOLS" || currentDraft.companyName.equals("MASTER TOOLS", ignoreCase = true)
                        val isLayout1 = !isMasterTools && (currentDraft.templateType == "LAYOUT_1" || currentDraft.templateType == "CLASSIC_RECEIPT")
                        val isLayoutFour = !isMasterTools && (currentDraft.templateType == "LAYOUT_4" || currentDraft.templateType == "FOUR" || currentDraft.templateType == "4")
                        val isLayout2 = !isMasterTools && !isLayout1 && !isLayoutFour
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = isMasterTools,
                                onClick = {
                                    viewModel.updateDraft(
                                        currentDraft.copy(
                                            templateType = "MASTER_TOOLS",
                                            companyName = "MASTER TOOLS",
                                            companyAddress = "123 Hardware Lane",
                                            companyPhone1 = "+1-555-0198",
                                            companyPhone2 = "Tax ID: MT-987654321",
                                            companyEmail = "contact@mastertools.com",
                                            currency = "$",
                                            headerTitle = "INVOICE",
                                            paymentTerms = "Net 30. Bank: Master Tools Bank | Acc: 123456789 | Routing: 987654321",
                                            notes = "Tax ID: MT-987654321 • contact@mastertools.com"
                                        )
                                    )
                                },
                                label = { Text("Master Tools", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).testTag("template_master_tools_chip")
                            )

                            FilterChip(
                                selected = isLayout1,
                                onClick = {
                                    viewModel.updateDraft(
                                        currentDraft.copy(
                                            templateType = "LAYOUT_1",
                                            companyName = "XP Computers",
                                            companyAddress = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET",
                                            companyPhone1 = "MUSHARAF #03485577343",
                                            companyPhone2 = "ISMAIL #03002698445",
                                            currency = "PKR",
                                            headerTitle = "Sales Receipt"
                                        )
                                    )
                                },
                                label = { Text("XP Computers", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).testTag("template_layout_1_chip")
                            )

                            FilterChip(
                                selected = isLayout2,
                                onClick = {
                                    viewModel.updateDraft(
                                        currentDraft.copy(
                                            templateType = "LAYOUT_2",
                                            companyName = "Master Tech",
                                            companyAddress = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET",
                                            companyPhone1 = "MUSHARAF #03485577343",
                                            companyPhone2 = "ISMAIL #03002698445",
                                            currency = "PKR",
                                            headerTitle = "Sales Receipt"
                                        )
                                    )
                                },
                                label = { Text("Master Tech", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).testTag("template_layout_2_chip")
                            )

                            FilterChip(
                                selected = isLayoutFour,
                                onClick = {
                                    viewModel.updateDraft(
                                        currentDraft.copy(
                                            templateType = "LAYOUT_4",
                                            companyName = "Master Tech.",
                                            companyAddress = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET",
                                            companyPhone1 = "MUSHARAF #03485577343",
                                            companyPhone2 = "ISMAIL #03002698445",
                                            currency = "PKR",
                                            headerTitle = "Sales Receipt"
                                        )
                                    )
                                },
                                label = { Text("Four", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(0.7f).testTag("template_layout_4_chip")
                            )
                        }
                    }
                }
            }

            // Company & Receipt Business Details (for Master Tools, XP Computers, Master Tech & Four)
            if (currentDraft.templateType == "MASTER_TOOLS" || currentDraft.templateType == "LAYOUT_1" || currentDraft.templateType == "CLASSIC_RECEIPT" || currentDraft.templateType == "LAYOUT_2" || currentDraft.templateType == "LAYOUT_4" || currentDraft.templateType == "FOUR") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF0284C7))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Business Details (Header)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = currentDraft.companyName,
                                onValueChange = { viewModel.updateDraft(currentDraft.copy(companyName = it)) },
                                label = { Text("Company / Business Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("edit_company_name")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = currentDraft.companyPhone1,
                                    onValueChange = { viewModel.updateDraft(currentDraft.copy(companyPhone1 = it)) },
                                    label = { Text("Phone") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("edit_company_phone1")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = currentDraft.companyPhone2,
                                    onValueChange = { viewModel.updateDraft(currentDraft.copy(companyPhone2 = it)) },
                                    label = { Text("Tax ID / Extra") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("edit_company_phone2")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = currentDraft.companyAddress,
                                onValueChange = { viewModel.updateDraft(currentDraft.copy(companyAddress = it)) },
                                label = { Text("Address / Location") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("edit_company_address")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = currentDraft.currency,
                                onValueChange = { viewModel.updateDraft(currentDraft.copy(currency = it)) },
                                label = { Text("Currency ($, PKR, USD, etc.)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("edit_currency")
                            )
                        }
                    }
                }
            }

            // Client & Invoice Metadata Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentDraft.templateType == "CLASSIC_RECEIPT") "Sold To (Customer / Cash)" else "Client & Billing Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = currentDraft.clientName,
                            onValueChange = { viewModel.updateDraft(currentDraft.copy(clientName = it)) },
                            label = { Text(if (currentDraft.templateType == "CLASSIC_RECEIPT") "Sold To (e.g. Cash)" else "Client Name / Company") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("edit_client_name")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = currentDraft.issueDate,
                            onValueChange = { viewModel.updateDraft(currentDraft.copy(issueDate = it)) },
                            label = { Text("Bill Date") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (currentDraft.templateType != "CLASSIC_RECEIPT") {
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = currentDraft.clientEmail,
                                onValueChange = { viewModel.updateDraft(currentDraft.copy(clientEmail = it)) },
                                label = { Text("Client Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = currentDraft.clientAddress,
                                onValueChange = { viewModel.updateDraft(currentDraft.copy(clientAddress = it)) },
                                label = { Text("Client Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Line Items Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Line Items (${currentDraft.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )

                    Button(
                        onClick = { showAddItemDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_line_item_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp)
                    }
                }
            }

            // Line Items List
            items(currentDraft.items, key = { it.id }) { item ->
                EditableItemRow(
                    item = item,
                    draft = currentDraft,
                    onDelete = { viewModel.removeDraftItem(item.id) }
                )
            }

            // Financial Calculations Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Financial Calculations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Subtotal
                        CalculationRow(
                            label = "Subtotal (Sum of Items):",
                            value = currentDraft.formatMoney(currentDraft.subtotal),
                            isBold = false
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Tax Rate Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tax / GST (%):",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                            OutlinedTextField(
                                value = if (currentDraft.taxRate == 0.0) "" else currentDraft.taxRate.toString(),
                                onValueChange = {
                                    val rate = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateDraftTaxRate(rate)
                                },
                                placeholder = { Text("0.0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(110.dp).testTag("edit_tax_rate")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        CalculationRow(
                            label = "Tax Amount:",
                            value = currentDraft.formatMoney(currentDraft.taxAmount),
                            isBold = false
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Discount Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Discount (${currentDraft.currency}):",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                            OutlinedTextField(
                                value = if (currentDraft.discountAmount == 0.0) "" else currentDraft.discountAmount.toString(),
                                onValueChange = {
                                    val d = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateDraftDiscount(d)
                                },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.width(110.dp).testTag("edit_discount_amount")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Grand Total Highlight
                        CalculationRow(
                            label = "Grand Total:",
                            value = currentDraft.formatMoney(currentDraft.grandTotal),
                            isBold = true,
                            color = PrimaryBlue
                        )
                    }
                }
            }
        }

        // Sticky Bottom Banner & Action Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL DUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = currentDraft.formatMoney(currentDraft.grandTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                }

                Button(
                    onClick = { viewModel.saveDraftToLedgerAndGeneratePdf(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp).testTag("save_and_generate_pdf_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentDraft.templateType == "CLASSIC_RECEIPT") "Generate Receipt PDF" else "Generate PDF & Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        AddItemDialog(
            currency = currentDraft.currency,
            onDismiss = { showAddItemDialog = false },
            onAdd = { name, desc, qty, price ->
                viewModel.addDraftItem(name, desc, qty, price)
                showAddItemDialog = false
            }
        )
    }
}

@Composable
fun EditableItemRow(
    item: InvoiceDraftItem,
    draft: InvoiceDraft,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                Text(
                    text = "$qtyStr × ${draft.formatMoney(item.unitPrice)}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = draft.formatMoney(item.total),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CalculationRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Navy900
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Navy900 else Color(0xFF64748B)
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun AddItemDialog(
    currency: String = "USD",
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var priceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Line Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Unit Price ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyText.toDoubleOrNull() ?: 1.0
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    onAdd(name, desc, q, p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

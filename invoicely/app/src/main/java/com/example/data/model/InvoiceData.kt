package com.example.data.model

import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems

data class InvoiceDraft(
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String = "",
    val clientName: String,
    val clientEmail: String = "",
    val clientAddress: String = "",
    val companyName: String = "Xp Computer",
    val companyEmail: String = "",
    val companyAddress: String = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET",
    val items: List<InvoiceDraftItem> = emptyList(),
    val taxRate: Double = 0.0,
    val discountAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentTerms: String = "Cash on Delivery",
    val notes: String = "Thank you for your business.",
    val currency: String = "PKR",
    val templateType: String = "CLASSIC_RECEIPT", // CLASSIC_RECEIPT or MODERN
    val companyPhone1: String = "MUSHARAF #03485577343",
    val companyPhone2: String = "ISMAIL #03002698445",
    val headerTitle: String = "Sales Receipt",
    val createdBy: String = "Mansoor"
) {
    val subtotal: Double
        get() = items.sumOf { it.total }

    val taxAmount: Double
        get() = Math.round((subtotal * (taxRate / 100.0)) * 100.0) / 100.0

    val grandTotal: Double
        get() = Math.round(Math.max(0.0, (subtotal + taxAmount) - discountAmount) * 100.0) / 100.0

    val balanceDue: Double
        get() = Math.round(Math.max(0.0, grandTotal - amountPaid) * 100.0) / 100.0

    val status: String
        get() = when {
            balanceDue <= 0.001 -> "PAID"
            else -> "PENDING"
        }

    fun formatMoney(amount: Double): String {
        return if (currency.equals("USD", ignoreCase = true)) {
            String.format(java.util.Locale.US, "$%.2f", amount)
        } else {
            String.format(java.util.Locale.US, "%s %.2f", currency, amount)
        }
    }

    fun toEntity(id: Long = 0, pdfPath: String? = null): InvoiceEntity {
        return InvoiceEntity(
            id = id,
            invoiceNumber = invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            clientName = clientName,
            clientEmail = clientEmail,
            clientAddress = clientAddress,
            companyName = companyName,
            companyEmail = companyEmail,
            companyAddress = companyAddress,
            subtotal = subtotal,
            taxRate = taxRate,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            grandTotal = grandTotal,
            amountPaid = amountPaid,
            balanceDue = balanceDue,
            status = status,
            paymentTerms = paymentTerms,
            notes = notes,
            pdfFilePath = pdfPath,
            currency = currency,
            templateType = templateType,
            companyPhone1 = companyPhone1,
            companyPhone2 = companyPhone2,
            headerTitle = headerTitle,
            createdBy = createdBy
        )
    }

    fun toItemEntities(invoiceId: Long): List<InvoiceItemEntity> {
        return items.map {
            InvoiceItemEntity(
                invoiceId = invoiceId,
                itemName = it.name,
                itemDescription = it.description,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                itemTotal = it.total
            )
        }
    }
}

data class InvoiceDraftItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0
) {
    val total: Double
        get() = Math.round((quantity * unitPrice) * 100.0) / 100.0
}

data class LedgerMetrics(
    val totalBilled: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalCount: Int = 0
)

fun InvoiceWithItems.toDraft(): InvoiceDraft {
    return InvoiceDraft(
        invoiceNumber = invoice.invoiceNumber,
        issueDate = invoice.issueDate,
        dueDate = invoice.dueDate,
        clientName = invoice.clientName,
        clientEmail = invoice.clientEmail,
        clientAddress = invoice.clientAddress,
        companyName = invoice.companyName,
        companyEmail = invoice.companyEmail,
        companyAddress = invoice.companyAddress,
        items = items.map {
            InvoiceDraftItem(
                id = it.id.toString(),
                name = it.itemName,
                description = it.itemDescription,
                quantity = it.quantity,
                unitPrice = it.unitPrice
            )
        },
        taxRate = invoice.taxRate,
        discountAmount = invoice.discountAmount,
        amountPaid = invoice.amountPaid,
        paymentTerms = invoice.paymentTerms,
        notes = invoice.notes,
        currency = invoice.currency,
        templateType = invoice.templateType,
        companyPhone1 = invoice.companyPhone1,
        companyPhone2 = invoice.companyPhone2,
        headerTitle = invoice.headerTitle,
        createdBy = invoice.createdBy
    )
}

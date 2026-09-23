package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String = "",
    val clientName: String,
    val clientEmail: String,
    val clientAddress: String,
    val companyName: String = "Acme Solutions Inc.",
    val companyEmail: String = "billing@acmesolutions.com",
    val companyAddress: String = "100 Enterprise Way, Suite 400",
    val subtotal: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val amountPaid: Double = 0.0,
    val balanceDue: Double,
    val status: String = "PENDING", // PENDING, PAID, OVERDUE
    val paymentTerms: String = "Payment due within 15 days of invoice date.",
    val notes: String = "Thank you for your valued business.",
    val pdfFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val currency: String = "PKR",
    val templateType: String = "CLASSIC_RECEIPT",
    val companyPhone1: String = "MUSHARAF #03485577343",
    val companyPhone2: String = "ISMAIL #03002698445",
    val headerTitle: String = "Sales Receipt",
    val createdBy: String = "Mansoor"
)

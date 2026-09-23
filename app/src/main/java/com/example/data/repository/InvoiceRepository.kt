package com.example.data.repository

import com.example.data.local.InvoiceDao
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemEntity
import com.example.data.local.InvoiceWithItems
import kotlinx.coroutines.flow.Flow

class InvoiceRepository(private val invoiceDao: InvoiceDao) {

    val allInvoicesWithItems: Flow<List<InvoiceWithItems>> = invoiceDao.getAllInvoicesWithItems()

    fun getInvoiceById(id: Long): Flow<InvoiceWithItems?> = invoiceDao.getInvoiceWithItemsById(id)

    suspend fun saveInvoiceWithItems(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Long {
        val invoiceId = invoiceDao.insertInvoice(invoice)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        invoiceDao.insertItems(itemsWithId)
        return invoiceId
    }

    suspend fun deleteInvoice(id: Long) {
        invoiceDao.deleteInvoiceById(id)
    }

    suspend fun deleteAllInvoices() {
        invoiceDao.deleteAllInvoiceItems()
        invoiceDao.deleteAllInvoices()
    }

    suspend fun updatePayment(id: Long, amountPaid: Double, balanceDue: Double, status: String) {
        invoiceDao.updatePayment(id, amountPaid, balanceDue, status)
    }

    suspend fun updatePdfPath(id: Long, path: String) {
        invoiceDao.updatePdfPath(id, path)
    }

    suspend fun ensureMasterToolsInvoice() {
        val existing = invoiceDao.getInvoiceByNumber("INV-2023-001")
        if (existing == null) {
            val invoice = InvoiceEntity(
                invoiceNumber = "INV-2023-001",
                issueDate = "2023-10-24",
                dueDate = "2023-11-24",
                clientName = "John Doe",
                clientEmail = "john@buildit.com",
                clientAddress = "BuildIt Construction, 456 Builder Ave",
                companyName = "MASTER TOOLS",
                companyEmail = "contact@mastertools.com",
                companyAddress = "123 Hardware Lane",
                subtotal = 645.00,
                taxRate = 10.0,
                taxAmount = 64.50,
                discountAmount = 20.00,
                grandTotal = 689.50,
                amountPaid = 0.0,
                balanceDue = 689.50,
                status = "PENDING",
                paymentTerms = "Net 30. Payment Terms & Bank Details: Bank: Master Tools Bank | Acc: 123456789 | Routing: 987654321",
                notes = "Tax ID: MT-987654321 • Phone: +1-555-0198",
                currency = "$",
                templateType = "MASTER_TOOLS",
                companyPhone1 = "+1-555-0198",
                companyPhone2 = "Tax ID: MT-987654321",
                headerTitle = "INVOICE",
                createdBy = "Mansoor"
            )

            val items = listOf(
                InvoiceItemEntity(
                    itemName = "Cordless Power Drill Set",
                    itemDescription = "Cordless Power Drill Set",
                    quantity = 2.0,
                    unitPrice = 150.00,
                    itemTotal = 300.00
                ),
                InvoiceItemEntity(
                    itemName = "Precision Screwdriver Kit",
                    itemDescription = "Precision Screwdriver Kit",
                    quantity = 5.0,
                    unitPrice = 25.00,
                    itemTotal = 125.00
                ),
                InvoiceItemEntity(
                    itemName = "Heavy Duty Socket Wrench Set",
                    itemDescription = "Heavy Duty Socket Wrench Set",
                    quantity = 1.0,
                    unitPrice = 85.00,
                    itemTotal = 85.00
                ),
                InvoiceItemEntity(
                    itemName = "Digital Multimeter",
                    itemDescription = "Digital Multimeter",
                    quantity = 3.0,
                    unitPrice = 45.00,
                    itemTotal = 135.00
                )
            )

            saveInvoiceWithItems(invoice, items)
        }
    }
}

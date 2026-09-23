package com.example.data.parser

import android.content.Context
import android.net.Uri
import com.example.data.model.InvoiceDraft
import com.example.data.model.InvoiceDraftItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

object SpreadsheetParser {

    /**
     * Parses an input stream from a picked file (.xlsx or .csv)
     */
    fun parseStream(inputStream: InputStream, filename: String): ParseResult {
        return try {
            val isXlsx = filename.endsWith(".xlsx", ignoreCase = true)
            if (isXlsx) {
                parseXlsx(inputStream)
            } else {
                parseCsvOrTsv(inputStream)
            }
        } catch (e: Exception) {
            ParseResult.Error("Failed to parse spreadsheet: ${e.localizedMessage ?: "Unknown format"}")
        }
    }

    /**
     * Parses raw CSV or TSV text
     */
    fun parseCsvText(text: String): ParseResult {
        val rows = parseCsvRows(text)
        return processTableRows(rows)
    }

    /**
     * Reads CSV / TSV from InputStream
     */
    private fun parseCsvOrTsv(inputStream: InputStream): ParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val fullContent = reader.readText()
        return parseCsvText(fullContent)
    }

    /**
     * Parses a CSV/TSV string into rows and columns handling quotes
     */
    private fun parseCsvRows(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val lines = text.split(Regex("\r?\n"))

        // Detect separator (comma, semicolon, or tab)
        val firstLine = lines.firstOrNull { it.isNotBlank() } ?: return emptyList()
        val commaCount = firstLine.count { it == ',' }
        val semiCount = firstLine.count { it == ';' }
        val tabCount = firstLine.count { it == '\t' }
        val separator = when {
            semiCount > commaCount && semiCount > tabCount -> ';'
            tabCount > commaCount && tabCount > semiCount -> '\t'
            else -> ','
        }

        for (line in lines) {
            if (line.isBlank()) continue
            val row = mutableListOf<String>()
            val sb = java.lang.StringBuilder()
            var inQuotes = false

            for (i in line.indices) {
                val c = line[i]
                when {
                    c == '"' -> inQuotes = !inQuotes
                    c == separator && !inQuotes -> {
                        row.add(sb.toString().trim().removeSurrounding("\""))
                        sb.clear()
                    }
                    else -> sb.append(c)
                }
            }
            row.add(sb.toString().trim().removeSurrounding("\""))
            result.add(row)
        }
        return result
    }

    /**
     * Native XLSX parser using ZipInputStream and XmlPullParser.
     * Extracts shared strings and sheet cell values.
     */
    private fun parseXlsx(inputStream: InputStream): ParseResult {
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        val sharedStrings = mutableListOf<String>()
        val sheetBytes = mutableListOf<ByteArray>()

        // 1. First scan for sharedStrings.xml and sheet1.xml
        val allEntries = mutableMapOf<String, ByteArray>()
        while (entry != null) {
            val name = entry.name
            if (name.equals("xl/sharedStrings.xml", ignoreCase = true) ||
                name.contains("sheet1.xml", ignoreCase = true)
            ) {
                allEntries[name] = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        // 2. Parse sharedStrings.xml if present
        val sharedStringsBytes = allEntries.entries.find { it.key.contains("sharedStrings.xml", ignoreCase = true) }?.value
        if (sharedStringsBytes != null) {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(sharedStringsBytes.inputStream(), "UTF-8")

            var eventType = parser.eventType
            var inText = false
            var currentStr = java.lang.StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inText = true
                            currentStr.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            currentStr.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.equals("t", ignoreCase = true)) {
                            inText = false
                            sharedStrings.add(currentStr.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        // 3. Parse sheet1.xml
        val sheetDataBytes = allEntries.entries.find { it.key.contains("sheet1.xml", ignoreCase = true) }?.value
            ?: allEntries.entries.find { it.key.contains("sheet", ignoreCase = true) }?.value
            ?: return ParseResult.Error("Could not find worksheet in Excel file")

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(sheetDataBytes.inputStream(), "UTF-8")

        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        var currentCellType: String? = null
        var inValue = false
        var cellVal = java.lang.StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "row" -> {
                            currentRow = mutableListOf()
                        }
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t")
                            cellVal.clear()
                        }
                        "v" -> {
                            inValue = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValue) {
                        cellVal.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name.lowercase()) {
                        "v" -> {
                            inValue = false
                        }
                        "c" -> {
                            val raw = cellVal.toString().trim()
                            val finalVal = if (currentCellType == "s") {
                                val idx = raw.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                            } else {
                                raw
                            }
                            currentRow.add(finalVal)
                            currentCellType = null
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                rows.add(currentRow)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return processTableRows(rows)
    }

    /**
     * Extracts invoice metadata and structured item rows from 2D grid
     */
    private fun processTableRows(rows: List<List<String>>): ParseResult {
        if (rows.isEmpty()) {
            return ParseResult.Error("Spreadsheet has no rows")
        }

        var invoiceNumber = ""
        var issueDate = ""
        var dueDate = ""
        var clientName = ""
        var clientEmail = ""
        var clientAddress = ""
        var taxRate = 0.0
        var discount = 0.0
        var amountPaid = 0.0
        var paymentTerms = "Payment due within 15 days of invoice date."
        var companyName = "Xp Computer"
        var companyAddress = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET"
        var companyPhone1 = "MUSHARAF #03485577343"
        var companyPhone2 = "ISMAIL #03002698445"
        var currency = "PKR"
        var templateType = "CLASSIC_RECEIPT"
        var headerTitle = "Sales Receipt"

        val items = mutableListOf<InvoiceDraftItem>()

        var headerRowIndex = -1
        val colMap = mutableMapOf<String, Int>()

        // 1. Locate headers and key-value metadata
        for (r in rows.indices) {
            val row = rows[r]

            val hasItemCol = row.any { Regex("item|product|description|discription|service|title", RegexOption.IGNORE_CASE).containsMatchIn(it) }
            val hasQtyCol = row.any { Regex("qty|quantity|units|count", RegexOption.IGNORE_CASE).containsMatchIn(it) }
            val hasPriceCol = row.any { Regex("price|rate|cost|unit|amount", RegexOption.IGNORE_CASE).containsMatchIn(it) }

            if (hasItemCol && (hasQtyCol || hasPriceCol)) {
                headerRowIndex = r
                row.forEachIndexed { cIdx, colName ->
                    val str = colName.trim().lowercase()
                    when {
                        str.contains("item") || str.contains("product") || str.contains("service") || str.contains("title") || str.contains("discription") || str.contains("description") -> colMap["item"] = cIdx
                        str.contains("desc") -> colMap["desc"] = cIdx
                        str.contains("qty") || str.contains("quantity") || str.contains("units") || str.contains("count") -> colMap["qty"] = cIdx
                        str.contains("rate") || (str.contains("price") && !str.contains("total")) -> colMap["price"] = cIdx
                        str.contains("amount") && !str.contains("paid") -> colMap["amount"] = cIdx
                        str.contains("tax") || str.contains("gst") || str.contains("vat") -> colMap["tax"] = cIdx
                        str.contains("discount") -> colMap["discount"] = cIdx
                        str.contains("client") || str.contains("customer") || str.contains("sold to") -> colMap["client"] = cIdx
                        str.contains("invoice") || str.contains("receipt") -> colMap["invNum"] = cIdx
                        str.contains("due") -> colMap["dueDate"] = cIdx
                        str.contains("date") -> colMap["date"] = cIdx
                    }
                }
                break
            }

            // Key-value metadata rows
            for (c in 0 until row.size - 1) {
                val key = row[c].trim().lowercase()
                val value = row.getOrNull(c + 1)?.trim() ?: ""
                if (value.isBlank()) continue

                when {
                    Regex("invoice\\s*(#|no|num|id)|receipt\\s*(#|no|num|id)|^no\\.?$", RegexOption.IGNORE_CASE).containsMatchIn(key) && invoiceNumber.isBlank() -> invoiceNumber = value
                    Regex("issue\\s*date|invoice\\s*date|^date$", RegexOption.IGNORE_CASE).containsMatchIn(key) && issueDate.isBlank() -> issueDate = value
                    Regex("due\\s*date", RegexOption.IGNORE_CASE).containsMatchIn(key) && dueDate.isBlank() -> dueDate = value
                    Regex("client|customer|bill\\s*to|sold\\s*to", RegexOption.IGNORE_CASE).containsMatchIn(key) && clientName.isBlank() -> clientName = value
                    key.contains("company") -> companyName = value
                    key.contains("location") || key.contains("address") -> if (!key.contains("client")) companyAddress = value else clientAddress = value
                    key.contains("phone 1") || key.contains("contact 1") -> companyPhone1 = value
                    key.contains("phone 2") || key.contains("contact 2") -> companyPhone2 = value
                    key.contains("currency") -> currency = value.uppercase(Locale.US)
                    key.contains("template") -> templateType = value.uppercase(Locale.US)
                    key.contains("email") && clientEmail.isBlank() -> clientEmail = value
                    (key.contains("tax") || key.contains("gst")) && taxRate == 0.0 -> taxRate = parseNumber(value)
                    key.contains("discount") && discount == 0.0 -> discount = parseNumber(value)
                    (key.contains("paid") || key.contains("amount paid")) && amountPaid == 0.0 -> amountPaid = parseNumber(value)
                    key.contains("terms") -> paymentTerms = value
                }
            }
        }

        // 2. Parse Line Items
        val startRow = if (headerRowIndex >= 0) headerRowIndex + 1 else 1
        for (r in startRow until rows.size) {
            val row = rows[r]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            // Skip summary rows
            val first = row.getOrNull(0)?.lowercase() ?: ""
            val second = row.getOrNull(1)?.lowercase() ?: ""
            if (first.contains("total") || first.contains("subtotal") || first.contains("tax") ||
                second.contains("total") || second.contains("subtotal")
            ) {
                // extract tax or discount if present
                row.forEach { cell ->
                    val cl = cell.lowercase()
                    if (cl.contains("tax") && taxRate == 0.0) taxRate = parseNumber(cell)
                    if (cl.contains("discount") && discount == 0.0) discount = parseNumber(cell)
                }
                continue
            }

            var itemName = ""
            var itemDesc = ""
            var qty = 1.0
            var price = 0.0

            if (headerRowIndex >= 0) {
                colMap["item"]?.let { itemName = row.getOrNull(it) ?: "" }
                colMap["desc"]?.let { itemDesc = row.getOrNull(it) ?: "" }
                colMap["qty"]?.let { qty = parseNumber(row.getOrNull(it) ?: "1.0") }
                colMap["price"]?.let { price = parseNumber(row.getOrNull(it) ?: "0.0") }

                if (invoiceNumber.isBlank()) colMap["invNum"]?.let { invoiceNumber = row.getOrNull(it) ?: "" }
                if (clientName.isBlank()) colMap["client"]?.let { clientName = row.getOrNull(it) ?: "" }
                if (issueDate.isBlank()) colMap["date"]?.let { issueDate = row.getOrNull(it) ?: "" }
                if (dueDate.isBlank()) colMap["dueDate"]?.let { dueDate = row.getOrNull(it) ?: "" }
            } else {
                itemName = row.getOrNull(0) ?: ""
                qty = parseNumber(row.getOrNull(1) ?: "1.0")
                price = parseNumber(row.getOrNull(2) ?: "0.0")
            }

            if (itemName.isBlank() && price == 0.0) continue

            if (itemName.isBlank()) {
                itemName = "Item ${items.size + 1}"
            }
            if (qty <= 0.0) qty = 1.0

            items.add(
                InvoiceDraftItem(
                    name = itemName,
                    description = itemDesc,
                    quantity = qty,
                    unitPrice = price
                )
            )
        }

        // Defaults
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())
        val in15Days = sdf.format(Date(System.currentTimeMillis() + 15L * 24 * 60 * 60 * 1000))

        if (invoiceNumber.isBlank()) {
            invoiceNumber = "INV-${SimpleDateFormat("yyyy", Locale.US).format(Date())}-${(1000..9999).random()}"
        }
        if (issueDate.isBlank()) issueDate = today
        if (dueDate.isBlank()) dueDate = in15Days
        if (clientName.isBlank()) clientName = "Global Enterprises LLC"
        if (clientEmail.isBlank()) clientEmail = "billing@global-enterprises.com"
        if (clientAddress.isBlank()) clientAddress = "742 Evergreen Terrace, Springfield, OR"

        if (items.isEmpty()) {
            items.add(
                InvoiceDraftItem(
                    name = "Consulting Services",
                    description = "Professional architecture & integration deliverables",
                    quantity = 1.0,
                    unitPrice = 750.0
                )
            )
        }

        val draft = InvoiceDraft(
            invoiceNumber = invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            clientName = clientName,
            clientEmail = clientEmail,
            clientAddress = clientAddress,
            companyName = companyName,
            companyAddress = companyAddress,
            companyPhone1 = companyPhone1,
            companyPhone2 = companyPhone2,
            currency = currency,
            templateType = templateType,
            headerTitle = headerTitle,
            items = items,
            taxRate = taxRate,
            discountAmount = discount,
            amountPaid = amountPaid,
            paymentTerms = paymentTerms
        )

        return ParseResult.Success(draft)
    }

    private fun parseNumber(str: String): Double {
        val cleaned = str.replace(Regex("[^0-9.-]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    // Pre-built demo templates for instant 1-click loading in the app
    val SAMPLE_XP_COMPUTER = """
        Receipt #,27248
        Date,07-Feb-2026
        Company Name,Xp Computer
        Sold To,Cash
        Location,SADDAR RAWALPINDI - GREEN BUILDING - COMPUTER MARKET
        Phone 1,MUSHARAF #03485577343
        Phone 2,ISMAIL #03002698445
        Currency,PKR
        Template,CLASSIC_RECEIPT
        
        Discription,Qty,Rate,Amount
        Tenda Router AC6 (Dual Band ),1,7000.00,7000.00
    """.trimIndent()

    val SAMPLE_APEX_LOGISTICS = """
        Invoice #,INV-2026-1042
        Date,2026-09-01
        Due Date,2026-09-20
        Client Name,Apex Global Logistics Inc.
        Client Email,accounts@apexlogistics.com
        Client Address,742 Evergreen Terrace, Springfield, OR 97477
        Tax Rate %,8.5
        Discount,50.00
        Payment Terms,Net 15 Days. Wire transfer or corporate card.
        
        Item Name,Description,Quantity,Unit Price
        Cloud Server Migration,Azure to AWS architecture modernization,40,120.00
        DevOps Pipeline Setup,CI/CD workflow with automated security scans,1,1500.00
        Database Optimization,PostgreSQL query indexing & connection pooling,15,95.00
        Security Audit,Vulnerability penetration testing and remediation,1,2200.00
        24/7 SLA Support Tier,Enterprise incident response retainer,1,850.00
    """.trimIndent()

    val SAMPLE_CREATIVE_STUDIO = """
        Invoice #,INV-2026-2088
        Date,2026-09-05
        Due Date,2026-09-25
        Client Name,Quantum Retail Brands
        Client Email,finance@quantumbrands.io
        Client Address,880 Mission Bay Blvd, San Francisco, CA
        Tax Rate %,9.0
        Discount,100.00
        Payment Terms,Payment due upon receipt.
        
        Item Name,Description,Quantity,Unit Price
        Brand Identity System,Logo guidelines visual design and palette,1,3500.00
        Mobile UI/UX Design,Complete Figma prototypes and design system,60,85.00
        Front-end React Integration,Component library and testing,35,95.00
        Design Token Documentation,Figma to Kotlin Compose tokens,10,90.00
    """.trimIndent()

    val SAMPLE_WHOLESALE_SUPPLY = """
        Invoice #,INV-2026-3012
        Date,2026-09-02
        Due Date,2026-09-17
        Client Name,Horizon Industrial Corp
        Client Email,procurement@horizon-ind.com
        Client Address,1200 Industrial Pkwy, Chicago, IL
        Tax Rate %,7.0
        Discount,0.00
        Payment Terms,Net 30 Days.
        
        Item Name,Description,Quantity,Unit Price
        Heavy Duty Steel Enclosures,NEMA 4X certified electrical cabinets,12,320.00
        Industrial Sensors (Optical),High-precision distance measurement,25,145.00
        Programmable Logic Relay,48V DC automated controller units,8,450.00
        Cabling & Conduit Bundles,Shielded armored ethernet cables (500m),4,180.00
    """.trimIndent()
}

sealed class ParseResult {
    data class Success(val draft: InvoiceDraft) : ParseResult()
    data class Error(val message: String) : ParseResult()
}

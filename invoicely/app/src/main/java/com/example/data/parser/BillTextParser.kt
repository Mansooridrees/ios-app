package com.example.data.parser

import java.util.Locale
import java.util.regex.Pattern

data class ParsedOcrBill(
    val vendorName: String = "Master Tech.",
    val invoiceDate: String = "",
    val invoiceNumber: String = "",
    val clientName: String = "Cash",
    val items: List<ParsedOcrItem> = emptyList(),
    val grandTotal: Double = 0.0,
    val rawText: String = ""
)

data class ParsedOcrItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
    val total: Double
)

object BillTextParser {

    /**
     * Converts raw text (from camera OCR scan, image, or pasted receipt text)
     * into structured bill entities matching the user's logic:
     * - Date detection (DD/MM/YYYY, YYYY-MM-DD, or DD-MMM-YYYY)
     * - Vendor name
     * - Sold To / Customer name
     * - Grand Total detection
     * - Item lines: Name, Qty, Rate, Total
     */
    fun parseBillText(rawText: String): ParsedOcrBill {
        val lines = rawText.split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

        var vendorName = "Master Tech."
        var invoiceDate = ""
        var invoiceNumber = ""
        var clientName = "Cash"
        var grandTotal = 0.0
        val items = mutableListOf<ParsedOcrItem>()

        // 1. Basic Date detection: (DD/MM/YYYY, YYYY-MM-DD, or DD-MMM-YYYY)
        val dateRegex = Pattern.compile(
            "(\\d{1,2}[\\/\\-\\.]\\d{1,2}[\\/\\-\\.]\\d{2,4}|\\d{1,2}[\\-\\s](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[\\-\\s]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
        )

        // 2. Grand total regex: total, net amount, grand total, balance
        val totalRegex = Pattern.compile(
            "(?:total|net\\s*amount|grand\\s*total|balance|total\\s*pkr)[\\s\\:\\=]*[\\$₹Rs\\.]*\\s*([\\d,]+\\.?\\d*)",
            Pattern.CASE_INSENSITIVE
        )

        // 3. Invoice/Receipt number regex: No., Receipt #, Invoice #
        val numberRegex = Pattern.compile(
            "(?:invoice\\s*(?:no|#)?|receipt\\s*(?:no|#)?|no[\\.:]?)\\s*([A-Za-z0-9\\-]+)",
            Pattern.CASE_INSENSITIVE
        )

        // 4. Sold To / Client name regex
        val soldToRegex = Pattern.compile(
            "(?:sold\\s*to|billed\\s*to|customer|client)[\\s\\:\\=]*([A-Za-z0-9\\s\\.]+)",
            Pattern.CASE_INSENSITIVE
        )

        // 5. Item line detection:
        // Matches: [Description name] [Quantity] [Rate] [Total/Amount]
        val itemRow4Regex = Pattern.compile(
            "^([a-zA-Z0-9\\s\\(\\)\\-\\+]{3,})\\s+(\\d+(?:\\.\\d+)?)\\s+([\\d,]+(?:\\.\\d+)?)\\s+([\\d,]+(?:\\.\\d+)?)$"
        )
        // Matches: [Description name] [Quantity] [Rate]
        val itemRow3Regex = Pattern.compile(
            "^([a-zA-Z0-9\\s\\(\\)\\-\\+]{3,})\\s+(\\d+(?:\\.\\d+)?)\\s+([\\d,]+(?:\\.\\d+)?)$"
        )

        // Pre-scan for vendor name in top lines
        for (i in 0 until minOf(3, lines.size)) {
            val line = lines[i]
            if (line.contains("Master Tech", ignoreCase = true)) {
                vendorName = "Master Tech."
                break
            } else if (line.contains("XP Computer", ignoreCase = true)) {
                vendorName = "XP Computers"
                break
            }
        }

        for (line in lines) {
            // Ignore table headers
            val lower = line.lowercase(Locale.ROOT)
            if (lower.contains("discription") || lower.contains("description") ||
                (lower.contains("qty") && lower.contains("rate")) ||
                lower.contains("sales receipt")
            ) {
                continue
            }

            // Capture Date
            val dateMatcher = dateRegex.matcher(line)
            if (dateMatcher.find() && invoiceDate.isBlank()) {
                invoiceDate = dateMatcher.group(1) ?: ""
            }

            // Capture Receipt / Invoice Number
            val numberMatcher = numberRegex.matcher(line)
            if (numberMatcher.find() && invoiceNumber.isBlank()) {
                val candidate = numberMatcher.group(1)?.trim() ?: ""
                if (candidate.isNotBlank() && !candidate.equals("date", ignoreCase = true)) {
                    invoiceNumber = candidate
                }
            }

            // Capture Sold To / Client Name
            val soldMatcher = soldToRegex.matcher(line)
            if (soldMatcher.find() && (clientName == "Cash" || clientName.isBlank())) {
                val found = soldMatcher.group(1)?.trim() ?: ""
                if (found.isNotBlank() && !found.equals("no", ignoreCase = true)) {
                    clientName = found
                }
            }

            // Capture Grand Total
            val totalMatcher = totalRegex.matcher(line)
            if (totalMatcher.find()) {
                val rawNum = totalMatcher.group(1)?.replace(",", "") ?: ""
                rawNum.toDoubleOrNull()?.let {
                    grandTotal = it
                }
            }

            // Capture Item Rows
            val m4 = itemRow4Regex.matcher(line)
            if (m4.matches()) {
                val name = m4.group(1)?.trim() ?: ""
                val qty = m4.group(2)?.toDoubleOrNull() ?: 1.0
                val rate = m4.group(3)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                val total = m4.group(4)?.replace(",", "")?.toDoubleOrNull() ?: (qty * rate)
                if (name.isNotBlank() && !name.contains("total", ignoreCase = true)) {
                    items.add(ParsedOcrItem(name, qty, rate, total))
                }
                continue
            }

            val m3 = itemRow3Regex.matcher(line)
            if (m3.matches()) {
                val name = m3.group(1)?.trim() ?: ""
                val qty = m3.group(2)?.toDoubleOrNull() ?: 1.0
                val rate = m3.group(3)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && !name.contains("total", ignoreCase = true)) {
                    items.add(ParsedOcrItem(name, qty, rate, qty * rate))
                }
            }
        }

        // Fallback: If no date found, default to today
        if (invoiceDate.isBlank()) {
            invoiceDate = java.text.SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(java.util.Date())
        }

        return ParsedOcrBill(
            vendorName = vendorName,
            invoiceDate = invoiceDate,
            invoiceNumber = invoiceNumber,
            clientName = clientName,
            items = items,
            grandTotal = if (grandTotal > 0) grandTotal else items.sumOf { it.total },
            rawText = rawText
        )
    }
}

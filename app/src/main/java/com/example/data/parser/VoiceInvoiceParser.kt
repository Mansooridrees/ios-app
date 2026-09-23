package com.example.data.parser

import com.example.data.model.InvoiceDraft
import com.example.data.model.InvoiceDraftItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object VoiceInvoiceParser {

    private val WORD_TO_NUMBER = mapOf(
        "one" to 1.0,
        "two" to 2.0,
        "three" to 3.0,
        "four" to 4.0,
        "five" to 5.0,
        "six" to 6.0,
        "seven" to 7.0,
        "eight" to 8.0,
        "nine" to 9.0,
        "ten" to 10.0,
        "dozen" to 12.0
    )

    /**
     * Parses spoken or dictated text into a structured InvoiceDraft
     * Example inputs:
     * - "Sold to Cash, 1 Tenda Router AC6 Dual Band at 7000 PKR"
     * - "Cash receipt number 27248, 2 wireless mouse at 1200 each and 1 keyboard for 2500"
     * - "Customer Ismail, 5 Cat6 network cables rate 1500"
     */
    fun parseVoicePrompt(
        voiceText: String,
        companyName: String = "Xp Computer",
        companyAddress: String = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET",
        companyPhone1: String = "MUSHARAF #03485577343",
        companyPhone2: String = "ISMAIL #03002698445"
    ): InvoiceDraft {
        val trimmed = voiceText.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Currency detection
        val currency = when {
            lower.contains("dollar") || lower.contains("usd") || lower.contains("$") -> "USD"
            lower.contains("euro") || lower.contains("eur") || lower.contains("€") -> "EUR"
            lower.contains("gbp") || lower.contains("pound") || lower.contains("£") -> "GBP"
            else -> "PKR"
        }

        // 2. Receipt / Invoice Number detection
        val noPattern = Pattern.compile("(?:receipt|invoice|bill|no\\.?|#)\\s*(?:number|no)?\\s*([0-9A-Za-z-]+)", Pattern.CASE_INSENSITIVE)
        val noMatcher = noPattern.matcher(trimmed)
        val detectedInvoiceNo = if (noMatcher.find()) {
            noMatcher.group(1) ?: "27248"
        } else {
            // Default or random receipt number sequence
            val randomOffset = (System.currentTimeMillis() % 9000).toInt() + 20000
            randomOffset.toString()
        }

        // 3. Client / Customer / "Sold To" detection
        val clientPattern = Pattern.compile("(?:sold to|billed to|bill to|customer|client|for)\\s+([a-zA-Z0-9\\s]+?)(?:,|\\.|\\band\\b|\\bwith\\b|\\bqty\\b|\\brate\\b|\\bat\\b|\\bfor\\b|\\d|$)", Pattern.CASE_INSENSITIVE)
        val clientMatcher = clientPattern.matcher(trimmed)
        val detectedClient = when {
            clientMatcher.find() -> {
                val match = clientMatcher.group(1)?.trim() ?: "Cash"
                if (match.isBlank()) "Cash" else match.split(" ").take(3).joinToString(" ").replaceFirstChar { it.uppercase() }
            }
            lower.startsWith("cash") || lower.contains("sold to cash") -> "Cash"
            else -> "Cash"
        }

        // 4. Line items extraction
        val items = extractLineItems(trimmed)

        val finalItems = if (items.isEmpty()) {
            listOf(
                InvoiceDraftItem(
                    name = "Tenda Router AC6 (Dual Band )",
                    quantity = 1.0,
                    unitPrice = 7000.0
                )
            )
        } else {
            items
        }

        val todayDate = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date())

        return InvoiceDraft(
            invoiceNumber = detectedInvoiceNo,
            issueDate = todayDate,
            dueDate = todayDate,
            clientName = detectedClient,
            companyName = companyName,
            companyAddress = companyAddress,
            companyPhone1 = companyPhone1,
            companyPhone2 = companyPhone2,
            currency = currency,
            templateType = "CLASSIC_RECEIPT",
            headerTitle = "Sales Receipt",
            paymentTerms = "Cash on Delivery",
            notes = "Warranty claim requires original receipt.",
            items = finalItems
        )
    }

    private fun extractLineItems(text: String): List<InvoiceDraftItem> {
        // Strip client & receipt number phrases from item parsing
        var cleaned = text
            .replace(Regex("(?i)(?:sold to|billed to|bill to|customer|client)\\s+[a-zA-Z0-9\\s]+?(?:,|\\.|\\band\\b|\\d|$)"), " ")
            .replace(Regex("(?i)(?:receipt|invoice|bill)\\s*(?:number|no)?\\s*[0-9A-Za-z-]+"), " ")
            .replace(Regex("(?i)\\b(in pkr|pkr|rupees|rs\\.?|dollars|usd)\\b"), " ")

        // Split into candidate item clauses
        val clauses = cleaned.split(Regex("(?i)\\b(?:and|also|plus)\\b|[\\n,;]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val result = mutableListOf<InvoiceDraftItem>()

        for (clause in clauses) {
            val item = parseSingleItemClause(clause)
            if (item != null) {
                result.add(item)
            }
        }

        return result
    }

    private fun parseSingleItemClause(clause: String): InvoiceDraftItem? {
        val words = clause.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return null

        var qty = 1.0
        var price = 0.0

        // Find quantity
        // Check for words like "two" or digits
        val qtyRegex = Regex("(?:^|\\b)(\\d+(?:\\.\\d+)?)\\b\\s*(?:units?|pcs?|pieces?|x)?")
        val qtyMatch = qtyRegex.find(clause)
        if (qtyMatch != null) {
            qty = qtyMatch.groupValues[1].toDoubleOrNull() ?: 1.0
        } else {
            for (word in words) {
                if (WORD_TO_NUMBER.containsKey(word.lowercase(Locale.ROOT))) {
                    qty = WORD_TO_NUMBER[word.lowercase(Locale.ROOT)] ?: 1.0
                    break
                }
            }
        }

        // Find price / rate
        // Patterns like "at 7000", "rate 7000", "for 7000", "price 7000", "@ 7000", "7000 each"
        val priceRegex = Regex("(?:rate|at|for|price|@|costs?|worth)\\s*(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE)
        val priceMatch = priceRegex.find(clause)

        if (priceMatch != null) {
            price = priceMatch.groupValues[1].toDoubleOrNull() ?: 0.0
        } else {
            // Fallback: look for any number > 10 in the clause that is not the quantity
            val allNumbers = Regex("\\b(\\d+(?:\\.\\d+)?)\\b").findAll(clause).mapNotNull { it.value.toDoubleOrNull() }.toList()
            val candidatePrice = allNumbers.filter { it != qty && it >= 10.0 }.lastOrNull()
            if (candidatePrice != null) {
                price = candidatePrice
            }
        }

        // Clean name
        var name = clause
            .replace(qtyRegex, " ")
            .replace(priceRegex, " ")
            .replace(Regex("(?i)\\b(rate|at|for|price|each|per unit|pieces?|pcs?|units?|with|dual band)\\b"), " ")
            .replace(Regex("\\b\\d+(?:\\.\\d+)?\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        WORD_TO_NUMBER.keys.forEach { wordKey ->
            name = name.replace(Regex("(?i)\\b$wordKey\\b"), " ").trim()
        }

        // Capitalize words nicely
        name = name.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length <= 2) word.uppercase(Locale.US) else word.replaceFirstChar { it.uppercase() }
            }

        if (name.isBlank()) {
            if (price > 0) name = "Hardware Component" else return null
        }

        return InvoiceDraftItem(
            name = name,
            quantity = if (qty > 0) qty else 1.0,
            unitPrice = price
        )
    }

    // Pre-made voice template prompt examples for quick 1-tap testing
    val VOICE_SAMPLE_XP_COMPUTER = "Sold to Cash, 1 Tenda Router AC6 (Dual Band) at 7000 PKR"
    val VOICE_SAMPLE_TECH_UPGRADE = "Customer Musharaf, 2 Kingston SSD 256GB at 4800 and 1 Corsair RAM 8GB at 3500 PKR"
    val VOICE_SAMPLE_NETWORKING = "Cash receipt 27249, 3 TP-Link Archer C6 at 6500 each and 5 Cat6 Cable 30m at 1500"
    val VOICE_SAMPLE_ACCESSORIES = "Sold to Ismail, 4 Wireless Optical Mouse at 1200 and 2 Mechanical Keyboards at 3800"
}

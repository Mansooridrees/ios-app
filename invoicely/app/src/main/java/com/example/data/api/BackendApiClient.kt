package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class BackendApiClient(
    var baseUrl: String = "http://10.0.2.2:3000"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkHealth(): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/health"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                ApiResult.Success("Backend Online: $body")
            } else {
                ApiResult.Error("HTTP Error: ${response.code}")
            }
        } catch (e: Exception) {
            ApiResult.Error("Cannot connect to $baseUrl: ${e.localizedMessage}")
        }
    }

    suspend fun uploadExcelFile(file: File): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/api/upload-excel"
            val mediaType = if (file.name.endsWith(".csv", ignoreCase = true)) {
                "text/csv".toMediaTypeOrNull()
            } else {
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".toMediaTypeOrNull()
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody(mediaType)
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error("Upload failed (${response.code}): $body")
            }
        } catch (e: Exception) {
            ApiResult.Error("Upload error: ${e.localizedMessage}")
        }
    }

    suspend fun fetchInvoicesFromBackend(): ApiResult<List<RemoteInvoice>> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/api/invoices"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val jsonArr = json.optJSONArray("invoices") ?: org.json.JSONArray()
                val list = mutableListOf<RemoteInvoice>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val itemsArr = obj.optJSONArray("items") ?: org.json.JSONArray()
                    val items = mutableListOf<RemoteInvoiceItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        items.add(
                            RemoteInvoiceItem(
                                itemName = itemObj.optString("item_name", "Item"),
                                itemDescription = itemObj.optString("item_description", ""),
                                quantity = itemObj.optDouble("quantity", 1.0),
                                unitPrice = itemObj.optDouble("unit_price", 0.0),
                                itemTotal = itemObj.optDouble("item_total", 0.0)
                            )
                        )
                    }

                    list.add(
                        RemoteInvoice(
                            id = obj.optLong("id", 0L),
                            invoiceNumber = obj.optString("invoice_number", "INV-${System.currentTimeMillis()}"),
                            issueDate = obj.optString("issue_date", ""),
                            dueDate = obj.optString("due_date", ""),
                            clientName = obj.optString("client_name", "Client"),
                            clientEmail = obj.optString("client_email", ""),
                            clientAddress = obj.optString("client_address", ""),
                            companyName = obj.optString("company_name", "Master Tech"),
                            subtotal = obj.optDouble("subtotal", 0.0),
                            taxRate = obj.optDouble("tax_rate", 0.0),
                            taxAmount = obj.optDouble("tax_amount", 0.0),
                            discountAmount = obj.optDouble("discount_amount", 0.0),
                            grandTotal = obj.optDouble("grand_total", 0.0),
                            amountPaid = obj.optDouble("amount_paid", 0.0),
                            balanceDue = obj.optDouble("balance_due", 0.0),
                            status = obj.optString("status", "PENDING"),
                            paymentTerms = obj.optString("payment_terms", "Cash"),
                            items = items
                        )
                    )
                }
                ApiResult.Success(list)
            } else {
                ApiResult.Error("Failed to fetch invoices (${response.code})")
            }
        } catch (e: Exception) {
            ApiResult.Error("Fetch failed: ${e.localizedMessage}")
        }
    }

    suspend fun pushInvoiceToBackend(
        invoiceNumber: String,
        issueDate: String,
        dueDate: String,
        clientName: String,
        clientEmail: String,
        clientAddress: String,
        companyName: String,
        subtotal: Double,
        taxRate: Double,
        taxAmount: Double,
        discountAmount: Double,
        grandTotal: Double,
        amountPaid: Double,
        balanceDue: Double,
        status: String,
        paymentTerms: String,
        items: List<Pair<String, Pair<Double, Double>>> // name -> (qty, price)
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/api/invoices"
            val json = JSONObject()
            json.put("invoice_number", invoiceNumber)
            json.put("issue_date", issueDate)
            json.put("due_date", dueDate)
            json.put("client_name", clientName)
            json.put("client_email", clientEmail)
            json.put("client_address", clientAddress)
            json.put("company_name", companyName)
            json.put("subtotal", subtotal)
            json.put("tax_rate", taxRate)
            json.put("tax_amount", taxAmount)
            json.put("discount_amount", discountAmount)
            json.put("grand_total", grandTotal)
            json.put("amount_paid", amountPaid)
            json.put("balance_due", balanceDue)
            json.put("status", status)
            json.put("payment_terms", paymentTerms)

            val jsonItems = org.json.JSONArray()
            for ((name, pair) in items) {
                val itemObj = JSONObject()
                itemObj.put("item_name", name)
                itemObj.put("item_description", "")
                itemObj.put("quantity", pair.first)
                itemObj.put("unit_price", pair.second)
                itemObj.put("item_total", pair.first * pair.second)
                jsonItems.put(itemObj)
            }
            json.put("items", jsonItems)

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                ApiResult.Success("Synced with backend: $invoiceNumber")
            } else {
                ApiResult.Error("Push failed (${response.code}): $body")
            }
        } catch (e: Exception) {
            ApiResult.Error("Push error: ${e.localizedMessage}")
        }
    }

    suspend fun updateBackendPayment(invoiceId: Long, amountPaid: Double): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${baseUrl.trimEnd('/')}/api/invoices/$invoiceId/payment"
            val json = JSONObject().apply { put("amount_paid", amountPaid) }
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = json.toString().toRequestBody(mediaType)
            val request = Request.Builder().url(url).patch(requestBody).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult.Success("Backend payment updated")
            } else {
                ApiResult.Error("Payment update failed: ${response.code}")
            }
        } catch (e: Exception) {
            ApiResult.Error("Payment update error: ${e.localizedMessage}")
        }
    }
}

data class RemoteInvoice(
    val id: Long,
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String,
    val clientName: String,
    val clientEmail: String,
    val clientAddress: String,
    val companyName: String,
    val subtotal: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val amountPaid: Double,
    val balanceDue: Double,
    val status: String,
    val paymentTerms: String,
    val items: List<RemoteInvoiceItem>
)

data class RemoteInvoiceItem(
    val itemName: String,
    val itemDescription: String,
    val quantity: Double,
    val unitPrice: Double,
    val itemTotal: Double
)

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}

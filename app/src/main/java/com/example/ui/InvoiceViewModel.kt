package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiResult
import com.example.data.api.BackendApiClient
import com.example.data.local.InvoiceWithItems
import com.example.data.local.UserEntity
import com.example.data.model.InvoiceDraft
import com.example.data.model.InvoiceDraftItem
import com.example.data.model.LedgerMetrics
import com.example.data.model.toDraft
import com.example.data.parser.ParseResult
import com.example.data.parser.SpreadsheetParser
import com.example.data.pdf.PdfInvoiceGenerator
import com.example.data.repository.InvoiceRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

enum class AppTab {
    DASHBOARD,
    IMPORT,
    PREVIEW_EDIT,
    PDF_VIEWER,
    BACKEND_INFO
}

enum class StatusFilter {
    ALL,
    PENDING,
    PAID,
    OVERDUE
}

data class AuthUser(
    val username: String,
    val displayName: String,
    val role: String = "Super Admin"
)

class InvoiceViewModel(
    private val repository: InvoiceRepository,
    private val userRepository: UserRepository? = null,
    val backendClient: BackendApiClient = BackendApiClient()
) : ViewModel() {

    init {
        viewModelScope.launch {
            userRepository?.ensureDefaultAdmin()
            repository.ensureMasterToolsInvoice()
        }
    }

    // User management state
    val allUsers: StateFlow<List<UserEntity>> = userRepository?.allUsers?.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    ) ?: MutableStateFlow(
        listOf(
            UserEntity(
                id = 1,
                username = "mansoor",
                displayName = "Mansoor",
                password = "admin123",
                role = "Super Admin"
            ),
            UserEntity(
                id = 2,
                username = "abdulqadir",
                displayName = "Abdul Qadir",
                password = "user123",
                role = "Cashier"
            )
        )
    )

    // Authentication State: Default to logged in as Mansoor (Super Admin)
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<AuthUser?>(
        AuthUser(username = "mansoor", displayName = "Mansoor", role = "Super Admin")
    )
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(username: String, password: String): Boolean {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        val isMansoor = cleanUser.equals("mansoor", ignoreCase = true) ||
                cleanUser.equals("mansoor.ijg@gmail.com", ignoreCase = true)
        val isPassMatch = cleanPass == "admin123" || cleanPass == "Admin123"

        if (isMansoor && isPassMatch) {
            _currentUser.value = AuthUser(
                username = "mansoor",
                displayName = "Mansoor",
                role = "Super Admin"
            )
            _isLoggedIn.value = true
            _loginError.value = null
            return true
        }

        // Check against created users
        val userMatch = allUsers.value.find {
            (it.username.equals(cleanUser, ignoreCase = true) ||
             it.displayName.equals(cleanUser, ignoreCase = true) ||
             it.username.replace(" ", "").equals(cleanUser.replace(" ", ""), ignoreCase = true) ||
             it.username.replace(".", "").equals(cleanUser.replace(".", "").replace(" ", ""), ignoreCase = true)) &&
            it.password == cleanPass
        }

        if (userMatch != null) {
            _currentUser.value = AuthUser(
                username = userMatch.username,
                displayName = userMatch.displayName,
                role = userMatch.role
            )
            _isLoggedIn.value = true
            _loginError.value = null
            return true
        }

        _loginError.value = "Access Denied: Invalid username or password."
        return false
    }

    fun createUser(
        username: String,
        displayName: String,
        password: String,
        role: String = "Cashier"
    ): Pair<Boolean, String> {
        val cleanUser = username.trim().lowercase()
        val cleanName = displayName.trim().ifBlank { username.trim() }
        val cleanPass = password.trim()

        if (cleanUser.isBlank()) {
            return Pair(false, "Username cannot be empty.")
        }
        if (cleanPass.isBlank()) {
            return Pair(false, "Password cannot be empty.")
        }
        if (cleanUser == "mansoor") {
            return Pair(false, "Username 'mansoor' is reserved for Super Admin.")
        }
        if (allUsers.value.any { it.username.equals(cleanUser, ignoreCase = true) }) {
            return Pair(false, "A user with username '$cleanUser' already exists.")
        }

        viewModelScope.launch {
            userRepository?.insertUser(
                UserEntity(
                    username = cleanUser,
                    displayName = cleanName,
                    password = cleanPass,
                    role = role
                )
            )
        }
        return Pair(true, "User '$cleanName' created successfully.")
    }

    fun deleteUser(user: UserEntity): Pair<Boolean, String> {
        if (user.username.equals("mansoor", ignoreCase = true) || user.role == "Super Admin") {
            return Pair(false, "Cannot delete Super Admin account.")
        }
        viewModelScope.launch {
            userRepository?.deleteUser(user.id)
        }
        return Pair(true, "User removed successfully.")
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
        _loginError.value = null
        _currentTab.value = AppTab.DASHBOARD
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter.asStateFlow()

    // Invoices list from DB
    val allInvoices: StateFlow<List<InvoiceWithItems>> = repository.allInvoicesWithItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered Invoices
    val filteredInvoices: StateFlow<List<InvoiceWithItems>> = combine(
        allInvoices,
        _searchQuery,
        _statusFilter
    ) { invoices, query, filter ->
        invoices.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.invoice.clientName.contains(query, ignoreCase = true) ||
                    item.invoice.invoiceNumber.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                StatusFilter.ALL -> true
                StatusFilter.PENDING -> item.invoice.status == "PENDING"
                StatusFilter.PAID -> item.invoice.status == "PAID"
                StatusFilter.OVERDUE -> item.invoice.status == "OVERDUE"
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Ledger Summary Metrics
    val ledgerMetrics: StateFlow<LedgerMetrics> = allInvoices.combine(_searchQuery) { invoices, _ ->
        val totalBilled = invoices.sumOf { it.invoice.grandTotal }
        val totalCollected = invoices.sumOf { it.invoice.amountPaid }
        val totalPending = invoices.sumOf { it.invoice.balanceDue }
        LedgerMetrics(
            totalBilled = totalBilled,
            totalCollected = totalCollected,
            totalPending = totalPending,
            totalCount = invoices.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LedgerMetrics()
    )

    // Current draft being edited or previewed
    private val _currentDraft = MutableStateFlow<InvoiceDraft?>(null)
    val currentDraft: StateFlow<InvoiceDraft?> = _currentDraft.asStateFlow()

    // Selected PDF view state: Pair(InvoiceDraft, File)
    private val _activePdf = MutableStateFlow<Pair<InvoiceDraft, File>?>(null)
    val activePdf: StateFlow<Pair<InvoiceDraft, File>?> = _activePdf.asStateFlow()

    // Import status message
    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Backend status message
    private val _backendStatus = MutableStateFlow<String?>("Not checked yet")
    val backendStatus: StateFlow<String?> = _backendStatus.asStateFlow()

    // Theme Mode: null = system default, true = Dark, false = Light
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = when (_isDarkMode.value) {
            null -> true
            true -> false
            false -> null
        }
    }

    fun setDarkMode(dark: Boolean?) {
        _isDarkMode.value = dark
    }

    // Voice Dialog state
    private val _showVoiceDialog = MutableStateFlow(false)
    val showVoiceDialog: StateFlow<Boolean> = _showVoiceDialog.asStateFlow()

    init {
        // Ready for user-created, voice-dictated, or imported invoices
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    fun setVoiceDialogVisible(visible: Boolean) {
        _showVoiceDialog.value = visible
    }

    fun parseVoicePrompt(voiceText: String) {
        val draft = com.example.data.parser.VoiceInvoiceParser.parseVoicePrompt(voiceText)
        _currentDraft.value = draft
        _currentTab.value = AppTab.PREVIEW_EDIT
    }

    fun loadVoiceSampleXpComputer() {
        val draft = com.example.data.parser.VoiceInvoiceParser.parseVoicePrompt(
            com.example.data.parser.VoiceInvoiceParser.VOICE_SAMPLE_XP_COMPUTER
        )
        _currentDraft.value = draft
        _currentTab.value = AppTab.PREVIEW_EDIT
    }

    fun createDirectBill(
        customerName: String,
        items: List<InvoiceDraftItem>,
        invoiceNumber: String = "",
        layoutType: String = "LAYOUT_1"
    ) {
        val today = java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date())
        val generatedNum = if (invoiceNumber.isNotBlank()) invoiceNumber.trim() else (10000 + (Math.random() * 89999).toInt()).toString()
        val finalCustomer = customerName.trim().ifBlank { "Cash" }
        val finalItems = if (items.isEmpty()) {
            listOf(InvoiceDraftItem(name = "General Item", quantity = 1.0, unitPrice = 0.0))
        } else {
            items.filter { it.name.isNotBlank() || it.unitPrice > 0 }.ifEmpty {
                listOf(InvoiceDraftItem(name = "General Item", quantity = 1.0, unitPrice = 0.0))
            }
        }
        val isLayout1 = layoutType.equals("LAYOUT_1", ignoreCase = true) || layoutType.equals("CLASSIC_RECEIPT", ignoreCase = true)
        val isLayoutFour = layoutType.equals("LAYOUT_4", ignoreCase = true) || layoutType.equals("FOUR", ignoreCase = true) || layoutType.equals("4", ignoreCase = true)
        val isMasterTools = layoutType.equals("MASTER_TOOLS", ignoreCase = true) || layoutType.equals("Master Tools", ignoreCase = true)
        val currentCreator = _currentUser.value?.displayName ?: "Mansoor"

        val resolvedCompany = when {
            isMasterTools -> "MASTER TOOLS"
            isLayout1 -> "XP Computers"
            isLayoutFour -> "Master Tech."
            else -> "Master Tech"
        }
        val resolvedTemplate = when {
            isMasterTools -> "MASTER_TOOLS"
            isLayout1 -> "LAYOUT_1"
            isLayoutFour -> "LAYOUT_4"
            else -> "LAYOUT_2"
        }
        val resolvedCurrency = if (isMasterTools) "$" else "PKR"
        val resolvedAddress = if (isMasterTools) {
            "123 Hardware Lane"
        } else {
            "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET"
        }
        val resolvedPhone1 = if (isMasterTools) "+1-555-0198" else "MUSHARAF #03485577343"
        val resolvedPhone2 = if (isMasterTools) "Tax ID: MT-987654321" else "ISMAIL #03002698445"
        val resolvedPaymentTerms = if (isMasterTools) {
            "Net 30. Bank: Master Tools Bank | Acc: 123456789 | Routing: 987654321"
        } else {
            "Cash on Delivery"
        }
        val resolvedNotes = if (isMasterTools) "Tax ID: MT-987654321 • contact@mastertools.com" else "Thank you for your business."

        val draft = InvoiceDraft(
            invoiceNumber = generatedNum,
            issueDate = today,
            dueDate = today,
            clientName = finalCustomer,
            companyName = resolvedCompany,
            companyAddress = resolvedAddress,
            companyPhone1 = resolvedPhone1,
            companyPhone2 = resolvedPhone2,
            companyEmail = if (isMasterTools) "contact@mastertools.com" else "",
            currency = resolvedCurrency,
            templateType = resolvedTemplate,
            headerTitle = "INVOICE",
            paymentTerms = resolvedPaymentTerms,
            notes = resolvedNotes,
            items = finalItems,
            createdBy = currentCreator
        )
        _currentDraft.value = draft
        _currentTab.value = AppTab.PREVIEW_EDIT
    }

    fun createNewClassicReceipt() {
        val today = java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.US).format(java.util.Date())
        val currentCreator = _currentUser.value?.displayName ?: "Mansoor"
        val draft = InvoiceDraft(
            invoiceNumber = "27248",
            issueDate = today,
            dueDate = today,
            clientName = "Cash",
            companyName = "XP Computers",
            companyAddress = "SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET",
            companyPhone1 = "MUSHARAF #03485577343",
            companyPhone2 = "ISMAIL #03002698445",
            currency = "PKR",
            templateType = "LAYOUT_1",
            headerTitle = "Sales Receipt",
            paymentTerms = "Cash on Delivery",
            notes = "Thank you for your business.",
            items = listOf(
                InvoiceDraftItem(
                    name = "Tenda Router AC6 (Dual Band )",
                    quantity = 1.0,
                    unitPrice = 7000.0
                )
            ),
            createdBy = currentCreator
        )
        _currentDraft.value = draft
        _currentTab.value = AppTab.PREVIEW_EDIT
    }

    fun loadSampleData(sampleCsv: String) {
        _isImporting.value = true
        _importStatus.value = "Parsing spreadsheet sample..."
        viewModelScope.launch(Dispatchers.Default) {
            when (val result = SpreadsheetParser.parseCsvText(sampleCsv)) {
                is ParseResult.Success -> {
                    _currentDraft.value = result.draft
                    _importStatus.value = "Spreadsheet successfully extracted!"
                    _currentTab.value = AppTab.PREVIEW_EDIT
                }
                is ParseResult.Error -> {
                    _importStatus.value = result.message
                }
            }
            _isImporting.value = false
        }
    }

    fun parseStream(inputStream: InputStream, filename: String) {
        _isImporting.value = true
        _importStatus.value = "Reading and parsing $filename..."
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = SpreadsheetParser.parseStream(inputStream, filename)) {
                is ParseResult.Success -> {
                    _currentDraft.value = result.draft
                    _importStatus.value = "Parsed ${result.draft.items.size} line items from $filename"
                    _currentTab.value = AppTab.PREVIEW_EDIT
                }
                is ParseResult.Error -> {
                    _importStatus.value = result.message
                }
            }
            _isImporting.value = false
        }
    }

    fun updateDraft(updatedDraft: InvoiceDraft) {
        _currentDraft.value = updatedDraft
    }

    fun updateDraftTaxRate(rate: Double) {
        _currentDraft.value = _currentDraft.value?.copy(taxRate = rate)
    }

    fun updateDraftDiscount(discount: Double) {
        _currentDraft.value = _currentDraft.value?.copy(discountAmount = discount)
    }

    fun addDraftItem(name: String, description: String, qty: Double, price: Double) {
        val current = _currentDraft.value ?: return
        val newItem = InvoiceDraftItem(
            name = name.ifBlank { "New Item" },
            description = description,
            quantity = if (qty > 0) qty else 1.0,
            unitPrice = if (price >= 0) price else 0.0
        )
        _currentDraft.value = current.copy(items = current.items + newItem)
    }

    fun removeDraftItem(itemId: String) {
        val current = _currentDraft.value ?: return
        _currentDraft.value = current.copy(items = current.items.filter { it.id != itemId })
    }

    fun saveDraftToLedgerAndGeneratePdf(context: Context) {
        val originalDraft = _currentDraft.value ?: return
        val currentCreator = if (originalDraft.createdBy.isNotBlank()) originalDraft.createdBy else (_currentUser.value?.displayName ?: "Mansoor")
        val draft = originalDraft.copy(createdBy = currentCreator)
        _currentDraft.value = draft
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Generate PDF file
            val pdfFile = PdfInvoiceGenerator.generateInvoicePdf(context, draft)

            // 2. Save into Room DB
            val invoiceEntity = draft.toEntity(pdfPath = pdfFile.absolutePath)
            val items = draft.toItemEntities(0)
            repository.saveInvoiceWithItems(invoiceEntity, items)

            // 2b. Automatically push to live backend asynchronously
            try {
                backendClient.pushInvoiceToBackend(
                    invoiceNumber = draft.invoiceNumber,
                    issueDate = draft.issueDate,
                    dueDate = draft.dueDate,
                    clientName = draft.clientName,
                    clientEmail = draft.clientEmail,
                    clientAddress = draft.clientAddress,
                    companyName = draft.companyName,
                    subtotal = draft.subtotal,
                    taxRate = draft.taxRate,
                    taxAmount = draft.taxAmount,
                    discountAmount = draft.discountAmount,
                    grandTotal = draft.grandTotal,
                    amountPaid = draft.amountPaid,
                    balanceDue = draft.balanceDue,
                    status = draft.status,
                    paymentTerms = draft.paymentTerms,
                    items = draft.items.map { Pair(it.name, Pair(it.quantity, it.unitPrice)) }
                )
            } catch (e: Exception) {
                // Keep local invoice safe even if backend is offline
            }

            // 3. Set active PDF for viewing and switch tab
            _activePdf.value = Pair(draft, pdfFile)
            _currentTab.value = AppTab.PDF_VIEWER
        }
    }

    fun viewInvoicePdf(context: Context, invoiceWithItems: InvoiceWithItems) {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = invoiceWithItems.toDraft()
            val existingPath = invoiceWithItems.invoice.pdfFilePath
            val file = if (existingPath != null && File(existingPath).exists()) {
                File(existingPath)
            } else {
                val newFile = PdfInvoiceGenerator.generateInvoicePdf(context, draft)
                repository.updatePdfPath(invoiceWithItems.invoice.id, newFile.absolutePath)
                newFile
            }
            _activePdf.value = Pair(draft, file)
            _currentTab.value = AppTab.PDF_VIEWER
        }
    }

    fun deleteInvoice(id: Long) {
        viewModelScope.launch {
            repository.deleteInvoice(id)
        }
    }

    fun deleteAllInvoices() {
        _currentDraft.value = null
        _activePdf.value = null
        viewModelScope.launch {
            repository.deleteAllInvoices()
        }
    }

    fun markInvoiceAsPaid(id: Long, grandTotal: Double) {
        viewModelScope.launch {
            repository.updatePayment(id, amountPaid = grandTotal, balanceDue = 0.0, status = "PAID")
        }
    }

    fun updatePaymentAmount(id: Long, amountPaid: Double, grandTotal: Double) {
        viewModelScope.launch {
            val balance = Math.max(0.0, grandTotal - amountPaid)
            val status = if (balance <= 0.001) "PAID" else "PENDING"
            repository.updatePayment(id, amountPaid, balance, status)
        }
    }

    fun checkBackendHealth() {
        _backendStatus.value = "Pinging backend at ${backendClient.baseUrl}..."
        viewModelScope.launch {
            when (val res = backendClient.checkHealth()) {
                is ApiResult.Success -> _backendStatus.value = "Online! Response: ${res.data}"
                is ApiResult.Error -> _backendStatus.value = "Error: ${res.message}"
            }
        }
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun syncAllWithLiveBackend() {
        viewModelScope.launch {
            _isSyncing.value = true
            _backendStatus.value = "Connecting & syncing with live backend..."
            
            // 1. Fetch remote invoices and import any missing ones
            when (val fetchRes = backendClient.fetchInvoicesFromBackend()) {
                is ApiResult.Success -> {
                    val remoteList = fetchRes.data
                    val localInvoices = repository.allInvoicesWithItems.stateIn(viewModelScope).value
                    val localNumbers = localInvoices.map { it.invoice.invoiceNumber }.toSet()

                    var importedCount = 0
                    for (remote in remoteList) {
                        if (!localNumbers.contains(remote.invoiceNumber)) {
                            val draft = InvoiceDraft(
                                invoiceNumber = remote.invoiceNumber,
                                issueDate = remote.issueDate,
                                dueDate = remote.dueDate,
                                clientName = remote.clientName,
                                clientEmail = remote.clientEmail,
                                clientAddress = remote.clientAddress,
                                companyName = remote.companyName,
                                taxRate = remote.taxRate,
                                discountAmount = remote.discountAmount,
                                amountPaid = remote.amountPaid,
                                paymentTerms = remote.paymentTerms,
                                items = remote.items.map {
                                    InvoiceDraftItem(
                                        name = it.itemName,
                                        description = it.itemDescription,
                                        quantity = it.quantity,
                                        unitPrice = it.unitPrice
                                    )
                                }
                            )
                            val entity = draft.toEntity()
                            val items = draft.toItemEntities(0)
                            repository.saveInvoiceWithItems(entity, items)
                            importedCount++
                        }
                    }

                    // 2. Also push any local invoices not on server
                    val remoteNumbers = remoteList.map { it.invoiceNumber }.toSet()
                    var pushedCount = 0
                    for (loc in localInvoices) {
                        if (!remoteNumbers.contains(loc.invoice.invoiceNumber)) {
                            backendClient.pushInvoiceToBackend(
                                invoiceNumber = loc.invoice.invoiceNumber,
                                issueDate = loc.invoice.issueDate,
                                dueDate = loc.invoice.dueDate,
                                clientName = loc.invoice.clientName,
                                clientEmail = loc.invoice.clientEmail,
                                clientAddress = loc.invoice.clientAddress,
                                companyName = loc.invoice.companyName,
                                subtotal = loc.invoice.subtotal,
                                taxRate = loc.invoice.taxRate,
                                taxAmount = loc.invoice.taxAmount,
                                discountAmount = loc.invoice.discountAmount,
                                grandTotal = loc.invoice.grandTotal,
                                amountPaid = loc.invoice.amountPaid,
                                balanceDue = loc.invoice.balanceDue,
                                status = loc.invoice.status,
                                paymentTerms = loc.invoice.paymentTerms,
                                items = loc.items.map { Pair(it.itemName, Pair(it.quantity, it.unitPrice)) }
                            )
                            pushedCount++
                        }
                    }

                    _backendStatus.value = "Sync Complete! Remote: ${remoteList.size} bills. Imported: $importedCount new, Pushed: $pushedCount."
                }
                is ApiResult.Error -> {
                    _backendStatus.value = "Backend Sync Failed: ${fetchRes.message}"
                }
            }
            _isSyncing.value = false
        }
    }

    class Factory(
        private val repository: InvoiceRepository,
        private val userRepository: UserRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InvoiceViewModel(repository, userRepository) as T
        }
    }
}

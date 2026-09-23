import Foundation

public struct InvoiceItem: Identifiable, Codable, Equatable {
    public var id: Int?
    public var itemName: String
    public var itemDescription: String
    public var quantity: Double
    public var unitPrice: Double
    public var itemTotal: Double

    public init(
        id: Int? = nil,
        itemName: String = "",
        itemDescription: String = "",
        quantity: Double = 1.0,
        unitPrice: Double = 0.0,
        itemTotal: Double = 0.0
    ) {
        self.id = id
        self.itemName = itemName
        self.itemDescription = itemDescription
        self.quantity = quantity
        self.unitPrice = unitPrice
        self.itemTotal = itemTotal
    }

    enum CodingKeys: String, CodingKey {
        case id
        case itemName = "item_name"
        case itemDescription = "item_description"
        case quantity
        case unitPrice = "unit_price"
        case itemTotal = "item_total"
    }
}

public struct Invoice: Identifiable, Codable, Equatable {
    public var id: Int?
    public var invoiceNumber: String
    public var issueDate: String
    public var dueDate: String
    public var clientName: String
    public var clientEmail: String?
    public var clientAddress: String?
    public var companyName: String
    public var companyEmail: String?
    public var companyAddress: String?
    public var templateType: String
    public var subtotal: Double
    public var taxRate: Double
    public var taxAmount: Double
    public var discountAmount: Double
    public var grandTotal: Double
    public var amountPaid: Double
    public var balanceDue: Double
    public var status: String
    public var paymentTerms: String
    public var items: [InvoiceItem]

    public init(
        id: Int? = nil,
        invoiceNumber: String = "INV-2026-001",
        issueDate: String = "2026-09-22",
        dueDate: String = "2026-09-22",
        clientName: String = "",
        clientEmail: String? = nil,
        clientAddress: String? = nil,
        companyName: String = "Master Tech.",
        companyEmail: String? = "sales@mastertech.com",
        companyAddress: String? = "Saddar Rawalpindi, Computer Market",
        templateType: String = "LAYOUT_2",
        subtotal: Double = 0.0,
        taxRate: Double = 8.5,
        taxAmount: Double = 0.0,
        discountAmount: Double = 0.0,
        grandTotal: Double = 0.0,
        amountPaid: Double = 0.0,
        balanceDue: Double = 0.0,
        status: String = "PENDING",
        paymentTerms: String = "Due on receipt",
        items: [InvoiceItem] = []
    ) {
        self.id = id
        self.invoiceNumber = invoiceNumber
        self.issueDate = issueDate
        self.dueDate = dueDate
        self.clientName = clientName
        self.clientEmail = clientEmail
        self.clientAddress = clientAddress
        self.companyName = companyName
        self.companyEmail = companyEmail
        self.companyAddress = companyAddress
        self.templateType = templateType
        self.subtotal = subtotal
        self.taxRate = taxRate
        self.taxAmount = taxAmount
        self.discountAmount = discountAmount
        self.grandTotal = grandTotal
        self.amountPaid = amountPaid
        self.balanceDue = balanceDue
        self.status = status
        self.paymentTerms = paymentTerms
        self.items = items
    }

    enum CodingKeys: String, CodingKey {
        case id
        case invoiceNumber = "invoice_number"
        case issueDate = "issue_date"
        case dueDate = "due_date"
        case clientName = "client_name"
        case clientEmail = "client_email"
        case clientAddress = "client_address"
        case companyName = "company_name"
        case companyEmail = "company_email"
        case companyAddress = "company_address"
        case templateType = "template_type"
        case subtotal
        case taxRate = "tax_rate"
        case taxAmount = "tax_amount"
        case discountAmount = "discount_amount"
        case grandTotal = "grand_total"
        case amountPaid = "amount_paid"
        case balanceDue = "balance_due"
        case status
        case paymentTerms = "payment_terms"
        case items
    }
}

public struct LedgerMetrics: Codable {
    public var totalBilled: Double
    public var totalCollected: Double
    public var totalOutstanding: Double

    enum CodingKeys: String, CodingKey {
        case totalBilled = "total_billed"
        case totalCollected = "total_collected"
        case totalOutstanding = "total_outstanding"
    }
}


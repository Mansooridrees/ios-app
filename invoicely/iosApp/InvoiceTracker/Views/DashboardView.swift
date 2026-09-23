import SwiftUI

public struct DashboardView: View {
    @Binding var activeInvoice: Invoice?
    @Binding var selectedTab: Int

    @State private var invoices: [Invoice] = []
    @State private var metrics: LedgerMetrics? = nil
    @State private var searchText: String = ""
    @State private var selectedFilter: String = "ALL"
    @State private var isLoading: Bool = false

    public var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // Metrics Cards
                HStack(spacing: 12) {
                    MetricCard(
                        title: "TOTAL BILLED",
                        value: String(format: "Rs. %.2f", metrics?.totalBilled ?? 0.0),
                        color: .blue
                    )
                    MetricCard(
                        title: "COLLECTED",
                        value: String(format: "Rs. %.2f", metrics?.totalCollected ?? 0.0),
                        color: .green
                    )
                    MetricCard(
                        title: "OUTSTANDING",
                        value: String(format: "Rs. %.2f", metrics?.totalOutstanding ?? 0.0),
                        color: .orange
                    )
                }
                .padding(.horizontal)

                // Search & Filter
                VStack(spacing: 8) {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.secondary)
                        TextField("Search client or invoice #...", text: $searchText)
                    }
                    .padding(10)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(10)
                    .padding(.horizontal)

                    HStack(spacing: 8) {
                        ForEach(["ALL", "PENDING", "PAID"], id: \.self) { filter in
                            Button(action: { selectedFilter = filter }) {
                                Text(filter)
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .padding(.vertical, 6)
                                    .padding(.horizontal, 14)
                                    .background(selectedFilter == filter ? Color(red: 2/255, green: 132/255, blue: 199/255) : Color(.secondarySystemBackground))
                                    .foregroundColor(selectedFilter == filter ? .white : .primary)
                                    .cornerRadius(16)
                            }
                        }
                        Spacer()
                    }
                    .padding(.horizontal)
                }

                // Invoices List
                if filteredInvoices.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "tray")
                            .font(.system(size: 40))
                            .foregroundColor(.secondary)
                        Text("No invoices found")
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 40)
                } else {
                    LazyVStack(spacing: 12) {
                        ForEach(filteredInvoices) { inv in
                            InvoiceCardView(invoice: inv) {
                                activeInvoice = inv
                                selectedTab = 2 // Switch to Draft / Preview
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .padding(.vertical)
        }
        .onAppear {
            loadData()
        }
        .refreshable {
            loadData()
        }
    }

    private var filteredInvoices: [Invoice] {
        invoices.filter { inv in
            let matchesFilter = selectedFilter == "ALL" || inv.status.uppercased() == selectedFilter
            let matchesSearch = searchText.isEmpty ||
                inv.clientName.localizedCaseInsensitiveContains(searchText) ||
                inv.invoiceNumber.localizedCaseInsensitiveContains(searchText)
            return matchesFilter && matchesSearch
        }
    }

    private func loadData() {
        BackendService.shared.fetchMetrics { m in
            self.metrics = m
        }
        BackendService.shared.fetchInvoices { list in
            if let list = list, !list.isEmpty {
                self.invoices = list
            } else if self.invoices.isEmpty {
                // Fallback default sample invoice
                self.invoices = [
                    Invoice(
                        id: 1,
                        invoiceNumber: "INV-2026-1042",
                        issueDate: "2026-09-22",
                        clientName: "Apex Global Logistics",
                        companyName: "Master Tech.",
                        grandTotal: 11640.88,
                        status: "PENDING",
                        items: [
                            InvoiceItem(id: 1, itemName: "Motherboard Replacement", quantity: 1, unitPrice: 8500, itemTotal: 8500),
                            InvoiceItem(id: 2, itemName: "RAM DDR4 8GB", quantity: 2, unitPrice: 1500, itemTotal: 3000)
                        ]
                    )
                ]
            }
        }
    }
}

struct MetricCard: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(.secondary)
            Text(value)
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(color)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color(.secondarySystemBackground))
        .cornerRadius(10)
    }
}

struct InvoiceCardView: View {
    let invoice: Invoice
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(invoice.invoiceNumber)
                        .font(.headline)
                        .foregroundColor(.primary)
                    Spacer()
                    Text(invoice.status)
                        .font(.caption2)
                        .fontWeight(.bold)
                        .padding(.vertical, 4)
                        .padding(.horizontal, 8)
                        .background(invoice.status == "PAID" ? Color.green.opacity(0.15) : Color.orange.opacity(0.15))
                        .foregroundColor(invoice.status == "PAID" ? .green : .orange)
                        .cornerRadius(6)
                }

                Text("\(invoice.clientName) • \(invoice.issueDate)")
                    .font(.subheadline)
                    .foregroundColor(.secondary)

                HStack {
                    Text("Total Due:")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text(String(format: "Rs. %.2f", invoice.grandTotal))
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(Color(red: 2/255, green: 132/255, blue: 199/255))
                }
            }
            .padding(14)
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(12)
            .shadow(color: Color.black.opacity(0.04), radius: 4, x: 0, y: 2)
        }
    }
}


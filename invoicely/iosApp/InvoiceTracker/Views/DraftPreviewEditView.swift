import SwiftUI

public struct DraftPreviewEditView: View {
    @Binding var invoice: Invoice?
    @Binding var selectedTab: Int

    public var body: some View {
        ScrollView {
            if let inv = invoice {
                VStack(alignment: .leading, spacing: 16) {
                    // Top Receipt Card
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(inv.companyName)
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(Color(red: 2/255, green: 132/255, blue: 199/255))
                                Text(inv.companyAddress ?? "Computer Market, Rawalpindi")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            VStack(alignment: .trailing) {
                                Text("Sales Receipt")
                                    .font(.headline)
                                Text("# \(inv.invoiceNumber)")
                                    .font(.caption)
                                Text("Date: \(inv.issueDate)")
                                    .font(.caption)
                            }
                        }

                        Divider()

                        Text("Sold To: \(inv.clientName)")
                            .font(.subheadline)
                            .fontWeight(.semibold)

                        // Items Table
                        VStack(spacing: 8) {
                            HStack {
                                Text("Description").font(.caption).bold()
                                Spacer()
                                Text("Qty").font(.caption).bold().frame(width: 40)
                                Text("Rate").font(.caption).bold().frame(width: 60)
                                Text("Amount").font(.caption).bold().frame(width: 70)
                            }
                            Divider()

                            ForEach(inv.items) { item in
                                HStack {
                                    Text(item.itemName).font(.caption)
                                    Spacer()
                                    Text(String(format: "%.0f", item.quantity)).font(.caption).frame(width: 40)
                                    Text(String(format: "%.2f", item.unitPrice)).font(.caption).frame(width: 60)
                                    Text(String(format: "%.2f", item.itemTotal)).font(.caption).frame(width: 70)
                                }
                                Divider()
                            }
                        }

                        // Summary
                        VStack(alignment: .trailing, spacing: 4) {
                            HStack {
                                Spacer()
                                Text("Subtotal: Rs. \(String(format: "%.2f", inv.subtotal))")
                                    .font(.caption)
                            }
                            HStack {
                                Spacer()
                                Text("Tax: Rs. \(String(format: "%.2f", inv.taxAmount))")
                                    .font(.caption)
                            }
                            HStack {
                                Spacer()
                                Text("Grand Total: Rs. \(String(format: "%.2f", inv.grandTotal))")
                                    .font(.headline)
                                    .foregroundColor(Color(red: 2/255, green: 132/255, blue: 199/255))
                            }
                        }
                        .padding(.top, 8)
                    }
                    .padding()
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 4, x: 0, y: 2)

                    // Action Buttons
                    HStack(spacing: 12) {
                        Button(action: {
                            selectedTab = 3 // Move to PDF tab
                        }) {
                            HStack {
                                Image(systemName: "doc.fill")
                                Text("View Official PDF")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(red: 2/255, green: 132/255, blue: 199/255))
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                    }
                }
                .padding()
            } else {
                VStack(spacing: 16) {
                    Image(systemName: "doc.badge.plus")
                        .font(.system(size: 50))
                        .foregroundColor(.secondary)
                    Text("No active draft loaded")
                        .foregroundColor(.secondary)
                    Button("Create New Bill") {
                        selectedTab = 1
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                }
                .padding(.top, 60)
            }
        }
    }
}


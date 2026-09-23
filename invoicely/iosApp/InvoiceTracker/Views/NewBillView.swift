import SwiftUI

public struct NewBillView: View {
    @Binding var activeInvoice: Invoice?
    @Binding var selectedTab: Int

    @State private var clientName: String = ""
    @State private var invoiceNumber: String = "INV-\(Int.random(in: 10000...99999))"
    @State private var issueDate: Date = Date()
    @State private var templateType: String = "LAYOUT_2"
    @State private var items: [InvoiceItem] = [
        InvoiceItem(itemName: "Computer Service", quantity: 1, unitPrice: 1500, itemTotal: 1500)
    ]
    @StateObject private var speechRecognizer = SpeechRecognizer()
    @State private var showVoiceSheet: Bool = false

    public var body: some View {
        Form {
            Section(header: Text("RECEIPT DETAILS")) {
                TextField("Customer Name", text: $clientName)
                TextField("Invoice #", text: $invoiceNumber)
                DatePicker("Date", selection: $issueDate, displayedComponents: .date)

                Picker("Template Layout", selection: $templateType) {
                    Text("Master Tech (Rawalpindi)").tag("LAYOUT_2")
                    Text("XP Computers (Classic)").tag("LAYOUT_1")
                    Text("Master Tools (Modern)").tag("MODERN")
                }
            }

            Section(header: HStack {
                Text("LINE ITEMS")
                Spacer()
                Button(action: {
                    items.append(InvoiceItem(itemName: "", quantity: 1, unitPrice: 0, itemTotal: 0))
                }) {
                    Text("+ Add Item").font(.caption).bold()
                }
            }) {
                ForEach(items.indices, id: \.self) { idx in
                    VStack(alignment: .leading, spacing: 6) {
                        TextField("Item Description", text: $items[idx].itemName)
                        HStack {
                            TextField("Qty", value: $items[idx].quantity, format: .number)
                                .keyboardType(.decimalPad)
                                .textFieldStyle(.roundedBorder)
                                .onChange(of: items[idx].quantity) { _ in
                                    recalculate(idx: idx)
                                }

                            TextField("Price", value: $items[idx].unitPrice, format: .number)
                                .keyboardType(.decimalPad)
                                .textFieldStyle(.roundedBorder)
                                .onChange(of: items[idx].unitPrice) { _ in
                                    recalculate(idx: idx)
                                }

                            Text(String(format: "Rs. %.2f", items[idx].itemTotal))
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                }
                .onDelete { indexSet in
                    if items.count > 1 {
                        items.remove(atOffsets: indexSet)
                    }
                }
            }

            Section(header: Text("TOTAL SUMMARY")) {
                HStack {
                    Text("Subtotal:")
                    Spacer()
                    Text(String(format: "Rs. %.2f", subtotal))
                }
                HStack {
                    Text("Tax (8.5%):")
                    Spacer()
                    Text(String(format: "Rs. %.2f", taxAmount))
                }
                HStack {
                    Text("Grand Total:").fontWeight(.bold)
                    Spacer()
                    Text(String(format: "Rs. %.2f", grandTotal))
                        .fontWeight(.bold)
                        .foregroundColor(Color(red: 2/255, green: 132/255, blue: 199/255))
                }
            }

            Section {
                Button(action: { showVoiceSheet = true }) {
                    HStack {
                        Image(systemName: "mic.fill")
                        Text("Voice Dictation Input")
                    }
                    .foregroundColor(.blue)
                }

                Button(action: saveAndPreview) {
                    HStack {
                        Spacer()
                        Text("Save & Preview Invoice")
                            .fontWeight(.bold)
                        Spacer()
                    }
                }
                .foregroundColor(.white)
                .listRowBackground(Color(red: 2/255, green: 132/255, blue: 199/255))
            }
        }
        .sheet(isPresented: $showVoiceSheet) {
            VoiceDictationSheet(speechRecognizer: speechRecognizer) { transcript in
                parseSpeechIntoBill(transcript)
            }
        }
    }

    private var subtotal: Double {
        items.reduce(0.0) { $0 + $1.itemTotal }
    }

    private var taxAmount: Double {
        subtotal * 0.085
    }

    private var grandTotal: Double {
        subtotal + taxAmount
    }

    private func recalculate(idx: Int) {
        items[idx].itemTotal = items[idx].quantity * items[idx].unitPrice
    }

    private func saveAndPreview() {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let dateStr = formatter.string(from: issueDate)

        let companyName = templateType == "MODERN" ? "MASTER TOOLS" :
            (templateType == "LAYOUT_2" ? "Master Tech." : "XP Computers")

        let newInvoice = Invoice(
            invoiceNumber: invoiceNumber,
            issueDate: dateStr,
            dueDate: dateStr,
            clientName: clientName.isEmpty ? "Cash Customer" : clientName,
            companyName: companyName,
            templateType: templateType,
            subtotal: subtotal,
            taxRate: 8.5,
            taxAmount: taxAmount,
            discountAmount: 0.0,
            grandTotal: grandTotal,
            amountPaid: 0.0,
            balanceDue: grandTotal,
            status: "PENDING",
            items: items
        )

        activeInvoice = newInvoice
        BackendService.shared.saveInvoice(newInvoice) { saved in
            if let saved = saved {
                self.activeInvoice = saved
            }
        }
        selectedTab = 2 // Move to Draft / Preview tab
    }

    private func parseSpeechIntoBill(_ text: String) {
        let lower = text.lowercased()
        if lower.contains("customer") {
            let parts = lower.components(separatedBy: "customer")
            if let target = parts.last {
                clientName = target.components(separatedBy: "item").first?.trimmingCharacters(in: .whitespacesAndNewlines).capitalized ?? ""
            }
        }
        if lower.contains("item") {
            items.append(InvoiceItem(itemName: "Speech Item", quantity: 1, unitPrice: 1000, itemTotal: 1000))
        }
    }
}

struct VoiceDictationSheet: View {
    @ObservedObject var speechRecognizer: SpeechRecognizer
    let onDone: (String) -> Void
    @Environment(\.presentationMode) var presentationMode

    var body: some View {
        VStack(spacing: 20) {
            Text("Voice to Invoice")
                .font(.headline)
                .padding(.top)

            Text(speechRecognizer.isRecording ? "Listening... Speak customer and item details" : "Tap microphone to record")
                .font(.subheadline)
                .foregroundColor(.secondary)

            ZStack {
                Circle()
                    .fill(speechRecognizer.isRecording ? Color.red.opacity(0.2) : Color.blue.opacity(0.1))
                    .frame(width: 100, height: 100)

                Button(action: {
                    speechRecognizer.toggleRecording()
                }) {
                    Image(systemName: speechRecognizer.isRecording ? "stop.fill" : "mic.fill")
                        .font(.system(size: 36))
                        .foregroundColor(speechRecognizer.isRecording ? .red : .blue)
                }
            }

            Text(speechRecognizer.transcript.isEmpty ? "(Transcript will appear here)" : speechRecognizer.transcript)
                .padding()
                .frame(maxWidth: .infinity, minHeight: 80)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(10)
                .padding(.horizontal)

            Button(action: {
                speechRecognizer.stopRecording()
                onDone(speechRecognizer.transcript)
                presentationMode.wrappedValue.dismiss()
            }) {
                Text("Insert into Bill")
                    .fontWeight(.bold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .padding(.horizontal)

            Spacer()
        }
    }
}


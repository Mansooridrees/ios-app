import SwiftUI

public struct BackendConfigView: View {
    @StateObject private var backend = BackendService.shared
    @State private var serverUrl: String = "http://localhost:3000"
    @State private var healthStatus: String = "Checking..."
    @State private var isOnline: Bool = false

    public var body: some View {
        Form {
            Section(header: Text("BACKEND API CONFIGURATION")) {
                TextField("Server URL", text: $serverUrl)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)

                HStack {
                    Text("Connection Status:")
                    Spacer()
                    Text(healthStatus)
                        .font(.caption)
                        .bold()
                        .foregroundColor(isOnline ? .green : .red)
                }

                Button("Test Connection") {
                    backend.baseUrl = serverUrl
                    backend.checkHealth { success in
                        isOnline = success
                        healthStatus = success ? "Online (Connected)" : "Offline (Failed)"
                    }
                }
            }

            Section(header: Text("SYNC INVOICES")) {
                Button("Sync Ledger with Backend") {
                    backend.fetchInvoices { list in
                        if list != nil {
                            healthStatus = "Synced successfully!"
                        }
                    }
                }
            }
        }
        .onAppear {
            serverUrl = backend.baseUrl
            backend.checkHealth { success in
                isOnline = success
                healthStatus = success ? "Online (Connected)" : "Offline"
            }
        }
    }
}


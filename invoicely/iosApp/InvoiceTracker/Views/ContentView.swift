import SwiftUI

public struct ContentView: View {
    @State private var selectedTab: Int = 0
    @State private var activeInvoice: Invoice? = nil
    @State private var isDarkMode: Bool = false
    @State private var showUserSheet: Bool = false
    @StateObject private var backendService = BackendService.shared

    public init() {}

    public var body: some View {
        NavigationView {
            TabView(selection: $selectedTab) {
                DashboardView(activeInvoice: $activeInvoice, selectedTab: $selectedTab)
                    .tabItem {
                        Label("Ledger", systemImage: "clock.arrow.circlepath")
                    }
                    .tag(0)

                NewBillView(activeInvoice: $activeInvoice, selectedTab: $selectedTab)
                    .tabItem {
                        Label("New Bill", systemImage: "plus.circle.fill")
                    }
                    .tag(1)

                DraftPreviewEditView(invoice: $activeInvoice, selectedTab: $selectedTab)
                    .tabItem {
                        Label("Draft", systemImage: "doc.text.fill")
                    }
                    .tag(2)

                PDFViewerView(invoice: $activeInvoice)
                    .tabItem {
                        Label("PDF", systemImage: "arrow.down.doc.fill")
                    }
                    .tag(3)

                BackendConfigView()
                    .tabItem {
                        Label("Backend", systemImage: "network")
                    }
                    .tag(4)
            }
            .accentColor(Color(red: 2/255, green: 132/255, blue: 199/255))
            .navigationTitle("Invoice Tracker")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    HStack(spacing: 8) {
                        Image(systemName: "wrench.and.screwdriver.fill")
                            .foregroundColor(Color(red: 2/255, green: 132/255, blue: 199/255))
                        Text("Sales & Invoicing")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        Button(action: { isDarkMode.toggle() }) {
                            Image(systemName: isDarkMode ? "moon.fill" : "sun.max.fill")
                                .foregroundColor(Color(red: 2/255, green: 132/255, blue: 199/255))
                        }
                        Button(action: { showUserSheet.toggle() }) {
                            Image(systemName: "person.circle")
                                .foregroundColor(Color(red: 2/255, green: 132/255, blue: 199/255))
                        }
                    }
                }
            }
            .sheet(isPresented: $showUserSheet) {
                ManageUsersView()
            }
        }
        .preferredColorScheme(isDarkMode ? .dark : .light)
    }
}


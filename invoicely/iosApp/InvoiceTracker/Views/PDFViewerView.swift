import SwiftUI
import PDFKit

public struct PDFViewerView: View {
    @Binding var invoice: Invoice?
    @State private var pdfData: Data? = nil
    @State private var showShareSheet: Bool = false

    public var body: some View {
        VStack(spacing: 0) {
            if let data = pdfData, let document = PDFDocument(data: data) {
                PDFKitRepresentedView(document: document)
                    .edgesIgnoringSafeArea(.all)

                HStack(spacing: 16) {
                    Button(action: { showShareSheet = true }) {
                        HStack {
                            Image(systemName: "square.and.arrow.up")
                            Text("Share / Export PDF")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(red: 2/255, green: 132/255, blue: 199/255))
                        .foregroundColor(.white)
                        .cornerRadius(10)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
            } else {
                VStack(spacing: 16) {
                    Image(systemName: "arrow.down.doc")
                        .font(.system(size: 40))
                        .foregroundColor(.secondary)
                    Text("Select or create an invoice to view its PDF")
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .onAppear {
            generatePDF()
        }
        .onChange(of: invoice) { _ in
            generatePDF()
        }
        .sheet(isPresented: $showShareSheet) {
            if let data = pdfData {
                ShareActivityView(activityItems: [data])
            }
        }
    }

    private func generatePDF() {
        guard let inv = invoice else { return }
        pdfData = PDFManager.shared.generatePDF(for: inv)
    }
}

struct PDFKitRepresentedView: UIViewRepresentable {
    let document: PDFDocument

    func makeUIView(context: Context) -> PDFView {
        let pdfView = PDFView()
        pdfView.document = document
        pdfView.autoScales = true
        return pdfView
    }

    func updateUIView(_ uiView: PDFView, context: Context) {
        uiView.document = document
    }
}

struct ShareActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}


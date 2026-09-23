import UIKit
import PDFKit

public class PDFManager {
    public static let shared = PDFManager()

    private init() {}

    public func generatePDF(for invoice: Invoice) -> Data {
        let pageWidth: CGFloat = 595.2 // A4 standard width in points
        let pageHeight: CGFloat = 841.8 // A4 standard height in points
        let pageRect = CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)

        let renderer = UIGraphicsPDFRenderer(bounds: pageRect)

        let data = renderer.pdfData { context in
            context.beginPage()

            // Header Accent Line
            let primaryColor = UIColor(red: 2/255, green: 132/255, blue: 199/255, alpha: 1.0)
            primaryColor.setFill()
            UIRectFill(CGRect(x: 0, y: 0, width: pageWidth, height: 10))

            // Company Title
            let titleFont = UIFont.boldSystemFont(ofSize: 22)
            let titleAttributes: [NSAttributedString.Key: Any] = [
                .font: titleFont,
                .foregroundColor: primaryColor
            ]
            let companyText = invoice.companyName.isEmpty ? "Master Tech." : invoice.companyName
            companyText.draw(at: CGPoint(x: 40, y: 40), withAttributes: titleAttributes)

            // Address & Contacts
            let subFont = UIFont.systemFont(ofSize: 9)
            let subAttributes: [NSAttributedString.Key: Any] = [
                .font: subFont,
                .foregroundColor: UIColor.darkGray
            ]
            let address = invoice.companyAddress ?? "SADDAR RAWALPINDI, COMPUTER MARKET"
            address.draw(at: CGPoint(x: 40, y: 68), withAttributes: subAttributes)

            // Invoice Title & Info (Top Right)
            let invHeaderAttr: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 24),
                .foregroundColor: UIColor.black
            ]
            "Sales Receipt".draw(at: CGPoint(x: 380, y: 40), withAttributes: invHeaderAttr)

            let metaAttr: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 11),
                .foregroundColor: UIColor.black
            ]
            "Date: \(invoice.issueDate)".draw(at: CGPoint(x: 380, y: 75), withAttributes: metaAttr)
            "No.:  \(invoice.invoiceNumber)".draw(at: CGPoint(x: 380, y: 95), withAttributes: metaAttr)

            // Customer Name
            let custAttr: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 14),
                .foregroundColor: UIColor.black
            ]
            "Sold To: \(invoice.clientName)".draw(at: CGPoint(x: 40, y: 150), withAttributes: custAttr)

            // Table Border
            let tableTop: CGFloat = 190
            let tableBottom: CGFloat = 650
            let tableWidth: CGFloat = pageWidth - 80

            let borderPath = UIBezierPath(rect: CGRect(x: 40, y: tableTop, width: tableWidth, height: tableBottom - tableTop))
            UIColor.black.setStroke()
            borderPath.lineWidth = 1.5
            borderPath.stroke()

            // Header line
            let headerLine = UIBezierPath()
            headerLine.move(to: CGPoint(x: 40, y: tableTop + 25))
            headerLine.addLine(to: CGPoint(x: 40 + tableWidth, y: tableTop + 25))
            headerLine.stroke()

            // Table Headers
            let thAttr: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 11),
                .foregroundColor: UIColor.black
            ]
            "Description".draw(at: CGPoint(x: 48, y: tableTop + 6), withAttributes: thAttr)
            "Qty".draw(at: CGPoint(x: 340, y: tableTop + 6), withAttributes: thAttr)
            "Rate".draw(at: CGPoint(x: 410, y: tableTop + 6), withAttributes: thAttr)
            "Amount".draw(at: CGPoint(x: 480, y: tableTop + 6), withAttributes: thAttr)

            // Items Rows
            var rowY: CGFloat = tableTop + 32
            for item in invoice.items {
                let itemAttr: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: 10),
                    .foregroundColor: UIColor.black
                ]
                item.itemName.draw(at: CGPoint(x: 48, y: rowY), withAttributes: itemAttr)
                String(format: "%.0f", item.quantity).draw(at: CGPoint(x: 345, y: rowY), withAttributes: itemAttr)
                String(format: "%.2f", item.unitPrice).draw(at: CGPoint(x: 410, y: rowY), withAttributes: itemAttr)
                String(format: "%.2f", item.itemTotal).draw(at: CGPoint(x: 480, y: rowY), withAttributes: itemAttr)

                let divPath = UIBezierPath()
                divPath.move(to: CGPoint(x: 40, y: rowY + 16))
                divPath.addLine(to: CGPoint(x: 40 + tableWidth, y: rowY + 16))
                UIColor.lightGray.setStroke()
                divPath.lineWidth = 0.5
                divPath.stroke()

                rowY += 22
            }

            // Total Box
            let totalAttr: [NSAttributedString.Key: Any] = [
                .font: UIFont.boldSystemFont(ofSize: 14),
                .foregroundColor: UIColor.black
            ]
            let totalStr = String(format: "Total: Rs. %.2f", invoice.grandTotal)
            totalStr.draw(at: CGPoint(x: 380, y: tableBottom + 12), withAttributes: totalAttr)
        }

        return data
    }
}


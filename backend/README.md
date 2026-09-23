# Invoice Generation & Tracking Backend (Node.js)

A production-ready Node.js & Express service that parses Excel (`.xlsx`) and CSV (`.csv`) spreadsheets, executes financial calculations, records billing data into an SQLite ledger, and renders print-ready PDF invoices.

---

## 🚀 Step-by-Step Setup

### 1. Prerequisites
- Node.js (v18+ recommended)
- npm or yarn

### 2. Installation
Navigate to the `backend` folder and install dependencies:
```bash
cd backend
npm install
```

### 3. Dependencies in `package.json`:
- `express`: Fast, unopinionated HTTP web framework
- `multer`: Multipart/form-data handler for spreadsheet uploads
- `xlsx`: Robust spreadsheet parser for `.xlsx`, `.xls`, and `.csv`
- `pdfkit`: High-performance PDF generation engine for modern invoices
- `better-sqlite3`: Zero-config, ACID-compliant local SQL database engine for ledger tracking
- `cors`: Cross-Origin Resource Sharing middleware

### 4. Running the Server
```bash
# Start in production mode
npm start

# Or develop with auto-restart
npm run dev
```
The server will start listening on port `3000` (or `process.env.PORT`).

---

## 📡 API Endpoints

### 1. Upload & Parse Spreadsheet
- **Endpoint**: `POST /api/upload-excel`
- **Content-Type**: `multipart/form-data`
- **Body**: `file: <your-file.xlsx | your-file.csv>`
- **Response**:
```json
{
  "message": "Invoice parsed, calculated, and saved to ledger successfully",
  "invoice": {
    "id": 1,
    "invoice_number": "INV-2026-1042",
    "issue_date": "2026-09-01",
    "due_date": "2026-09-20",
    "client_name": "Apex Global Logistics Inc.",
    "subtotal": 10775.00,
    "tax_rate": 8.5,
    "tax_amount": 915.88,
    "discount_amount": 50.00,
    "grand_total": 11640.88,
    "amount_paid": 0.00,
    "balance_due": 11640.88,
    "status": "PENDING",
    "items": [ ... ]
  }
}
```

### 2. View Ledger / Historical Invoices
- **Endpoint**: `GET /api/invoices`
- **Response**: Array of all processed invoices and line items.

### 3. Generate & Download PDF
- **Endpoint**: `GET /api/invoices/:id/pdf`
- **Response**: Print-ready `application/pdf` stream with company header, client details, line items table, financial summary box, and bank payment instructions.

### 4. Update Payment & Balance
- **Endpoint**: `PATCH /api/invoices/:id/payment`
- **Body**: `{ "amount_paid": 5000.00 }`
- **Response**: Updated invoice with recalculated `balance_due` and `status`.

### 5. Summary Metrics
- **Endpoint**: `GET /api/metrics`
- **Response**: Total billed volume, total collected, and outstanding balances across all clients.

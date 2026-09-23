const Database = require('better-sqlite3');
const path = require('path');

const dbPath = path.join(__dirname, 'invoices.db');
const db = new Database(dbPath);

// Enable WAL mode for high performance
db.pragma('journal_mode = WAL');

// Initialize schema
db.exec(`
  CREATE TABLE IF NOT EXISTS invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_number TEXT UNIQUE NOT NULL,
    issue_date TEXT NOT NULL,
    due_date TEXT NOT NULL,
    client_name TEXT NOT NULL,
    client_email TEXT,
    client_address TEXT,
    company_name TEXT DEFAULT 'Acme Solutions Inc.',
    company_email TEXT DEFAULT 'billing@acme.com',
    company_address TEXT DEFAULT '100 Enterprise Way, Suite 400',
    subtotal REAL NOT NULL DEFAULT 0.0,
    tax_rate REAL NOT NULL DEFAULT 0.0,
    tax_amount REAL NOT NULL DEFAULT 0.0,
    discount_amount REAL NOT NULL DEFAULT 0.0,
    grand_total REAL NOT NULL DEFAULT 0.0,
    amount_paid REAL NOT NULL DEFAULT 0.0,
    balance_due REAL NOT NULL DEFAULT 0.0,
    status TEXT NOT NULL DEFAULT 'PENDING',
    payment_terms TEXT DEFAULT 'Payment due within 15 days of invoice date.',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  CREATE TABLE IF NOT EXISTS invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL,
    item_name TEXT NOT NULL,
    item_description TEXT,
    quantity REAL NOT NULL,
    unit_price REAL NOT NULL,
    item_total REAL NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
  );

  CREATE INDEX IF NOT EXISTS idx_invoice_number ON invoices(invoice_number);
  CREATE INDEX IF NOT EXISTS idx_invoice_items_inv_id ON invoice_items(invoice_id);
`);

// Prepared Statements
const insertInvoiceStmt = db.prepare(`
  INSERT INTO invoices (
    invoice_number, issue_date, due_date, client_name, client_email, client_address,
    company_name, company_email, company_address,
    subtotal, tax_rate, tax_amount, discount_amount, grand_total,
    amount_paid, balance_due, status, payment_terms
  ) VALUES (
    @invoice_number, @issue_date, @due_date, @client_name, @client_email, @client_address,
    @company_name, @company_email, @company_address,
    @subtotal, @tax_rate, @tax_amount, @discount_amount, @grand_total,
    @amount_paid, @balance_due, @status, @payment_terms
  )
`);

const insertItemStmt = db.prepare(`
  INSERT INTO invoice_items (
    invoice_id, item_name, item_description, quantity, unit_price, item_total
  ) VALUES (
    @invoice_id, @item_name, @item_description, @quantity, @unit_price, @item_total
  )
`);

const getAllInvoicesStmt = db.prepare(`
  SELECT * FROM invoices ORDER BY id DESC
`);

const getInvoiceByIdStmt = db.prepare(`
  SELECT * FROM invoices WHERE id = ?
`);

const getInvoiceByNumberStmt = db.prepare(`
  SELECT * FROM invoices WHERE invoice_number = ?
`);

const getItemsForInvoiceStmt = db.prepare(`
  SELECT * FROM invoice_items WHERE invoice_id = ?
`);

const updatePaymentStmt = db.prepare(`
  UPDATE invoices 
  SET amount_paid = @amount_paid,
      balance_due = @balance_due,
      status = @status
  WHERE id = @id
`);

const getSummaryMetricsStmt = db.prepare(`
  SELECT 
    COUNT(*) as total_invoices,
    COALESCE(SUM(grand_total), 0) as total_billed,
    COALESCE(SUM(amount_paid), 0) as total_collected,
    COALESCE(SUM(balance_due), 0) as total_pending
  FROM invoices
`);

module.exports = {
  db,
  insertInvoice: (invoiceData, items) => {
    const transaction = db.transaction(() => {
      // Check if invoice_number already exists, append suffix if so
      let invNum = invoiceData.invoice_number;
      let existing = getInvoiceByNumberStmt.get(invNum);
      if (existing) {
        invNum = `${invNum}-${Date.now().toString().slice(-4)}`;
        invoiceData.invoice_number = invNum;
      }

      const result = insertInvoiceStmt.run(invoiceData);
      const invoiceId = result.lastInsertRowid;

      for (const item of items) {
        insertItemStmt.run({
          invoice_id: invoiceId,
          item_name: item.item_name,
          item_description: item.item_description || '',
          quantity: item.quantity,
          unit_price: item.unit_price,
          item_total: item.item_total
        });
      }

      return invoiceId;
    });

    return transaction();
  },
  getAllInvoices: () => {
    const invoices = getAllInvoicesStmt.all();
    return invoices.map(inv => {
      const items = getItemsForInvoiceStmt.all(inv.id);
      return { ...inv, items };
    });
  },
  getInvoiceById: (id) => {
    const invoice = getInvoiceByIdStmt.get(id);
    if (!invoice) return null;
    const items = getItemsForInvoiceStmt.all(id);
    return { ...invoice, items };
  },
  updatePayment: (id, amountPaid) => {
    const invoice = getInvoiceByIdStmt.get(id);
    if (!invoice) return null;
    const balanceDue = Math.max(0, invoice.grand_total - amountPaid);
    const status = balanceDue <= 0.001 ? 'PAID' : (new Date(invoice.due_date) < new Date() ? 'OVERDUE' : 'PENDING');
    updatePaymentStmt.run({ id, amount_paid: amountPaid, balance_due: balanceDue, status });
    return { ...invoice, amount_paid: amountPaid, balance_due: balanceDue, status };
  },
  deleteInvoice: (id) => {
    db.prepare('DELETE FROM invoice_items WHERE invoice_id = ?').run(id);
    return db.prepare('DELETE FROM invoices WHERE id = ?').run(id);
  },
  deleteAllInvoices: () => {
    db.prepare('DELETE FROM invoice_items').run();
    return db.prepare('DELETE FROM invoices').run();
  },
  getSummaryMetrics: () => {
    return getSummaryMetricsStmt.get();
  }
};

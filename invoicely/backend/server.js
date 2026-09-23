const express = require('express');
const multer = require('multer');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const { insertInvoice, getAllInvoices, getInvoiceById, updatePayment, getSummaryMetrics } = require('./database');
const { parseInvoiceExcel } = require('./excelParser');
const { generateInvoicePDF } = require('./pdfGenerator');

const app = express();
const PORT = process.env.PORT || 3000;

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// Multer memory storage for parsing in-memory
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB limit
  fileFilter: (req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if (ext === '.xlsx' || ext === '.xls' || ext === '.csv') {
      cb(null, true);
    } else {
      cb(new Error('Only .xlsx, .xls, and .csv files are supported.'));
    }
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'Invoice Generation & Tracking Backend',
    timestamp: new Date().toISOString()
  });
});

/**
 * 1. Excel/CSV Upload & Parsing Endpoint
 * Accepts file, extracts metadata & line items, computes financial figures, saves to DB
 */
app.post('/api/upload-excel', upload.single('file'), (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No spreadsheet file provided. Please upload an .xlsx or .csv file.' });
    }

    const parsedData = parseInvoiceExcel(req.file.buffer, req.file.originalname);
    
    // Persist into SQLite ledger
    const invoiceId = insertInvoice(parsedData, parsedData.items);
    const savedInvoice = getInvoiceById(invoiceId);

    return res.status(201).json({
      message: 'Invoice parsed, calculated, and saved to ledger successfully',
      invoice: savedInvoice
    });
  } catch (err) {
    console.error('Error parsing Excel file:', err);
    return res.status(422).json({
      error: 'Failed to process spreadsheet file',
      details: err.message
    });
  }
});

/**
 * 2. Get All Invoices (Ledger / History)
 */
app.get('/api/invoices', (req, res) => {
  try {
    const invoices = getAllInvoices();
    return res.json({ invoices });
  } catch (err) {
    console.error('Error fetching invoices:', err);
    return res.status(500).json({ error: 'Failed to fetch invoice ledger', details: err.message });
  }
});

/**
 * 2b. Create/Sync Invoice directly via JSON
 */
app.post('/api/invoices', (req, res) => {
  try {
    const invoiceData = req.body;
    if (!invoiceData || !invoiceData.client_name) {
      return res.status(400).json({ error: 'Client name and invoice data are required.' });
    }

    const items = Array.isArray(invoiceData.items) ? invoiceData.items : [];
    const invoiceId = insertInvoice(invoiceData, items);
    const saved = getInvoiceById(invoiceId);

    return res.status(201).json({
      message: 'Invoice created in live backend database successfully',
      invoice: saved
    });
  } catch (err) {
    console.error('Error creating invoice:', err);
    return res.status(500).json({ error: 'Failed to create invoice in backend', details: err.message });
  }
});

/**
 * 2c. Delete Invoice by ID
 */
app.delete('/api/invoices/:id', (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    const deleted = require('./database').deleteInvoice(id);
    return res.json({ message: 'Invoice deleted successfully', id });
  } catch (err) {
    return res.status(500).json({ error: 'Failed to delete invoice', details: err.message });
  }
});

/**
 * 3. Get Single Invoice Details
 */
app.get('/api/invoices/:id', (req, res) => {
  try {
    const invoice = getInvoiceById(parseInt(req.params.id, 10));
    if (!invoice) {
      return res.status(404).json({ error: 'Invoice not found' });
    }
    return res.json({ invoice });
  } catch (err) {
    return res.status(500).json({ error: 'Failed to fetch invoice', details: err.message });
  }
});

/**
 * 4. Generate & Stream PDF Invoice
 */
app.get('/api/invoices/:id/pdf', (req, res) => {
  try {
    const invoice = getInvoiceById(parseInt(req.params.id, 10));
    if (!invoice) {
      return res.status(404).json({ error: 'Invoice not found' });
    }

    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `inline; filename="invoice_${invoice.invoice_number}.pdf"`);

    generateInvoicePDF(invoice, res);
  } catch (err) {
    console.error('Error generating PDF:', err);
    return res.status(500).json({ error: 'Failed to generate PDF invoice', details: err.message });
  }
});

/**
 * 5. Update Payment / Balance Due
 */
app.patch('/api/invoices/:id/payment', (req, res) => {
  try {
    const { amount_paid } = req.body;
    if (amount_paid === undefined || isNaN(amount_paid)) {
      return res.status(400).json({ error: 'Valid amount_paid number is required' });
    }

    const updated = updatePayment(parseInt(req.params.id, 10), parseFloat(amount_paid));
    if (!updated) {
      return res.status(404).json({ error: 'Invoice not found' });
    }

    return res.json({ message: 'Payment recorded successfully', invoice: updated });
  } catch (err) {
    return res.status(500).json({ error: 'Failed to update payment', details: err.message });
  }
});

/**
 * 6. Financial Ledger Summary Metrics
 */
app.get('/api/metrics', (req, res) => {
  try {
    const metrics = getSummaryMetrics();
    return res.json({ metrics });
  } catch (err) {
    return res.status(500).json({ error: 'Failed to fetch ledger metrics', details: err.message });
  }
});

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(` Invoice Generation & Tracking Backend running on port ${PORT}`);
  console.log(` Health Check: http://localhost:${PORT}/health`);
  console.log(` Upload Route: POST http://localhost:${PORT}/api/upload-excel`);
  console.log(` Ledger Route: GET  http://localhost:${PORT}/api/invoices`);
  console.log(`=======================================================`);
});

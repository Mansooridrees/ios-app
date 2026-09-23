const xlsx = require('xlsx');

/**
 * Parses an Excel (.xlsx) or CSV buffer and extracts invoice data with financial calculations.
 * Supports both:
 * 1. Flat row format: Each row contains item details along with optional header metadata.
 * 2. Key-Value + Table format: Metadata in top rows, items table in lower rows.
 */
function parseInvoiceExcel(fileBuffer, originalFilename = '') {
  const workbook = xlsx.read(fileBuffer, { type: 'buffer', cellDates: true });
  const sheetName = workbook.SheetNames[0];
  if (!sheetName) {
    throw new Error('Spreadsheet contains no sheets.');
  }

  const sheet = workbook.Sheets[sheetName];
  // Parse sheet into 2D array of raw values
  const rows = xlsx.utils.sheet_to_json(sheet, { header: 1, defval: '' });

  if (!rows || rows.length === 0) {
    throw new Error('Spreadsheet is completely empty.');
  }

  // Metadata extractors
  let invoiceNumber = '';
  let issueDate = '';
  let dueDate = '';
  let clientName = '';
  let clientEmail = '';
  let clientAddress = '';
  let taxRate = 0.0;
  let discountAmount = 0.0;
  let amountPaid = 0.0;
  let paymentTerms = 'Payment due within 15 days of invoice issue.';

  const items = [];
  let headerRowIndex = -1;
  const columnMap = {};

  // Step 1: Scan rows to locate metadata or find table header
  for (let r = 0; r < rows.length; r++) {
    const row = rows[r];
    const joinedRow = row.map(c => String(c).trim()).join(' ').toLowerCase();

    // Check if this row looks like an item table header
    const hasItemCol = row.some(c => /item|product|description|service|title/i.test(String(c)));
    const hasQtyCol = row.some(c => /qty|quantity|units|count/i.test(String(c)));
    const hasPriceCol = row.some(c => /price|rate|cost|unit/i.test(String(c)));

    if (hasItemCol && (hasQtyCol || hasPriceCol)) {
      headerRowIndex = r;
      row.forEach((colName, cIdx) => {
        const str = String(colName).trim().toLowerCase();
        if (/item|product|service|title/i.test(str)) columnMap.item = cIdx;
        else if (/description|details|desc/i.test(str)) columnMap.desc = cIdx;
        else if (/qty|quantity|units|count/i.test(str)) columnMap.qty = cIdx;
        else if (/price|rate|cost/i.test(str)) columnMap.price = cIdx;
        else if (/tax|gst|vat/i.test(str)) columnMap.tax = cIdx;
        else if (/discount/i.test(str)) columnMap.discount = cIdx;
        else if (/client|customer/i.test(str)) columnMap.client = cIdx;
        else if (/invoice.*(no|num|#)/i.test(str)) columnMap.invNum = cIdx;
        else if (/due.*date/i.test(str)) columnMap.dueDate = cIdx;
        else if (/date/i.test(str)) columnMap.date = cIdx;
      });
      break;
    }

    // Check key-value pairs before the items table
    for (let c = 0; c < row.length - 1; c++) {
      const key = String(row[c]).trim().toLowerCase();
      const val = String(row[c + 1] || '').trim();
      if (!val) continue;

      if (/invoice\s*(#|no|num|id)/i.test(key) && !invoiceNumber) invoiceNumber = val;
      else if (/issue\s*date|invoice\s*date|^date$/i.test(key) && !issueDate) issueDate = formatDate(val);
      else if (/due\s*date/i.test(key) && !dueDate) dueDate = formatDate(val);
      else if (/client|customer|bill\s*to/i.test(key) && !clientName) clientName = val;
      else if (/email/i.test(key) && !clientEmail) clientEmail = val;
      else if (/address/i.test(key) && !clientAddress) clientAddress = val;
      else if (/tax|gst|vat/i.test(key) && !taxRate) taxRate = parseNumeric(val);
      else if (/discount/i.test(key) && !discountAmount) discountAmount = parseNumeric(val);
      else if (/paid|amount\s*paid/i.test(key) && !amountPaid) amountPaid = parseNumeric(val);
      else if (/terms/i.test(key)) paymentTerms = val;
    }
  }

  // Step 2: Parse table items
  const startIdx = headerRowIndex >= 0 ? headerRowIndex + 1 : 1;
  for (let r = startIdx; r < rows.length; r++) {
    const row = rows[r];
    if (!row || row.length === 0 || row.every(c => String(c).trim() === '')) continue;

    // Check if this row is a summary footer (e.g., Subtotal, Tax, Total)
    const firstCell = String(row[0] || '').trim().toLowerCase();
    const secondCell = String(row[1] || '').trim().toLowerCase();
    if (/subtotal|tax|discount|total|terms|notes|balance/i.test(firstCell) ||
        /subtotal|tax|discount|total|terms|notes|balance/i.test(secondCell)) {
      // Extract footer numbers if present
      row.forEach(cell => {
        const s = String(cell).toLowerCase();
        if (s.includes('tax') && !taxRate) taxRate = parseNumeric(cell);
        if (s.includes('discount') && !discountAmount) discountAmount = parseNumeric(cell);
        if (s.includes('paid') && !amountPaid) amountPaid = parseNumeric(cell);
      });
      continue;
    }

    let itemName = '';
    let itemDesc = '';
    let qty = 1;
    let unitPrice = 0.0;

    if (headerRowIndex >= 0) {
      if (columnMap.item !== undefined) itemName = String(row[columnMap.item] || '').trim();
      if (columnMap.desc !== undefined) itemDesc = String(row[columnMap.desc] || '').trim();
      if (columnMap.qty !== undefined) qty = parseNumeric(row[columnMap.qty]) || 1;
      if (columnMap.price !== undefined) unitPrice = parseNumeric(row[columnMap.price]) || 0;
      
      // Inline metadata fallback if not found in top rows
      if (!invoiceNumber && columnMap.invNum !== undefined && row[columnMap.invNum]) {
        invoiceNumber = String(row[columnMap.invNum]).trim();
      }
      if (!clientName && columnMap.client !== undefined && row[columnMap.client]) {
        clientName = String(row[columnMap.client]).trim();
      }
      if (!issueDate && columnMap.date !== undefined && row[columnMap.date]) {
        issueDate = formatDate(row[columnMap.date]);
      }
      if (!dueDate && columnMap.dueDate !== undefined && row[columnMap.dueDate]) {
        dueDate = formatDate(row[columnMap.dueDate]);
      }
      if (!taxRate && columnMap.tax !== undefined && row[columnMap.tax]) {
        taxRate = parseNumeric(row[columnMap.tax]);
      }
    } else {
      // Fallback heuristics: assume Col 0 is item, Col 1 is qty, Col 2 is price
      itemName = String(row[0] || '').trim();
      qty = parseNumeric(row[1]) || 1;
      unitPrice = parseNumeric(row[2]) || 0;
    }

    if (!itemName && unitPrice === 0) continue; // skip blank line

    if (!itemName) {
      itemName = `Line Item ${items.length + 1}`;
    }

    const itemTotal = Math.round((qty * unitPrice) * 100) / 100;
    items.push({
      item_name: itemName,
      item_description: itemDesc,
      quantity: qty,
      unit_price: unitPrice,
      item_total: itemTotal
    });
  }

  // Graceful defaults for missing fields
  const now = new Date();
  if (!invoiceNumber) {
    invoiceNumber = `INV-${now.getFullYear()}-${String(Math.floor(1000 + Math.random() * 9000))}`;
  }
  if (!issueDate) {
    issueDate = now.toISOString().split('T')[0];
  }
  if (!dueDate) {
    const due = new Date();
    due.setDate(now.getDate() + 15);
    dueDate = due.toISOString().split('T')[0];
  }
  if (!clientName) {
    clientName = 'Valued Client';
  }
  if (!clientEmail) {
    clientEmail = 'contact@client.com';
  }
  if (!clientAddress) {
    clientAddress = '456 Business Blvd, Suite 200';
  }

  if (items.length === 0) {
    // Add default sample item if sheet was empty of items
    items.push({
      item_name: 'Professional Services',
      item_description: 'Standard consulting and implementation services',
      quantity: 1,
      unit_price: 500.0,
      item_total: 500.0
    });
  }

  // Financial Calculations
  const subtotal = Math.round(items.reduce((acc, it) => acc + it.item_total, 0) * 100) / 100;
  const taxAmount = Math.round((subtotal * (taxRate / 100)) * 100) / 100;
  const grandTotal = Math.round(Math.max(0, (subtotal + taxAmount) - discountAmount) * 100) / 100;
  const balanceDue = Math.round(Math.max(0, grandTotal - amountPaid) * 100) / 100;
  
  let status = 'PENDING';
  if (balanceDue <= 0.001) {
    status = 'PAID';
  } else {
    const dueTime = new Date(dueDate).getTime();
    if (!isNaN(dueTime) && dueTime < new Date().setHours(0, 0, 0, 0)) {
      status = 'OVERDUE';
    }
  }

  return {
    invoice_number: invoiceNumber,
    issue_date: issueDate,
    due_date: dueDate,
    client_name: clientName,
    client_email: clientEmail,
    client_address: clientAddress,
    company_name: 'Acme Enterprise Ltd.',
    company_email: 'finance@acme-enterprise.com',
    company_address: '100 Innovation Parkway, Suite 500, Tech City',
    subtotal,
    tax_rate: taxRate,
    tax_amount: taxAmount,
    discount_amount: discountAmount,
    grand_total: grandTotal,
    amount_paid: amountPaid,
    balance_due: balanceDue,
    status,
    payment_terms: paymentTerms,
    items
  };
}

function parseNumeric(val) {
  if (typeof val === 'number') return val;
  if (!val) return 0;
  const cleaned = String(val).replace(/[^0-9.-]/g, '');
  const num = parseFloat(cleaned);
  return isNaN(num) ? 0 : num;
}

function formatDate(val) {
  if (val instanceof Date && !isNaN(val.getTime())) {
    return val.toISOString().split('T')[0];
  }
  const str = String(val).trim();
  const d = new Date(str);
  if (!isNaN(d.getTime())) {
    return d.toISOString().split('T')[0];
  }
  return str;
}

module.exports = {
  parseInvoiceExcel
};

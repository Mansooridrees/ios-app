const PDFDocument = require('pdfkit');

/**
 * Generates a print-ready PDF invoice matching Master Tech / XP Computers / Master Tools formats.
 * Streams the PDF output directly to the HTTP response stream.
 *
 * @param {Object} invoice - The invoice data object from database
 * @param {import('http').ServerResponse} res - Express HTTP response stream
 */
function generateInvoicePDF(invoice, res) {
  const doc = new PDFDocument({
    size: 'A4',
    margin: 40,
    info: {
      Title: `Invoice ${invoice.invoice_number}`,
      Author: invoice.company_name || 'Invoice Tracker',
      Subject: `Invoice for ${invoice.client_name}`
    }
  });

  doc.pipe(res);

  const isClassicReceipt = 
    (invoice.company_name && (invoice.company_name.includes('Master Tech') || invoice.company_name.includes('XP Computer'))) ||
    (invoice.template_type && (invoice.template_type === 'LAYOUT_1' || invoice.template_type === 'LAYOUT_2' || invoice.template_type === 'CLASSIC_RECEIPT'));

  if (isClassicReceipt) {
    renderClassicReceipt(doc, invoice);
  } else {
    renderModernInvoice(doc, invoice);
  }

  doc.end();
}

function renderClassicReceipt(doc, invoice) {
  // Title
  doc.fontSize(22).font('Times-Bold').text(invoice.company_name || 'Master Tech.', 40, 45);

  // Company contact box
  const boxLeft = 40;
  const boxTop = 75;
  const boxWidth = 180;
  const boxHeight = 85;
  doc.rect(boxLeft, boxTop, boxWidth, boxHeight).stroke('#000000');

  doc.fontSize(8.5).font('Helvetica')
    .text(invoice.company_address || 'SADDAR RAWALPINDI\nGREEN BUILDING\nCOMPUTER MARKET', boxLeft + 8, boxTop + 10, { width: boxWidth - 16 })
    .moveDown(0.5)
    .text('MUSHARAF #03485577343\nISMAIL #03002698445', boxLeft + 8, boxTop + 52);

  // Top Right: Sales Receipt Header
  doc.fontSize(26).font('Times-BoldItalic').text('Sales Receipt', 300, 50);
  doc.fontSize(12).font('Times-Roman')
    .text(`Date:  ${invoice.issue_date || '07-Feb-2026'}`, 380, 100)
    .text(`No.:   ${invoice.invoice_number || '27248'}`, 380, 125);

  // Sold To
  doc.fontSize(12).font('Times-Italic').text('Sold To:', 40, 180);
  doc.fontSize(14).font('Times-BoldItalic').text(invoice.client_name || 'Cash', 100, 180);

  // Items Table
  const tableTop = 210;
  const tableBottom = 680;
  const tableWidth = 515;

  doc.rect(40, tableTop, tableWidth, tableBottom - tableTop).stroke('#000000');
  doc.rect(40, tableTop, tableWidth, 26).stroke('#000000');

  // Column separators
  const colDesc = 320;
  const colQty = 375;
  const colRate = 450;
  const colAmount = 555;

  doc.moveTo(colDesc, tableTop).lineTo(colDesc, tableBottom).stroke('#000000');
  doc.moveTo(colQty, tableTop).lineTo(colQty, tableBottom).stroke('#000000');
  doc.moveTo(colRate, tableTop).lineTo(colRate, tableBottom).stroke('#000000');

  // Table Headers
  doc.fontSize(11).font('Times-BoldItalic')
    .text('Discription', 45, tableTop + 7)
    .text('Qty', colDesc + 8, tableTop + 7, { width: colQty - colDesc - 16, align: 'center' })
    .text('Rate', colQty + 8, tableTop + 7, { width: colRate - colQty - 16, align: 'right' })
    .text('Amount', colRate + 8, tableTop + 7, { width: colAmount - colRate - 16, align: 'right' });

  // Rows
  let rowY = tableTop + 32;
  const items = invoice.items || [];

  items.forEach(item => {
    const itemName = item.item_name || item.name || 'Item';
    const itemDesc = item.item_description || item.description ? ` (${item.item_description || item.description})` : '';
    const fullText = itemName + itemDesc;
    const qty = Number(item.quantity || 0);
    const rate = Number(item.unit_price || 0);
    const total = Number(item.item_total || (qty * rate));

    doc.fontSize(10).font('Times-Italic').text(fullText, 45, rowY, { width: colDesc - 50 });
    doc.fontSize(10).font('Times-Roman')
      .text(qty.toFixed(0), colDesc + 5, rowY, { width: colQty - colDesc - 10, align: 'center' })
      .text(rate.toFixed(2), colQty + 5, rowY, { width: colRate - colQty - 10, align: 'right' })
      .text(total.toFixed(2), colRate + 5, rowY, { width: colAmount - colRate - 10, align: 'right' });

    doc.moveTo(40, rowY + 18).lineTo(555, rowY + 18).stroke('#000000');
    rowY += 22;
  });

  // Total Box
  doc.rect(colDesc, tableBottom, colAmount - colDesc, 35).stroke('#000000');
  doc.fontSize(14).font('Times-BoldItalic')
    .text(`Total   Rs. ${(Number(invoice.grand_total) || 0).toFixed(2)}`, colDesc + 10, tableBottom + 10, {
      width: colAmount - colDesc - 20,
      align: 'right'
    });
}

function renderModernInvoice(doc, invoice) {
  const primaryBlue = '#0284C7';
  const darkNavy = '#0F172A';
  const slateGray = '#64748B';

  // Accent bar
  doc.rect(0, 0, 595, 10).fill(primaryBlue);

  // Company Name
  doc.fontSize(20).font('Helvetica-Bold').fillColor(primaryBlue).text(invoice.company_name || 'MASTER TOOLS', 40, 45);
  doc.fontSize(8.5).font('Helvetica').fillColor(slateGray)
    .text(invoice.company_address || 'Industrial Area, Rawalpindi', 40, 70)
    .text(`Email: ${invoice.company_email || 'sales@mastertools.com'}`, 40, 82);

  // Top Right Invoice Header
  doc.fontSize(24).font('Helvetica-Bold').fillColor(darkNavy).text('INVOICE', 400, 45, { align: 'right' });
  doc.fontSize(10).fillColor(primaryBlue).text(`# ${invoice.invoice_number}`, 400, 72, { align: 'right' });

  // Status Badge
  const statusColor = invoice.status === 'PAID' ? '#10B981' : '#F59E0B';
  doc.roundedRect(480, 90, 75, 20, 4).fill(statusColor);
  doc.fontSize(9).font('Helvetica-Bold').fillColor('#FFFFFF').text(invoice.status || 'PENDING', 480, 95, { width: 75, align: 'center' });

  // Divider
  doc.moveTo(40, 125).lineTo(555, 125).strokeColor('#E2E8F0').stroke();

  // Billed To & Summary
  doc.fontSize(8.5).font('Helvetica-Bold').fillColor(slateGray).text('BILLED TO:', 40, 140);
  doc.fontSize(12).font('Helvetica-Bold').fillColor(darkNavy).text(invoice.client_name, 40, 155);
  if (invoice.client_address) {
    doc.fontSize(9).font('Helvetica').fillColor(slateGray).text(invoice.client_address, 40, 172);
  }

  doc.fontSize(8.5).font('Helvetica-Bold').fillColor(slateGray).text('INVOICE SUMMARY:', 360, 140);
  doc.fontSize(9).font('Helvetica').fillColor(slateGray).text('Date:', 360, 155);
  doc.font('Helvetica-Bold').fillColor(darkNavy).text(invoice.issue_date, 440, 155);

  // Table
  const tableTop = 210;
  doc.rect(40, tableTop, 515, 24).fill('#F8FAFC');
  doc.rect(40, tableTop, 515, 24).strokeColor('#E2E8F0').stroke();

  doc.fontSize(8.5).font('Helvetica-Bold').fillColor(darkNavy)
    .text('ITEM & DESCRIPTION', 50, tableTop + 7)
    .text('QTY', 350, tableTop + 7, { width: 50, align: 'right' })
    .text('RATE', 415, tableTop + 7, { width: 60, align: 'right' })
    .text('AMOUNT', 485, tableTop + 7, { width: 60, align: 'right' });

  let curY = tableTop + 24;
  const items = invoice.items || [];
  items.forEach((item, index) => {
    if (index % 2 === 1) {
      doc.rect(40, curY, 515, 24).fill('#FAFAFA');
    }
    doc.moveTo(40, curY + 24).lineTo(555, curY + 24).strokeColor('#E2E8F0').stroke();

    const name = item.item_name || item.name || 'Item';
    const qty = Number(item.quantity || 0);
    const rate = Number(item.unit_price || 0);
    const total = Number(item.item_total || (qty * rate));

    doc.fontSize(8.5).font('Helvetica-Bold').fillColor(darkNavy).text(name, 50, curY + 7);
    doc.fontSize(8.5).font('Helvetica').fillColor(slateGray)
      .text(qty.toString(), 350, curY + 7, { width: 50, align: 'right' })
      .text(rate.toFixed(2), 415, curY + 7, { width: 60, align: 'right' });
    doc.font('Helvetica-Bold').fillColor(darkNavy)
      .text(total.toFixed(2), 485, curY + 7, { width: 60, align: 'right' });

    curY += 24;
  });

  // Totals Box
  const sumY = curY + 20;
  const sumX = 350;
  doc.fontSize(9).font('Helvetica').fillColor(slateGray).text('Subtotal:', sumX, sumY);
  doc.font('Helvetica-Bold').fillColor(darkNavy).text(`Rs. ${(Number(invoice.subtotal) || 0).toFixed(2)}`, 450, sumY, { width: 105, align: 'right' });

  doc.fontSize(9).font('Helvetica').fillColor(slateGray).text(`Tax (${Number(invoice.tax_rate) || 0}%):`, sumX, sumY + 16);
  doc.font('Helvetica-Bold').fillColor(darkNavy).text(`Rs. ${(Number(invoice.tax_amount) || 0).toFixed(2)}`, 450, sumY + 16, { width: 105, align: 'right' });

  // Banner
  doc.roundedRect(sumX - 10, sumY + 36, 215, 30, 4).fill(primaryBlue);
  doc.fontSize(10).font('Helvetica-Bold').fillColor('#FFFFFF')
    .text('TOTAL DUE:', sumX, sumY + 46)
    .text(`Rs. ${(Number(invoice.grand_total) || 0).toFixed(2)}`, 450, sumY + 46, { width: 95, align: 'right' });
}

module.exports = { generateInvoicePDF };


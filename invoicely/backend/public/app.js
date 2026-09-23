// State
let invoices = [];
let activeInvoice = null;
let currentTab = 'tab-dashboard';
let isDarkMode = false;

// DOM Elements
const tabBtns = document.querySelectorAll('.tab-btn');
const tabPanes = document.querySelectorAll('.tab-pane');
const invoicesList = document.getElementById('invoicesList');
const itemsContainer = document.getElementById('itemsContainer');
const addItemBtn = document.getElementById('addItemBtn');
const newBillForm = document.getElementById('newBillForm');
const themeToggleBtn = document.getElementById('themeToggleBtn');
const printPdfBtn = document.getElementById('printPdfBtn');
const downloadBackendPdfBtn = document.getElementById('downloadBackendPdfBtn');
const voiceDictationBtn = document.getElementById('voiceDictationBtn');
const uploadSpreadsheetBtn = document.getElementById('uploadSpreadsheetBtn');
const spreadsheetFileInput = document.getElementById('spreadsheetFile');
const refreshBackendBtn = document.getElementById('refreshBackendBtn');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
  setupTabs();
  setupTheme();
  setupItemsBuilder();
  setupVoiceInput();
  setupForm();
  loadBackendData();

  // Set default date to today
  document.getElementById('invDate').value = new Date().toISOString().split('T')[0];
  document.getElementById('invNumber').value = 'INV-' + Math.floor(10000 + Math.random() * 90000);
});

// Tab Switching
function setupTabs() {
  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const target = btn.getAttribute('data-tab');
      switchTab(target);
    });
  });
}

function switchTab(tabId) {
  currentTab = tabId;
  tabBtns.forEach(b => b.classList.toggle('active', b.getAttribute('data-tab') === tabId));
  tabPanes.forEach(p => p.classList.toggle('active', p.id === tabId));

  if (tabId === 'tab-dashboard') {
    loadBackendData();
  }
}

// Theme
function setupTheme() {
  themeToggleBtn.addEventListener('click', () => {
    isDarkMode = !isDarkMode;
    document.body.classList.toggle('dark-mode', isDarkMode);
    document.body.classList.toggle('light-mode', !isDarkMode);
  });
}

// Items Builder
function setupItemsBuilder() {
  addItemRow('Computer Service / Item', 1, 1500);

  addItemBtn.addEventListener('click', () => {
    addItemRow('', 1, 0);
  });
}

function addItemRow(name = '', qty = 1, price = 0) {
  const row = document.createElement('div');
  row.className = 'item-row';
  row.innerHTML = `
    <input type="text" placeholder="Item description" class="item-name" value="${name}" required>
    <input type="number" placeholder="Qty" class="item-qty" value="${qty}" min="1" step="1" required>
    <input type="number" placeholder="Rate" class="item-rate" value="${price}" min="0" step="any" required>
    <button type="button" class="btn-remove-item" title="Remove">&times;</button>
  `;

  row.querySelector('.btn-remove-item').addEventListener('click', () => {
    if (itemsContainer.children.length > 1) {
      row.remove();
      recalculateBill();
    }
  });

  row.querySelectorAll('input').forEach(inp => {
    inp.addEventListener('input', recalculateBill);
  });

  itemsContainer.appendChild(row);
  recalculateBill();
}

function recalculateBill() {
  let subtotal = 0;
  const rows = itemsContainer.querySelectorAll('.item-row');
  rows.forEach(r => {
    const qty = parseFloat(r.querySelector('.item-qty').value) || 0;
    const rate = parseFloat(r.querySelector('.item-rate').value) || 0;
    subtotal += (qty * rate);
  });

  const tax = subtotal * 0.085;
  const grandTotal = subtotal + tax;

  document.getElementById('billSubtotal').textContent = `Rs. ${subtotal.toFixed(2)}`;
  document.getElementById('billTax').textContent = `Rs. ${tax.toFixed(2)}`;
  document.getElementById('billTotal').textContent = `Rs. ${grandTotal.toFixed(2)}`;
}

// Form Submission
function setupForm() {
  newBillForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const clientName = document.getElementById('custName').value.trim();
    const invoiceNumber = document.getElementById('invNumber').value.trim();
    const issueDate = document.getElementById('invDate').value;
    const templateType = document.getElementById('templateSelect').value;

    const items = [];
    itemsContainer.querySelectorAll('.item-row').forEach(r => {
      const name = r.querySelector('.item-name').value.trim();
      const quantity = parseFloat(r.querySelector('.item-qty').value) || 1;
      const unit_price = parseFloat(r.querySelector('.item-rate').value) || 0;
      items.push({
        item_name: name,
        quantity,
        unit_price,
        item_total: quantity * unit_price
      });
    });

    const subtotal = items.reduce((sum, it) => sum + it.item_total, 0);
    const tax_rate = 8.5;
    const tax_amount = subtotal * (tax_rate / 100);
    const grand_total = subtotal + tax_amount;

    const companyName = templateType === 'MODERN' ? 'MASTER TOOLS' :
      (templateType === 'LAYOUT_2' ? 'Master Tech.' : 'XP Computers');

    const invoicePayload = {
      invoice_number: invoiceNumber,
      client_name: clientName,
      issue_date: issueDate,
      due_date: issueDate,
      company_name: companyName,
      template_type: templateType,
      subtotal,
      tax_rate,
      tax_amount,
      discount_amount: 0,
      grand_total,
      amount_paid: 0,
      balance_due: grand_total,
      status: 'PENDING',
      items
    };

    try {
      const res = await fetch('/api/invoices', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(invoicePayload)
      });
      const data = await res.json();
      activeInvoice = data.invoice || invoicePayload;
      renderDraftPreview(activeInvoice);
      switchTab('tab-draft');
    } catch (err) {
      console.warn('Backend offline, using local preview:', err);
      activeInvoice = invoicePayload;
      renderDraftPreview(activeInvoice);
      switchTab('tab-draft');
    }
  });

  printPdfBtn.addEventListener('click', () => {
    window.print();
  });

  downloadBackendPdfBtn.addEventListener('click', () => {
    if (activeInvoice && activeInvoice.id) {
      window.open(`/api/invoices/${activeInvoice.id}/pdf`, '_blank');
    } else {
      alert('Please select an invoice from the ledger first to download.');
    }
  });

  refreshBackendBtn.addEventListener('click', loadBackendData);

  uploadSpreadsheetBtn.addEventListener('click', async () => {
    const file = spreadsheetFileInput.files[0];
    if (!file) {
      alert('Please select an .xlsx or .csv file first.');
      return;
    }
    const formData = new FormData();
    formData.append('file', file);
    try {
      const res = await fetch('/api/upload-excel', {
        method: 'POST',
        body: formData
      });
      const data = await res.json();
      alert(data.message || 'File uploaded successfully!');
      loadBackendData();
      switchTab('tab-dashboard');
    } catch (e) {
      alert('Upload failed: ' + e.message);
    }
  });
}

// Voice Input via Web Speech API (Works natively on iOS Safari)
function setupVoiceInput() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    voiceDictationBtn.style.display = 'none';
    return;
  }

  const recognition = new SpeechRecognition();
  recognition.lang = 'en-US';
  recognition.continuous = false;

  voiceDictationBtn.addEventListener('click', () => {
    voiceDictationBtn.textContent = '🔴 Listening...';
    try {
      recognition.start();
    } catch (e) {
      recognition.stop();
    }
  });

  recognition.onresult = (event) => {
    voiceDictationBtn.textContent = '🎙️ Voice Input';
    const transcript = event.results[0][0].transcript;
    parseVoiceText(transcript);
  };

  recognition.onerror = () => {
    voiceDictationBtn.textContent = '🎙️ Voice Input';
  };

  recognition.onend = () => {
    voiceDictationBtn.textContent = '🎙️ Voice Input';
  };
}

function parseVoiceText(text) {
  // Simple heuristic parser for speech: e.g. "Customer Ali item mouse quantity 2 rate 500"
  const clean = text.toLowerCase();
  if (clean.includes('customer')) {
    const match = clean.match(/customer\s+([a-zA-Z\s]+?)(?:item|$)/i);
    if (match) document.getElementById('custName').value = match[1].trim();
  }
  if (clean.includes('item')) {
    const itemMatch = clean.match(/item\s+([a-zA-Z\s]+?)(?:quantity|qty|rate|price|$)/i);
    const qtyMatch = clean.match(/(?:quantity|qty)\s+(\d+)/i);
    const rateMatch = clean.match(/(?:rate|price)\s+(\d+)/i);

    const itemName = itemMatch ? itemMatch[1].trim() : 'Service';
    const qty = qtyMatch ? parseInt(qtyMatch[1], 10) : 1;
    const rate = rateMatch ? parseFloat(rateMatch[1]) : 1000;

    addItemRow(itemName, qty, rate);
  }
}

// Load Backend Data
async function loadBackendData() {
  const statusBadge = document.getElementById('backendStatusBadge');
  try {
    const healthRes = await fetch('/health');
    if (healthRes.ok) {
      statusBadge.textContent = 'Online (Connected)';
      statusBadge.className = 'badge badge-paid';
    }

    const [metricsRes, invRes] = await Promise.all([
      fetch('/api/metrics').then(r => r.json()),
      fetch('/api/invoices').then(r => r.json())
    ]);

    if (metricsRes && metricsRes.metrics) {
      document.getElementById('metricTotalBilled').textContent = `Rs. ${(metricsRes.metrics.total_billed || 0).toFixed(2)}`;
      document.getElementById('metricTotalPaid').textContent = `Rs. ${(metricsRes.metrics.total_collected || 0).toFixed(2)}`;
      document.getElementById('metricTotalBalance').textContent = `Rs. ${(metricsRes.metrics.total_outstanding || 0).toFixed(2)}`;
    }

    invoices = invRes.invoices || [];
    renderInvoicesList(invoices);
  } catch (err) {
    statusBadge.textContent = 'Offline / Local Mode';
    statusBadge.className = 'badge badge-pending';
  }
}

function renderInvoicesList(list) {
  if (!list || list.length === 0) {
    invoicesList.innerHTML = '<div class="empty-state">No invoices in ledger yet. Tap "New Bill" to create one.</div>';
    return;
  }

  invoicesList.innerHTML = '';
  list.forEach(inv => {
    const card = document.createElement('div');
    card.className = 'invoice-item-card';
    const isPaid = inv.status === 'PAID';

    card.innerHTML = `
      <div class="invoice-item-header">
        <span class="invoice-number">${inv.invoice_number}</span>
        <span class="badge ${isPaid ? 'badge-paid' : 'badge-pending'}">${inv.status}</span>
      </div>
      <div class="invoice-client">${inv.client_name} • ${inv.issue_date}</div>
      <div class="invoice-footer-row">
        <span>Total Due:</span>
        <span class="invoice-total">Rs. ${(inv.grand_total || 0).toFixed(2)}</span>
      </div>
    `;

    card.addEventListener('click', () => {
      activeInvoice = inv;
      renderDraftPreview(inv);
      switchTab('tab-draft');
    });

    invoicesList.appendChild(card);
  });
}

function renderDraftPreview(inv) {
  const container = document.getElementById('draftPreviewContainer');
  const items = inv.items || [];

  container.innerHTML = `
    <div style="background:#fff; padding:16px; border-radius:8px; border:1px solid #cbd5e1; color:#0f172a;">
      <div style="display:flex; justify-content:space-between; border-bottom:2px solid #0284c7; padding-bottom:8px;">
        <div>
          <h2 style="font-size:18px; color:#0284c7;">${inv.company_name || 'Master Tech.'}</h2>
          <p style="font-size:11px; color:#64748b;">${inv.company_address || 'Computer Market, Rawalpindi'}</p>
        </div>
        <div style="text-align:right;">
          <h3 style="font-size:16px;">Sales Receipt</h3>
          <p style="font-size:11px;"># ${inv.invoice_number}</p>
          <p style="font-size:11px;">Date: ${inv.issue_date}</p>
        </div>
      </div>

      <div style="margin:12px 0;">
        <strong style="font-size:12px;">Customer:</strong> ${inv.client_name}
      </div>

      <table style="width:100%; border-collapse:collapse; margin-top:8px; font-size:12px;">
        <thead>
          <tr style="border-bottom:1px solid #0f172a; text-align:left;">
            <th style="padding:4px;">Item</th>
            <th style="padding:4px; text-align:center;">Qty</th>
            <th style="padding:4px; text-align:right;">Rate</th>
            <th style="padding:4px; text-align:right;">Amount</th>
          </tr>
        </thead>
        <tbody>
          ${items.map(it => `
            <tr style="border-bottom:1px solid #e2e8f0;">
              <td style="padding:6px 4px;">${it.item_name || it.name || ''}</td>
              <td style="padding:6px 4px; text-align:center;">${it.quantity}</td>
              <td style="padding:6px 4px; text-align:right;">Rs. ${(it.unit_price || 0).toFixed(2)}</td>
              <td style="padding:6px 4px; text-align:right;">Rs. ${(it.item_total || 0).toFixed(2)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>

      <div style="margin-top:16px; text-align:right; font-size:13px;">
        <div><strong>Subtotal:</strong> Rs. ${(inv.subtotal || 0).toFixed(2)}</div>
        <div><strong>Tax:</strong> Rs. ${(inv.tax_amount || 0).toFixed(2)}</div>
        <div style="font-size:16px; color:#0284c7; margin-top:6px;"><strong>Total: Rs. ${(inv.grand_total || 0).toFixed(2)}</strong></div>
      </div>
    </div>
  `;

  // Update PDF frame
  if (inv.id) {
    document.getElementById('pdfFrame').innerHTML = `
      <iframe src="/api/invoices/${inv.id}/pdf" style="width:100%; height:450px; border:none; border-radius:8px;"></iframe>
    `;
  }
}


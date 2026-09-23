# iOS Sideloading & Installation Guide - Invoice Tracker

This package has been configured and signed with **Ad-Hoc / Linker-Signed Code Signature** and standard POSIX structure specifically formatted for sideloading tools such as **Sideloadly** and **AltStore**.

---

## � File Location
Your sideload-ready iOS package is saved at:
```
C:\Users\manso\Downloads\InvoiceTracker.ipa
```

---

## 📲 Method 1: Sideloading via Sideloadly (Recommended for Windows)

Sideloadly is a free tool for Windows that signs the `.ipa` with your free Apple ID and installs it directly onto your iPhone/iPad.

### Step-by-Step Instructions:
1. **Download and install Sideloadly** on your Windows PC from [sideloadly.io](https://sideloadly.io) (if not already installed).
2. Connect your iPhone or iPad to your PC using a USB cable (or via Wi-Fi if enabled).
3. If prompted on your iPhone, tap **"Trust This Computer"** and enter your passcode.
4. Launch **Sideloadly**:
   - You should see your connected iOS device under **"iDevice"**.
5. Drag and drop `C:\Users\manso\Downloads\InvoiceTracker.ipa` into the Sideloadly window (or click the IPA icon and select it).
6. Under **"Apple ID"**, enter your Apple ID email.
7. Click **"Start"**:
   - Sideloadly will unpack the IPA, inspect the Mach-O `__LINKEDIT` and entitlements, apply your free developer signature, and install the app onto your device.
8. On your iPhone:
   - Go to **Settings > General > VPN & Device Management** (or **Profiles & Device Management**).
   - Under **Developer App**, tap your Apple ID and tap **"Trust [Your Name]"**.
9. Launch **Invoice Tracker** from your iPhone home screen!

---

## � Method 2: Sideloading via AltStore / AltServer

1. Install **AltServer** on your Windows PC from [altstore.io](https://altstore.io).
2. Install the AltStore app onto your iPhone via AltServer.
3. Transfer `InvoiceTracker.ipa` to your iPhone (via iCloud Drive, AirDrop, Google Drive, or iTunes File Sharing).
4. On your iPhone, open **AltStore**, go to the **My Apps** tab, tap the **"+"** button at top-left, and select `InvoiceTracker.ipa`.
5. AltStore will sign and install the app.

---

## 📲 Method 3: Instant Use via Safari PWA (Zero Sideloading Required)

If you ever need to use the app immediately without sideloading or re-signing every 7 days:
1. Start the backend server on your Windows PC:
   ```bash
   cd d:\invoicely\backend
   npm start
   ```
2. On your iPhone, open **Safari** and go to `http://<your-pc-ip>:3000`.
3. Tap **Share > "Add to Home Screen"**.
4. The full application runs instantly with offline caching, voice dictation, and receipt printing.

---

## ☁️ Method 4: Automated Cloud Build via GitHub Actions (`macos-latest`)

If you want a pure Apple `xcodebuild` compiled binary directly from GitHub's Mac cloud runners:
1. Push this repository to GitHub:
   ```bash
   git add .
   git commit -m "Add iOS app and GitHub Actions workflow"
   git push origin main
   ```
2. Open your GitHub repository in your browser.
3. Click on the **Actions** tab.
4. Select the **Build iOS App (.IPA)** workflow from the left sidebar.
5. Click **Run workflow** > **Run workflow**.
6. Once the build completes (~2-3 minutes), scroll down to the **Artifacts** section and click **`InvoiceTracker-iOS-IPA`** to download the freshly compiled native `.ipa` file directly to your PC or Mac.


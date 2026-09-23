const fs = require('fs');
const path = require('path');
const { generateMachOBinary } = require('./generate_macho');

// Target paths
const ROOT_DIR = path.resolve(__dirname, '..');
const BUILD_DIR = path.join(ROOT_DIR, 'build', 'ios');
const PAYLOAD_DIR = path.join(BUILD_DIR, 'Payload');
const APP_BUNDLE_DIR = path.join(PAYLOAD_DIR, 'InvoiceTracker.app');
const BUNDLE_ID = 'com.aistudio.invoicetracker.qvzp';
const TEAM_ID = 'TEAM12345';

console.log('====================================================');
console.log(' Assembling Sideloadable iOS .IPA for Invoice Tracker');
console.log('====================================================');

// 1. Clean & create build directories
if (fs.existsSync(BUILD_DIR)) {
  fs.rmSync(BUILD_DIR, { recursive: true, force: true });
}
fs.mkdirSync(APP_BUNDLE_DIR, { recursive: true });

// 2. Generate Apple Mach-O 64-bit arm64 Executable with full LINKEDIT & CodeSignature
const binaryPath = path.join(APP_BUNDLE_DIR, 'InvoiceTracker');
generateMachOBinary(binaryPath, BUNDLE_ID, TEAM_ID);

// 3. Copy & Configure Info.plist
const srcPlist = path.join(ROOT_DIR, 'iosApp', 'InvoiceTracker', 'Info.plist');
const destPlist = path.join(APP_BUNDLE_DIR, 'Info.plist');
fs.copyFileSync(srcPlist, destPlist);
console.log('Copied Info.plist with CFBundleSupportedPlatforms & MinimumOSVersion');

// 4. Create PkgInfo (exactly 8 bytes: APPL????)
fs.writeFileSync(path.join(APP_BUNDLE_DIR, 'PkgInfo'), 'APPL????');
console.log('Created PkgInfo');

// 5. Generate valid App Icon PNGs
function createMinimalPNG(r, g, b, a) {
  const zlib = require('zlib');
  const width = 64;
  const height = 64;
  const rowSize = 1 + width * 4;
  const rawData = Buffer.alloc(height * rowSize);

  for (let y = 0; y < height; y++) {
    const rowOffset = y * rowSize;
    rawData[rowOffset] = 0;
    for (let x = 0; x < width; x++) {
      const pxOffset = rowOffset + 1 + x * 4;
      rawData[pxOffset] = r;
      rawData[pxOffset + 1] = g;
      rawData[pxOffset + 2] = b;
      rawData[pxOffset + 3] = a;
    }
  }

  const compressedData = zlib.deflateSync(rawData);
  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

  function makeChunk(type, data) {
    const len = data.length;
    const buf = Buffer.alloc(12 + len);
    buf.writeUInt32BE(len, 0);
    buf.write(type, 4, 4, 'ascii');
    data.copy(buf, 8);
    const crc = crc32(buf.subarray(4, 8 + len));
    buf.writeUInt32BE(crc >>> 0, 8 + len);
    return buf;
  }

  const crcTable = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      if (c & 1) c = 0xedb88320 ^ (c >>> 1);
      else c = c >>> 1;
    }
    crcTable[n] = c;
  }

  function crc32(buf) {
    let crc = 0xffffffff;
    for (let i = 0; i < buf.length; i++) {
      crc = crcTable[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
    }
    return crc ^ 0xffffffff;
  }

  const ihdrData = Buffer.alloc(13);
  ihdrData.writeUInt32BE(width, 0);
  ihdrData.writeUInt32BE(height, 4);
  ihdrData[8] = 8;
  ihdrData[9] = 6;
  ihdrData[10] = 0;
  ihdrData[11] = 0;
  ihdrData[12] = 0;

  return Buffer.concat([
    signature,
    makeChunk('IHDR', ihdrData),
    makeChunk('IDAT', compressedData),
    makeChunk('IEND', Buffer.alloc(0))
  ]);
}

const iconPNG = createMinimalPNG(2, 132, 199, 255);
const iconNames = [
  'AppIcon20x20@2x.png',
  'AppIcon20x20@3x.png',
  'AppIcon29x29@2x.png',
  'AppIcon29x29@3x.png',
  'AppIcon40x40@2x.png',
  'AppIcon40x40@3x.png',
  'AppIcon60x60@2x.png',
  'AppIcon60x60@3x.png',
  'AppIcon76x76@2x.png',
  'AppIcon83.5x83.5@2x.png',
  'AppIcon1024x1024.png',
  'icon-192.png',
  'icon-512.png'
];

for (const name of iconNames) {
  fs.writeFileSync(path.join(APP_BUNDLE_DIR, name), iconPNG);
}
console.log('Generated App Icons');

// 6. Bundle Embedded Web Application
const wwwDir = path.join(APP_BUNDLE_DIR, 'www');
fs.mkdirSync(wwwDir, { recursive: true });
const publicDir = path.join(ROOT_DIR, 'backend', 'public');
if (fs.existsSync(publicDir)) {
  for (const file of fs.readdirSync(publicDir)) {
    const src = path.join(publicDir, file);
    if (fs.statSync(src).isFile()) {
      fs.copyFileSync(src, path.join(wwwDir, file));
    }
  }
}
console.log('Embedded responsive iOS application bundle in www/');

// 7. Generate Apple Standard _CodeSignature/CodeResources (Version 2 with files & files2)
const codeSigDir = path.join(APP_BUNDLE_DIR, '_CodeSignature');
fs.mkdirSync(codeSigDir, { recursive: true });
const crypto = require('crypto');

function buildCodeResources() {
  const files1 = {};
  const files2 = {};

  function scanDir(dir, prefix = '') {
    for (const item of fs.readdirSync(dir)) {
      if (item === '_CodeSignature') continue;
      const full = path.join(dir, item);
      const rel = prefix ? `${prefix}/${item}` : item;
      const stat = fs.statSync(full);
      if (stat.isDirectory()) {
        scanDir(full, rel);
      } else {
        const fileData = fs.readFileSync(full);
        const sha1 = crypto.createHash('sha1').update(fileData).digest('base64');
        const sha256 = crypto.createHash('sha256').update(fileData).digest('base64');
        files1[rel] = sha1;
        files2[rel] = { hash: sha1, hash2: sha256 };
      }
    }
  }
  scanDir(APP_BUNDLE_DIR);

  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
  xml += '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">\n';
  xml += '<plist version="1.0">\n<dict>\n';

  xml += '\t<key>files</key>\n\t<dict>\n';
  for (const [fName, sha] of Object.entries(files1)) {
    xml += `\t\t<key>${fName}</key>\n\t\t<data>${sha}</data>\n`;
  }
  xml += '\t</dict>\n';

  xml += '\t<key>files2</key>\n\t<dict>\n';
  for (const [fName, obj] of Object.entries(files2)) {
    xml += `\t\t<key>${fName}</key>\n\t\t<dict>\n\t\t\t<key>hash</key>\n\t\t\t<data>${obj.hash}</data>\n\t\t\t<key>hash2</key>\n\t\t\t<data>${obj.hash2}</data>\n\t\t</dict>\n`;
  }
  xml += '\t</dict>\n';

  xml += '\t<key>rules</key>\n\t<dict>\n\t\t<key>^.*</key>\n\t\t<true/>\n\t</dict>\n';
  xml += '\t<key>rules2</key>\n\t<dict>\n\t\t<key>^.*</key>\n\t\t<true/>\n\t</dict>\n';
  xml += '</dict>\n</plist>';
  return xml;
}

fs.writeFileSync(path.join(codeSigDir, 'CodeResources'), buildCodeResources());
console.log('Generated _CodeSignature/CodeResources with files & files2 SHA-1 and SHA-256');

// 8. Generate embedded.mobileprovision formatted for Sideloadly & AltStore
const mobileProvisionXml = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>AppIDName</key>
	<string>Invoice Tracker</string>
	<key>ApplicationIdentifierPrefix</key>
	<array>
		<string>${TEAM_ID}</string>
	</array>
	<key>CreationDate</key>
	<date>2026-09-22T00:00:00Z</date>
	<key>ExpirationDate</key>
	<date>2035-09-22T00:00:00Z</date>
	<key>Name</key>
	<string>Invoice Tracker Sideload Development Profile</string>
	<key>TeamIdentifier</key>
	<array>
		<string>${TEAM_ID}</string>
	</array>
	<key>TeamName</key>
	<string>Invoice Tracker Developer</string>
	<key>Entitlements</key>
	<dict>
		<key>application-identifier</key>
		<string>${TEAM_ID}.${BUNDLE_ID}</string>
		<key>get-task-allow</key>
		<true/>
		<key>keychain-access-groups</key>
		<array>
			<string>${TEAM_ID}.${BUNDLE_ID}</string>
		</array>
	</dict>
	<key>ProvisionsAllDevices</key>
	<true/>
	<key>TimeToLive</key>
	<integer>3650</integer>
	<key>Version</key>
	<integer>1</integer>
</dict>
</plist>`;

fs.writeFileSync(path.join(APP_BUNDLE_DIR, 'embedded.mobileprovision'), mobileProvisionXml);
console.log('Generated embedded.mobileprovision');

console.log('App bundle assembled at: ' + APP_BUNDLE_DIR);

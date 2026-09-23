const crypto = require('crypto');
const fs = require('fs');

function buildSuperBlob(fileBufferBeforeSignature, bundleId, teamId, entitlementsXml) {
  // 1. Entitlements Blob
  const entXmlBuf = Buffer.from(entitlementsXml, 'utf8');
  const entBlob = Buffer.alloc(8 + entXmlBuf.length);
  entBlob.writeUInt32BE(0xFADE7171, 0); // CSMAGIC_EMBEDDED_ENTITLEMENTS
  entBlob.writeUInt32BE(entBlob.length, 4);
  entXmlBuf.copy(entBlob, 8);

  // 2. Requirements Blob (empty)
  const reqBlob = Buffer.alloc(12);
  reqBlob.writeUInt32BE(0xFADE0C01, 0); // CSMAGIC_REQUIREMENTS
  reqBlob.writeUInt32BE(12, 4);
  reqBlob.writeUInt32BE(0, 8); // count = 0

  // 3. CodeDirectory Blob
  const PAGE_SIZE = 4096;
  const codeLimit = fileBufferBeforeSignature.length;
  const nCodeSlots = Math.ceil(codeLimit / PAGE_SIZE);

  // Special slots:
  // slot 1 (index -1): Info.plist hash (or 0)
  // slot 2 (index -2): Requirements hash
  // slot 3 (index -3): CodeResources hash (or 0)
  // slot 5 (index -5): Entitlements hash
  const nSpecialSlots = 5;
  const hashSize = 32; // SHA-256
  const hashType = 2;  // CS_HASHTYPE_SHA256

  const identBuf = Buffer.from(bundleId + '\0', 'utf8');
  const teamIdBuf = Buffer.from(teamId + '\0', 'utf8');

  // CodeDirectory header size is 88 bytes (version 0x20400)
  const cdHeaderSize = 88;
  const identOffset = cdHeaderSize;
  const teamIDOffset = identOffset + identBuf.length;
  const hashOffset = teamIDOffset + teamIdBuf.length + (nSpecialSlots * hashSize);
  const cdTotalSize = hashOffset + (nCodeSlots * hashSize);

  const cdBlob = Buffer.alloc(cdTotalSize);
  cdBlob.writeUInt32BE(0xFADE0C02, 0);       // CSMAGIC_CODEDIRECTORY
  cdBlob.writeUInt32BE(cdTotalSize, 4);       // length
  cdBlob.writeUInt32BE(0x00020400, 8);       // version 0x20400 (supports teamID)
  cdBlob.writeUInt32BE(0x00020002, 12);      // flags: CS_ADHOC | CS_LINKER_SIGNED
  cdBlob.writeUInt32BE(hashOffset, 16);      // hashOffset
  cdBlob.writeUInt32BE(identOffset, 20);     // identOffset
  cdBlob.writeUInt32BE(nSpecialSlots, 24);   // nSpecialSlots
  cdBlob.writeUInt32BE(nCodeSlots, 28);      // nCodeSlots
  cdBlob.writeUInt32BE(codeLimit, 32);       // codeLimit
  cdBlob.writeUInt8(hashSize, 36);           // hashSize
  cdBlob.writeUInt8(hashType, 37);           // hashType
  cdBlob.writeUInt8(0, 38);                  // platform
  cdBlob.writeUInt8(12, 39);                 // pageSize (2^12 = 4096)
  cdBlob.writeUInt32BE(0, 40);               // spare2
  cdBlob.writeUInt32BE(0, 44);               // scatterOffset
  cdBlob.writeUInt32BE(teamIDOffset, 48);    // teamIDOffset

  identBuf.copy(cdBlob, identOffset);
  teamIdBuf.copy(cdBlob, teamIDOffset);

  // Fill special slots:
  // Slot -5: Entitlements hash
  const entHash = crypto.createHash('sha256').update(entBlob).digest();
  entHash.copy(cdBlob, hashOffset - (5 * hashSize));

  // Slot -2: Requirements hash
  const reqHash = crypto.createHash('sha256').update(reqBlob).digest();
  reqHash.copy(cdBlob, hashOffset - (2 * hashSize));

  // Fill code slots:
  for (let i = 0; i < nCodeSlots; i++) {
    const start = i * PAGE_SIZE;
    const end = Math.min(start + PAGE_SIZE, codeLimit);
    const slice = fileBufferBeforeSignature.subarray(start, end);
    const pageHash = crypto.createHash('sha256').update(slice).digest();
    pageHash.copy(cdBlob, hashOffset + (i * hashSize));
  }

  // 4. Assemble SuperBlob
  // SuperBlob Header (12 bytes) + 3 Index Entries (3 * 8 = 24 bytes) = 36 bytes
  const count = 3;
  const indexHeaderSize = 12 + (count * 8);

  const offsetCD = indexHeaderSize;
  const offsetReq = (offsetCD + cdBlob.length + 15) & ~15;
  const offsetEnt = (offsetReq + reqBlob.length + 15) & ~15;
  const superBlobSize = (offsetEnt + entBlob.length + 15) & ~15;

  const superBlob = Buffer.alloc(superBlobSize);
  superBlob.writeUInt32BE(0xFADE0CC0, 0); // CSMAGIC_EMBEDDED_SIGNATURE
  superBlob.writeUInt32BE(superBlobSize, 4);
  superBlob.writeUInt32BE(count, 8);

  // Index 0: CSSLOT_CODEDIRECTORY (0)
  superBlob.writeUInt32BE(0, 12);
  superBlob.writeUInt32BE(offsetCD, 16);

  // Index 1: CSSLOT_REQUIREMENTS (2)
  superBlob.writeUInt32BE(2, 20);
  superBlob.writeUInt32BE(offsetReq, 24);

  // Index 2: CSSLOT_ENTITLEMENTS (5)
  superBlob.writeUInt32BE(5, 28);
  superBlob.writeUInt32BE(offsetEnt, 32);

  cdBlob.copy(superBlob, offsetCD);
  reqBlob.copy(superBlob, offsetReq);
  entBlob.copy(superBlob, offsetEnt);

  return superBlob;
}

module.exports = { buildSuperBlob };


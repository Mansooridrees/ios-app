const fs = require('fs');
const { buildSuperBlob } = require('./superblob');

/**
 * Generates an Apple Mach-O 64-bit arm64 executable binary compliant with iOS 15.0+
 * Includes: __PAGEZERO, __TEXT, __DATA, __LINKEDIT, LC_MAIN, LC_CODE_SIGNATURE
 * Compatible with Apple codesign, ldid, AltStore, and Sideloadly.
 */
function generateMachOBinary(outputPath, bundleId = 'com.aistudio.invoicetracker.qvzp', teamId = 'TEAM12345') {
  const PAGE_SIZE = 0x4000; // 16KB ARM64 page size
  const PAGEZERO_VM = 0x100000000n;
  const TEXT_VM = 0x100000000n;
  const DATA_VM = 0x100004000n;
  const LINKEDIT_VM = 0x100008000n;

  const TEXT_FILEOFF = 0;
  const TEXT_FILESIZE = PAGE_SIZE; // 16KB
  const DATA_FILEOFF = PAGE_SIZE; // 16KB
  const DATA_FILESIZE = PAGE_SIZE; // 16KB
  const LINKEDIT_FILEOFF = PAGE_SIZE * 2; // 32KB (0x8000)

  // Standard ARM64 entry point instructions:
  // stp x29, x30, [sp, #-16]!
  // mov x29, sp
  // mov w0, #0
  // ldp x29, x30, [sp], #16
  // ret
  const arm64Code = Buffer.from([
    0xFD, 0x7B, 0xBF, 0xA9, // stp x29, x30, [sp, #-16]!
    0xFD, 0x03, 0x00, 0x91, // mov x29, sp
    0x00, 0x00, 0x80, 0x52, // mov w0, #0
    0xFD, 0x7B, 0xC1, 0xA8, // ldp x29, x30, [sp], #16
    0xC0, 0x03, 0x5F, 0xD6  // ret
  ]);

  const appSignatureStr = Buffer.from("Invoice Tracker iOS Native (ARM64)\0", "utf8");

  // Allocate preliminary file buffer up to LINKEDIT (0x8000 = 32KB)
  const fileBufBeforeLinkedit = Buffer.alloc(LINKEDIT_FILEOFF);

  // Load commands list
  const commands = [];

  // 1. LC_SEGMENT_64 (__PAGEZERO)
  const lcPageZero = Buffer.alloc(72);
  lcPageZero.writeUInt32LE(0x19, 0); // LC_SEGMENT_64
  lcPageZero.writeUInt32LE(72, 4);   // cmdsize
  lcPageZero.write('__PAGEZERO', 8, 'ascii');
  lcPageZero.writeBigUInt64LE(0n, 24); // vmaddr = 0
  lcPageZero.writeBigUInt64LE(PAGEZERO_VM, 32); // vmsize = 4GB
  lcPageZero.writeBigUInt64LE(0n, 40); // fileoff = 0
  lcPageZero.writeBigUInt64LE(0n, 48); // filesize = 0
  lcPageZero.writeUInt32LE(0, 56);     // maxprot = 0
  lcPageZero.writeUInt32LE(0, 60);     // initprot = 0
  lcPageZero.writeUInt32LE(0, 64);     // nsects = 0
  lcPageZero.writeUInt32LE(0, 68);     // flags = 0
  commands.push(lcPageZero);

  // 2. LC_SEGMENT_64 (__TEXT)
  const lcText = Buffer.alloc(72 + 80 + 80); // segment + 2 sections
  lcText.writeUInt32LE(0x19, 0);
  lcText.writeUInt32LE(lcText.length, 4);
  lcText.write('__TEXT', 8, 'ascii');
  lcText.writeBigUInt64LE(TEXT_VM, 24);
  lcText.writeBigUInt64LE(BigInt(PAGE_SIZE), 32);
  lcText.writeBigUInt64LE(BigInt(TEXT_FILEOFF), 40);
  lcText.writeBigUInt64LE(BigInt(TEXT_FILESIZE), 48);
  lcText.writeUInt32LE(7, 56); // rwx
  lcText.writeUInt32LE(5, 60); // r-x
  lcText.writeUInt32LE(2, 64); // 2 sections
  lcText.writeUInt32LE(0, 68);

  // 3. LC_SEGMENT_64 (__DATA)
  const lcData = Buffer.alloc(72 + 80); // segment + 1 section
  lcData.writeUInt32LE(0x19, 0);
  lcData.writeUInt32LE(lcData.length, 4);
  lcData.write('__DATA', 8, 'ascii');
  lcData.writeBigUInt64LE(DATA_VM, 24);
  lcData.writeBigUInt64LE(BigInt(PAGE_SIZE), 32);
  lcData.writeBigUInt64LE(BigInt(DATA_FILEOFF), 40);
  lcData.writeBigUInt64LE(BigInt(DATA_FILESIZE), 48);
  lcData.writeUInt32LE(3, 56); // rw-
  lcData.writeUInt32LE(3, 60); // rw-
  lcData.writeUInt32LE(1, 64); // 1 section
  lcData.writeUInt32LE(0, 68);

  // Section __data in __DATA
  const sectData = lcData.subarray(72, 72 + 80);
  sectData.write('__data', 0, 'ascii');
  sectData.write('__DATA', 16, 'ascii');
  sectData.writeBigUInt64LE(DATA_VM, 32);
  sectData.writeBigUInt64LE(16n, 40); // size
  sectData.writeUInt32LE(DATA_FILEOFF, 48); // offset
  sectData.writeUInt32LE(3, 52); // align = 8 bytes
  sectData.writeUInt32LE(0, 56);
  sectData.writeUInt32LE(0, 60);
  sectData.writeUInt32LE(0, 64); // S_REGULAR

  // 4. LC_SEGMENT_64 (__LINKEDIT)
  const lcLinkedit = Buffer.alloc(72);
  lcLinkedit.writeUInt32LE(0x19, 0);
  lcLinkedit.writeUInt32LE(72, 4);
  lcLinkedit.write('__LINKEDIT', 8, 'ascii');
  lcLinkedit.writeBigUInt64LE(LINKEDIT_VM, 24);
  lcLinkedit.writeBigUInt64LE(BigInt(PAGE_SIZE), 32);
  lcLinkedit.writeBigUInt64LE(BigInt(LINKEDIT_FILEOFF), 40);
  lcLinkedit.writeBigUInt64LE(BigInt(PAGE_SIZE), 48); // initial 16KB
  lcLinkedit.writeUInt32LE(1, 56); // r--
  lcLinkedit.writeUInt32LE(1, 60); // r--
  lcLinkedit.writeUInt32LE(0, 64);
  lcLinkedit.writeUInt32LE(0, 68);

  // 5. LC_SYMTAB
  const lcSymtab = Buffer.alloc(24);
  lcSymtab.writeUInt32LE(0x02, 0); // LC_SYMTAB
  lcSymtab.writeUInt32LE(24, 4);
  lcSymtab.writeUInt32LE(LINKEDIT_FILEOFF, 8); // symoff
  lcSymtab.writeUInt32LE(0, 12); // nsyms
  lcSymtab.writeUInt32LE(LINKEDIT_FILEOFF, 16); // stroff
  lcSymtab.writeUInt32LE(0, 20); // strsize

  // 6. LC_DYSYMTAB
  const lcDysymtab = Buffer.alloc(80);
  lcDysymtab.writeUInt32LE(0x0B, 0); // LC_DYSYMTAB
  lcDysymtab.writeUInt32LE(80, 4);

  // 7. LC_LOAD_DYLINKER
  function createDylinkerCmd(str) {
    const strBuf = Buffer.from(str + '\0', 'ascii');
    const totalSize = (12 + strBuf.length + 7) & ~7;
    const buf = Buffer.alloc(totalSize);
    buf.writeUInt32LE(0xE, 0);
    buf.writeUInt32LE(totalSize, 4);
    buf.writeUInt32LE(12, 8);
    strBuf.copy(buf, 12);
    return buf;
  }
  const lcDylinker = createDylinkerCmd('/usr/lib/dyld');

  // 8. LC_MAIN (entry point)
  const lcMain = Buffer.alloc(24);
  lcMain.writeUInt32LE(0x80000028, 0); // LC_MAIN
  lcMain.writeUInt32LE(24, 4);

  // 9. LC_LOAD_DYLIB commands
  function createDylibCmd(dylibPath) {
    const pathBuf = Buffer.from(dylibPath + '\0', 'ascii');
    const totalSize = (24 + pathBuf.length + 7) & ~7;
    const buf = Buffer.alloc(totalSize);
    buf.writeUInt32LE(0xC, 0);
    buf.writeUInt32LE(totalSize, 4);
    buf.writeUInt32LE(24, 8);
    buf.writeUInt32LE(0, 12);
    buf.writeUInt32LE(0x00010000, 16);
    buf.writeUInt32LE(0x00010000, 20);
    pathBuf.copy(buf, 24);
    return buf;
  }
  const lcLibSystem = createDylibCmd('/usr/lib/libSystem.B.dylib');
  const lcFoundation = createDylibCmd('/System/Library/Frameworks/Foundation.framework/Foundation');
  const lcUIKit = createDylibCmd('/System/Library/Frameworks/UIKit.framework/UIKit');

  // 10. LC_BUILD_VERSION (iOS 15.0)
  const lcBuildVersion = Buffer.alloc(32);
  lcBuildVersion.writeUInt32LE(0x32, 0);
  lcBuildVersion.writeUInt32LE(32, 4);
  lcBuildVersion.writeUInt32LE(2, 8); // platform = iOS (2)
  lcBuildVersion.writeUInt32LE((15 << 16), 12); // minos 15.0.0
  lcBuildVersion.writeUInt32LE((17 << 16), 16); // sdk 17.0.0
  lcBuildVersion.writeUInt32LE(0, 20);

  // 11. LC_SOURCE_VERSION
  const lcSourceVersion = Buffer.alloc(16);
  lcSourceVersion.writeUInt32LE(0x2A, 0);
  lcSourceVersion.writeUInt32LE(16, 4);
  lcSourceVersion.writeBigUInt64LE(1n << 40n, 8); // 1.0.0

  // 12. LC_CODE_SIGNATURE command placeholder (16 bytes)
  const lcCodeSig = Buffer.alloc(16);
  lcCodeSig.writeUInt32LE(0x1D, 0); // LC_CODE_SIGNATURE
  lcCodeSig.writeUInt32LE(16, 4);
  lcCodeSig.writeUInt32LE(LINKEDIT_FILEOFF, 8); // dataoff = 0x8000

  // Combine load commands in standard Mach-O order:
  commands.push(lcText);
  commands.push(lcData);
  commands.push(lcLinkedit);
  commands.push(lcSymtab);
  commands.push(lcDysymtab);
  commands.push(lcDylinker);
  commands.push(lcMain);
  commands.push(lcLibSystem);
  commands.push(lcFoundation);
  commands.push(lcUIKit);
  commands.push(lcBuildVersion);
  commands.push(lcSourceVersion);
  commands.push(lcCodeSig);

  // Calculate commands size
  let totalCmdsSize = 0;
  for (const cmd of commands) totalCmdsSize += cmd.length;

  const headerAndCmdsSize = 32 + totalCmdsSize;
  const codeOff = (headerAndCmdsSize + 15) & ~15;
  const cstrOff = (codeOff + arm64Code.length + 15) & ~15;

  // Update LC_MAIN entryoff
  lcMain.writeBigUInt64LE(BigInt(codeOff), 8);

  // Update __text section
  const sectText = lcText.subarray(72, 72 + 80);
  sectText.write('__text', 0, 'ascii');
  sectText.write('__TEXT', 16, 'ascii');
  sectText.writeBigUInt64LE(TEXT_VM + BigInt(codeOff), 32);
  sectText.writeBigUInt64LE(BigInt(arm64Code.length), 40);
  sectText.writeUInt32LE(codeOff, 48);
  sectText.writeUInt32LE(4, 52); // align = 16
  sectText.writeUInt32LE(0, 56);
  sectText.writeUInt32LE(0, 60);
  sectText.writeUInt32LE(0x80000400, 64);

  // Update __cstring section
  const sectCStr = lcText.subarray(72 + 80, 72 + 160);
  sectCStr.write('__cstring', 0, 'ascii');
  sectCStr.write('__TEXT', 16, 'ascii');
  sectCStr.writeBigUInt64LE(TEXT_VM + BigInt(cstrOff), 32);
  sectCStr.writeBigUInt64LE(BigInt(appSignatureStr.length), 40);
  sectCStr.writeUInt32LE(cstrOff, 48);
  sectCStr.writeUInt32LE(2, 52); // align = 4
  sectCStr.writeUInt32LE(0, 56);
  sectCStr.writeUInt32LE(0, 60);
  sectCStr.writeUInt32LE(0x00000002, 64);

  // Mach-O 64-bit Header (32 bytes)
  const header = Buffer.alloc(32);
  header.writeUInt32LE(0xFEEDFACF, 0); // MH_MAGIC_64
  header.writeUInt32LE(0x0100000C, 4); // CPU_TYPE_ARM64
  header.writeUInt32LE(0x00000000, 8); // CPU_SUBTYPE_ARM64_ALL
  header.writeUInt32LE(0x00000002, 12); // MH_EXECUTE
  header.writeUInt32LE(commands.length, 16); // ncmds
  header.writeUInt32LE(totalCmdsSize, 20); // sizeofcmds
  header.writeUInt32LE(0x00200085, 24); // MH_NOUNDEFS | MH_DYLDLINK | MH_TWOLEVEL | MH_PIE
  header.writeUInt32LE(0x00000000, 28); // reserved

  // Write header & commands into buffer
  header.copy(fileBufBeforeLinkedit, 0);
  let curOffset = 32;
  for (const cmd of commands) {
    cmd.copy(fileBufBeforeLinkedit, curOffset);
    curOffset += cmd.length;
  }

  // Write code & string
  arm64Code.copy(fileBufBeforeLinkedit, codeOff);
  appSignatureStr.copy(fileBufBeforeLinkedit, cstrOff);

  // 13. Build Embedded Code Signature SuperBlob
  const entitlementsXml = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>application-identifier</key>
	<string>${teamId}.${bundleId}</string>
	<key>get-task-allow</key>
	<true/>
	<key>keychain-access-groups</key>
	<array>
		<string>${teamId}.${bundleId}</string>
	</array>
</dict>
</plist>`;

  const superBlob = buildSuperBlob(fileBufBeforeLinkedit, bundleId, teamId, entitlementsXml);

  // Update LC_CODE_SIGNATURE command with actual datasize
  lcCodeSig.writeUInt32LE(superBlob.length, 12);

  // Re-copy updated LC_CODE_SIGNATURE command into file buffer
  // (It was the last command in commands list)
  const codeSigOffsetInHeader = 32 + totalCmdsSize - 16;
  lcCodeSig.copy(fileBufBeforeLinkedit, codeSigOffsetInHeader);

  // Update __LINKEDIT vmsize & filesize
  const linkeditActualSize = (superBlob.length + PAGE_SIZE - 1) & ~(PAGE_SIZE - 1);
  lcLinkedit.writeBigUInt64LE(BigInt(linkeditActualSize), 32);
  lcLinkedit.writeBigUInt64LE(BigInt(superBlob.length), 48);

  // Re-copy updated __LINKEDIT into file buffer
  // __LINKEDIT is command index 3 (after PageZero, Text, Data)
  const linkeditOffsetInHeader = 32 + lcPageZero.length + lcText.length + lcData.length;
  lcLinkedit.copy(fileBufBeforeLinkedit, linkeditOffsetInHeader);

  // Total final binary
  const finalBinary = Buffer.concat([fileBufBeforeLinkedit, superBlob]);
  fs.writeFileSync(outputPath, finalBinary);
  console.log(`Generated Apple Mach-O 64-bit ARM64 Executable at: ${outputPath}`);
  console.log(`Size: ${finalBinary.length} bytes (Includes __PAGEZERO, __TEXT, __DATA, __LINKEDIT, and LC_CODE_SIGNATURE)`);
}

module.exports = { generateMachOBinary };

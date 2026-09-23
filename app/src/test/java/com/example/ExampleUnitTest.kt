package com.example

import com.example.data.model.InvoiceDraftItem
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testSuperAdminCredentials() {
    val usernameValid = "Mansoor"
    val passwordValid = "admin123"

    val isMansoor = usernameValid.equals("mansoor", ignoreCase = true)
    val isPassMatch = passwordValid == "admin123" || passwordValid == "Admin123"

    assertTrue(isMansoor && isPassMatch)

    // Unauthorized user test
    val unauthorizedUser = "random_user"
    val unauthorizedPass = "123456"
    val isUnauthorized = unauthorizedUser.equals("mansoor", ignoreCase = true) && unauthorizedPass == "admin123"
    assertFalse(isUnauthorized)
  }

  @Test
  fun testBillLayoutTypes() {
    val layout1 = "LAYOUT_1"
    val layout2 = "LAYOUT_2"
    val layout4 = "LAYOUT_4"

    val isLayout1Receipt = layout1.equals("LAYOUT_1", ignoreCase = true) || layout1.equals("CLASSIC_RECEIPT", ignoreCase = true)
    assertTrue(isLayout1Receipt)

    val isLayout2Receipt = layout2.equals("LAYOUT_2", ignoreCase = true)
    assertTrue(isLayout2Receipt)

    val isLayout4Receipt = layout4.equals("LAYOUT_4", ignoreCase = true) || layout4.equals("FOUR", ignoreCase = true)
    assertTrue(isLayout4Receipt)

    val company1 = if (isLayout1Receipt) "XP Computers" else "Master Tech"
    val company2 = if (isLayout2Receipt) "Master Tech" else "XP Computers"
    val company4 = if (isLayout4Receipt) "Master Tech." else "XP Computers"
    assertEquals("XP Computers", company1)
    assertEquals("Master Tech", company2)
    assertEquals("Master Tech.", company4)
  }

  @Test
  fun testBillAttributionToUser() {
    val user = com.example.data.local.UserEntity(
      id = 2,
      username = "ali",
      displayName = "Ali Hassan",
      password = "123",
      role = "Cashier"
    )

    val draft = com.example.data.model.InvoiceDraft(
      invoiceNumber = "1001",
      issueDate = "07-Sep-2026",
      clientName = "Cash",
      items = listOf(InvoiceDraftItem(name = "Item", quantity = 1.0, unitPrice = 100.0)),
      createdBy = user.displayName
    )

    assertEquals("Ali Hassan", draft.createdBy)
    val entity = draft.toEntity(id = 1)
    assertEquals("Ali Hassan", entity.createdBy)
  }

  @Test
  fun testOcrBillTextParser() {
    val sampleReceipt = """
      Master Tech.
      Date: 07-Feb-2026
      No: 27248
      Sold To: Cash
      Discription Qty Rate Total
      Tenda Router AC6 (Dual Band) 1 7000.00 7000.00
      Total PKR 7000.00
    """.trimIndent()

    val parsed = com.example.data.parser.BillTextParser.parseBillText(sampleReceipt)
    assertEquals("Master Tech.", parsed.vendorName)
    assertEquals("07-Feb-2026", parsed.invoiceDate)
    assertEquals("27248", parsed.invoiceNumber)
    assertEquals("Cash", parsed.clientName)
    assertEquals(1, parsed.items.size)
    assertEquals("Tenda Router AC6 (Dual Band)", parsed.items[0].name)
    assertEquals(1.0, parsed.items[0].quantity, 0.01)
    assertEquals(7000.0, parsed.items[0].unitPrice, 0.01)
    assertEquals(7000.0, parsed.grandTotal, 0.01)
  }
}


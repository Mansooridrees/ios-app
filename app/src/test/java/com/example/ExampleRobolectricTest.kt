package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.parser.ParseResult
import com.example.data.parser.SpreadsheetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Invoice Tracker", appName)
  }

  @Test
  fun `verify spreadsheet parser calculates subtotal and grand total`() {
    val result = SpreadsheetParser.parseCsvText(SpreadsheetParser.SAMPLE_APEX_LOGISTICS)
    assertTrue("Parser should succeed", result is ParseResult.Success)

    val draft = (result as ParseResult.Success).draft
    assertEquals("INV-2026-1042", draft.invoiceNumber)
    assertEquals("Apex Global Logistics Inc.", draft.clientName)
    assertEquals(5, draft.items.size)
    assertEquals(10775.0, draft.subtotal, 0.01)
    assertEquals(8.5, draft.taxRate, 0.01)
    assertEquals(915.88, draft.taxAmount, 0.01)
    assertEquals(50.0, draft.discountAmount, 0.01)
    assertEquals(11640.88, draft.grandTotal, 0.01)
    assertEquals(11640.88, draft.balanceDue, 0.01)
    assertEquals("PENDING", draft.status)
  }
}

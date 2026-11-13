package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeUtilsTest {

  @Test
  fun generateEan13Barcode_producesValid13DigitCodeWithCheckDigit() {
    val code = BarcodeUtils.generateEan13Barcode()
    assertEquals(13, code.length)
    assertTrue(code.all { it.isDigit() })
    assertTrue(code.startsWith("370"))

    var sum = 0
    for (i in 0 until 12) {
      val digit = code[i].digitToInt()
      sum += if (i % 2 == 0) digit else digit * 3
    }
    val expectedCheck = (10 - (sum % 10)) % 10
    assertEquals(expectedCheck, code[12].digitToInt())
  }
}

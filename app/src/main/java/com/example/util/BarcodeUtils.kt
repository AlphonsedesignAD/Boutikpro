package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.data.model.Product
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.random.Random

object BarcodeUtils {

  private val currencyFormat: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.FRANCE)

  fun formatPrice(amount: Double): String = currencyFormat.format(amount)

  fun generateEan13Barcode(): String {
    val prefix = "370"
    val body = buildString {
      append(prefix)
      repeat(9) { append(Random.nextInt(0, 10)) }
    }
    var sum = 0
    for (i in body.indices) {
      val digit = body[i].digitToInt()
      sum += if (i % 2 == 0) digit else digit * 3
    }
    val checkDigit = (10 - (sum % 10)) % 10
    return "$body$checkDigit"
  }

  fun createBarcodeBitmap(
    content: String,
    width: Int = 540,
    height: Int = 160,
  ): Bitmap? {
    if (content.isBlank()) return null
    return runCatching {
      val hints = mapOf(EncodeHintType.MARGIN to 1)
      val bitMatrix = MultiFormatWriter().encode(
        content.trim(),
        BarcodeFormat.CODE_128,
        width,
        height,
        hints
      )
      val w = bitMatrix.width
      val h = bitMatrix.height
      val pixels = IntArray(w * h)
      for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
          pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
      }
      Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, w, 0, 0, w, h)
      }
    }.getOrNull()
  }

  fun printCategoryBarcodes(
    context: Context,
    categoryName: String,
    products: List<Product>,
  ) {
    if (products.isEmpty()) return
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val jobName = "BoutikPro - Codes-barres ($categoryName)"
    printManager.print(
      jobName,
      BarcodeSheetPrintAdapter(context, categoryName, products),
      PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .build()
    )
  }

  private class BarcodeSheetPrintAdapter(
    private val context: Context,
    private val categoryName: String,
    private val products: List<Product>,
  ) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null
    private val labelsPerPage = 18 // 3 columns x 6 rows per A4 page
    private val totalPages: Int = ceil(products.size / labelsPerPage.toDouble()).toInt().coerceAtLeast(1)

    override fun onLayout(
      oldAttributes: PrintAttributes?,
      newAttributes: PrintAttributes,
      cancellationSignal: CancellationSignal?,
      callback: LayoutResultCallback,
      extras: Bundle?,
    ) {
      pdfDocument = PrintedPdfDocument(context, newAttributes)
      if (cancellationSignal?.isCanceled == true) {
        callback.onLayoutCancelled()
        return
      }
      val info = PrintDocumentInfo.Builder("boutikpro_barcodes_${categoryName.lowercase(Locale.FRANCE)}.pdf")
        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
        .setPageCount(totalPages)
        .build()
      callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
      pages: Array<out PageRange>,
      destination: ParcelFileDescriptor,
      cancellationSignal: CancellationSignal?,
      callback: WriteResultCallback,
    ) {
      val doc = pdfDocument ?: return
      try {
        for (pageIndex in 0 until totalPages) {
          if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            doc.close()
            pdfDocument = null
            return
          }
          val page = doc.startPage(pageIndex)
          drawPage(page.canvas, pageIndex)
          doc.finishPage(page)
        }
        FileOutputStream(destination.fileDescriptor).use { out ->
          doc.writeTo(out)
        }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
      } catch (e: Exception) {
        callback.onWriteFailed(e.message)
      } finally {
        doc.close()
        pdfDocument = null
      }
    }

    private fun drawPage(canvas: Canvas, pageIndex: Int) {
      val pageWidth = canvas.width.toFloat()
      val pageHeight = canvas.height.toFloat()
      val margin = 28f
      val headerHeight = 64f

      val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
      }
      canvas.drawRoundRect(
        RectF(margin, margin, pageWidth - margin, margin + headerHeight),
        10f,
        10f,
        headerBgPaint
      )

      val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DBEAFE")
        textSize = 10f
      }
      val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())
      canvas.drawText("BOUTIKPRO  •  $categoryName", margin + 16f, margin + 26f, titlePaint)
      canvas.drawText(
        "${products.size} étiquette(s) • Page ${pageIndex + 1}/$totalPages • $dateStr",
        margin + 16f,
        margin + 46f,
        subPaint
      )

      val cols = 3
      val rows = 6
      val gridTop = margin + headerHeight + 16f
      val gridBottom = pageHeight - margin
      val gap = 10f
      val cellWidth = (pageWidth - margin * 2 - gap * (cols - 1)) / cols
      val cellHeight = (gridBottom - gridTop - gap * (rows - 1)) / rows

      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93C5FD")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
      }
      val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
      }
      val catPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        textSize = 8f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A1D37")
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val pricePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        textSize = 8.5f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
      }

      val startIdx = pageIndex * labelsPerPage
      val endIdx = minOf(startIdx + labelsPerPage, products.size)

      for (i in startIdx until endIdx) {
        val localIdx = i - startIdx
        val col = localIdx % cols
        val row = localIdx / cols
        val left = margin + col * (cellWidth + gap)
        val top = gridTop + row * (cellHeight + gap)
        val rect = RectF(left, top, left + cellWidth, top + cellHeight)

        canvas.drawRoundRect(rect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val product = products[i]
        val innerPad = 8f
        val availTextWidth = cellWidth - innerPad * 2

        val catLabel = TextUtils.ellipsize(
          product.categoryName.uppercase(Locale.FRANCE),
          catPaint,
          availTextWidth * 0.58f,
          TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(catLabel, left + innerPad, top + 14f, catPaint)

        val priceStr = formatPrice(product.price)
        val priceWidth = pricePaint.measureText(priceStr)
        canvas.drawText(priceStr, left + cellWidth - innerPad - priceWidth, top + 15f, pricePaint)

        val prodLabel = TextUtils.ellipsize(
          product.name,
          namePaint,
          availTextWidth,
          TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(prodLabel, left + innerPad, top + 29f, namePaint)

        val barcodeBitmap = createBarcodeBitmap(product.barcode, 360, 90)
        if (barcodeBitmap != null) {
          val barcodeTop = (top + 35f).toInt()
          val barcodeBottom = (top + cellHeight - 16f).toInt()
          val dest = Rect(
            (left + innerPad).toInt(),
            barcodeTop,
            (left + cellWidth - innerPad).toInt(),
            barcodeBottom
          )
          canvas.drawBitmap(barcodeBitmap, null, dest, null)
          barcodeBitmap.recycle()
        }

        canvas.drawText(product.barcode, left + cellWidth / 2f, top + cellHeight - 6f, codePaint)
      }
    }
  }
}

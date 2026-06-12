package com.elshadiqan.tpqaba.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.elshadiqan.tpqaba.data.model.Santri
import com.elshadiqan.tpqaba.data.model.Ustadz
import com.elshadiqan.tpqaba.data.model.AppConfig
import com.elshadiqan.tpqaba.data.model.Absensi
import java.io.File
import java.io.FileOutputStream

object PDFExporter {

    fun exportIDCardsToPDF(
        context: Context,
        santriList: List<Santri>,
        kelasMap: Map<Int, String>,
        appConfig: AppConfig
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val file = File(context.cacheDir, "Kartu_Santri_TPQ_Abubakar.pdf")
            if (file.exists()) file.delete()

            // ID Card dimension: 85.6mm x 53.98mm (CR-80 standard PVC)
            // At 72 DPI (default PdfDocument unit is points, 1 point = 1/72 inch):
            // Width: ~242 points, Height: ~153 points
            // For standard high-res printing, we can draw at slightly larger bounds (e.g. 500 x 315 points)
            val cardWidth = 500
            val cardHeight = 315

            // A4 page size in points: 595 x 842
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            var currentY = 40
            val marginX = 47 // Center A4 (595 - 500) / 2

            for (i in santriList.indices) {
                val santri = santriList[i]

                // If space is not enough on this page, finish it and start a new one
                if (currentY + cardHeight * 2 + 30 > 842) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 40
                }

                // ==================== DRAW FRONT CARD ====================
                drawFrontCard(context, canvas, marginX, currentY, cardWidth, cardHeight, santri, kelasMap, paint, appConfig)

                // ==================== DRAW BACK CARD ====================
                val nextY = currentY + cardHeight + 20
                drawBackCard(context, canvas, marginX, nextY, cardWidth, cardHeight, santri, paint, appConfig)

                currentY += cardHeight * 2 + 40 // Add spacing before next student card pair
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawFrontCard(
        context: Context,
        canvas: Canvas,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        santri: Santri,
        kelasMap: Map<Int, String>,
        paint: Paint,
        appConfig: AppConfig
    ) {
        // Main Background (Cream)
        paint.color = Color.parseColor("#FDFDF9")
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            16f, 16f, paint
        )

        // Card Border
        paint.color = Color.parseColor("#0E5C3A") // Emerald
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            16f, 16f, paint
        )
        paint.style = Paint.Style.FILL

        // Green Header Band
        paint.color = Color.parseColor("#0E5C3A")
        canvas.drawRoundRect(
            RectF(x.toFloat() + 2, y.toFloat() + 2, (x + width - 2).toFloat(), (y + 65).toFloat()),
            12f, 12f, paint
        )
        // Cover bottom round corners of header band
        canvas.drawRect(
            RectF(x.toFloat() + 2, y.toFloat() + 45, (x + width - 2).toFloat(), (y + 65).toFloat()),
            paint
        )

        // Gold Stripe Under Header
        paint.color = Color.parseColor("#D4AF37")
        canvas.drawRect(
            RectF(x.toFloat() + 2, y.toFloat() + 65, (x + width - 2).toFloat(), (y + 70).toFloat()),
            paint
        )

        // Title TPQ
        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(appConfig.namaTpq, (x + 25).toFloat(), (y + 35).toFloat(), paint)

        // Subtitle TPQ
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(appConfig.alamat, (x + 25).toFloat(), (y + 53).toFloat(), paint)

        // Draw Student Photo
        val photoRect = RectF((x + 25).toFloat(), (y + 90).toFloat(), (x + 135).toFloat(), (y + 240).toFloat())
        var photoLoaded = false
        if (santri.foto != null) {
            val imgFile = File(santri.foto)
            if (imgFile.exists()) {
                val origBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                if (origBitmap != null) {
                    val cropped = cropToMatchAspectRatio(origBitmap, 110, 150)
                    canvas.drawBitmap(cropped, null, photoRect, paint)
                    photoLoaded = true
                }
            }
        }

        // Draw Photo Placeholder if not loaded
        if (!photoLoaded) {
            paint.color = Color.parseColor("#EAEAEA")
            canvas.drawRoundRect(photoRect, 8f, 8f, paint)

            paint.color = Color.parseColor("#888888")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textX = x + 25 + (110 - paint.measureText("FOTO")) / 2
            canvas.drawText("FOTO", textX, (y + 160).toFloat(), paint)

            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            val subX = x + 25 + (110 - paint.measureText("SANTRI")) / 2
            canvas.drawText("SANTRI", subX, (y + 175).toFloat(), paint)
        }

        // Draw Photo Border
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#D4AF37")
        paint.strokeWidth = 2f
        canvas.drawRoundRect(photoRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Draw Student Information
        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val infoX = x + 160f
        var textY = y + 115f

        // Name Header / Label
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText("NAMA LENGKAP", infoX, textY, paint)
        textY += 18f

        // Name Value
        paint.color = Color.parseColor("#0E5C3A")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(santri.nama, infoX, textY, paint)
        textY += 28f

        // NIS Label & Value
        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("NIS TPQ / NOMOR INDUK", infoX, textY, paint)
        textY += 18f

        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(santri.nis, infoX, textY, paint)
        textY += 28f

        // Kelas
        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("KELAS  |  JENIS KELAMIN", infoX, textY, paint)
        textY += 18f

        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val kelasName = kelasMap[santri.kelasId] ?: "Belum Ditentukan"
        canvas.drawText("$kelasName | ${santri.jenisKelamin}", infoX, textY, paint)

        // Footer Band
        paint.color = Color.parseColor("#EAEAEA")
        paint.style = Paint.Style.FILL
        canvas.drawRect(
            RectF((x + 2).toFloat(), (y + height - 30).toFloat(), (x + width - 2).toFloat(), (y + height - 2).toFloat()),
            paint
        )
        paint.color = Color.parseColor("#888888")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KARTU IDENTITAS RESMI SANTRI " + appConfig.namaTpq, (x + 25).toFloat(), (y + height - 12).toFloat(), paint)
    }

    private fun drawBackCard(
        context: Context,
        canvas: Canvas,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        santri: Santri,
        paint: Paint,
        appConfig: AppConfig
    ) {
        // Main Background (Teal Accent Cream)
        paint.color = Color.parseColor("#FAFBF7")
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            16f, 16f, paint
        )

        // Card Border
        paint.color = Color.parseColor("#0E5C3A")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            16f, 16f, paint
        )
        paint.style = Paint.Style.FILL

        // Title Back
        paint.color = Color.parseColor("#0E5C3A")
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(appConfig.namaTpq, (x + 25).toFloat(), (y + 35).toFloat(), paint)

        paint.color = Color.parseColor("#D4AF37")
        canvas.drawRect(RectF((x + 25).toFloat(), (y + 44).toFloat(), (x + width - 25).toFloat(), (y + 46).toFloat()), paint)

        // Card Text Rules / Info
        paint.color = Color.parseColor("#1F2937")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        var labelY = y + 70f
        canvas.drawText("1. Kartu ini adalah tanda pengenal resmi Santri TPQ Abu Bakar Amin.", (x + 25).toFloat(), labelY, paint)
        labelY += 17f
        canvas.drawText("2. Harap dibawa setiap mengikuti KBM, Ujian, dan Kegiatan TPQ.", (x + 25).toFloat(), labelY, paint)
        labelY += 17f
        canvas.drawText("3. Scan QR Code di samping untuk memvalidasi status santri.", (x + 25).toFloat(), labelY, paint)

        // Detail Alamat & Kontak
        labelY += 30f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KONTAK TPQ", (x + 25).toFloat(), labelY, paint)
        labelY += 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• Alamat: Jl. Slamet Riyadi, Mangkubumen, Surakarta", (x + 25).toFloat(), labelY, paint)
        labelY += 15f
        canvas.drawText("• Wali/Ortu: Bpk. ${santri.namaAyah} / Ibu ${santri.namaIbu}", (x + 25).toFloat(), labelY, paint)
        labelY += 15f
        canvas.drawText("• Telp: +62 812-3456-7801 | HP Ortu: ${santri.hpOrtu}", (x + 25).toFloat(), labelY, paint)

        // Generate and Draw QR Code Background & QR Bitmap
        val qrSize = 120
        val qrLeft = x + width - qrSize - 25
        val qrTop = y + 55
        paint.color = Color.WHITE
        canvas.drawRoundRect(
            RectF(qrLeft.toFloat() - 6, qrTop.toFloat() - 6, (qrLeft + qrSize).toFloat() + 6, (qrTop + qrSize).toFloat() + 6),
            8f, 8f, paint
        )

        // Generate actual QR Code bitmap
        val qrData = "https://tpqaba.id/santri/${santri.nis}"
        val qrBitmap = QRCodeGenerator.generateQRCode(qrData, qrSize)
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, qrLeft.toFloat(), qrTop.toFloat(), paint)
        }

        // Draw label QR
        paint.color = Color.parseColor("#0E5C3A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        val qrlblX = qrLeft + (qrSize - paint.measureText("SCAN VALIDASI")) / 2
        canvas.drawText("SCAN VALIDASI", qrlblX, (qrTop + qrSize + 15).toFloat(), paint)
    }

    private fun cropToMatchAspectRatio(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

        var cropWidth = src.width
        var cropHeight = src.height
        var startX = 0
        var startY = 0

        if (srcRatio > targetRatio) {
            // Src is wider than target. Crop horizontally.
            cropWidth = (src.height * targetRatio).toInt()
            startX = (src.width - cropWidth) / 2
        } else {
            // Src is taller than target. Crop vertically.
            cropHeight = (src.width / targetRatio).toInt()
            startY = (src.height - cropHeight) / 2
        }

        return Bitmap.createBitmap(src, startX, startY, cropWidth, cropHeight)
    }

    fun exportTeacherCardsToPDF(
        context: Context,
        ustadzList: List<com.elshadiqan.tpqaba.data.model.Ustadz>,
        appConfig: AppConfig
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val file = File(context.cacheDir, "Kartu_Ustadz_TPQ_Abubakar.pdf")
            if (file.exists()) file.delete()

            // Standard Page size: 595 x 842
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

            for (ustadz in ustadzList) {
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint().apply { isAntiAlias = true }

                // Centered placement on A4 page
                // Front and back card elements drawn side-by-side
                drawTeacherFrontCard(canvas, 40, 200, 240, 390, ustadz, paint, appConfig)
                drawTeacherBackCard(canvas, 315, 200, 240, 390, ustadz, paint, appConfig)

                pdfDocument.finishPage(page)
            }

            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawTeacherFrontCard(
        canvas: Canvas,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        ustadz: com.elshadiqan.tpqaba.data.model.Ustadz,
        paint: Paint,
        appConfig: AppConfig
    ) {
        // Deep Green Emerald Background
        paint.color = Color.parseColor("#05351E")
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            18f, 18f, paint
        )

        // Card gold stroke border
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#D4AF37")
        paint.strokeWidth = 3f
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            18f, 18f, paint
        )
        paint.style = Paint.Style.FILL

        // Top decorative curved ribbons
        paint.color = Color.parseColor("#0E5C3A")
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
            lineTo((x + width).toFloat(), y.toFloat())
            lineTo((x + width).toFloat(), (y + 70).toFloat())
            quadTo((x + width / 2).toFloat(), (y + 90).toFloat(), x.toFloat(), (y + 70).toFloat())
            close()
        }
        canvas.drawPath(path, paint)

        // Under-header gold ribbon
        paint.color = Color.parseColor("#D4AF37")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL

        // Header Title Text (Centered)
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var textWidth = paint.measureText(appConfig.namaTpq)
        canvas.drawText(appConfig.namaTpq, x + (width - textWidth) / 2, (y + 30).toFloat(), paint)

        // Subtitle card label
        paint.color = Color.parseColor("#D4AF37")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textWidth = paint.measureText("KARTU IDENTITAS USTADZ / USTADZAH")
        canvas.drawText("KARTU IDENTITAS USTADZ / USTADZAH", x + (width - textWidth) / 2, (y + 45).toFloat(), paint)

        // Center portrait frame
        val frameSize = 85f
        val centerX = x + width / 2f
        val centerY = y + 130f
        
        // Gold Circle Frame
        paint.color = Color.parseColor("#D4AF37")
        canvas.drawCircle(centerX, centerY, frameSize / 2 + 3f, paint)

        // White inner Circle
        paint.color = Color.parseColor("#EAEAEA")
        canvas.drawCircle(centerX, centerY, frameSize / 2, paint)

        // Display photo if it is present, otherwise display monogram
        var photoDrawSuccess = false
        if (ustadz.foto != null) {
            val file = File(ustadz.foto)
            if (file.exists()) {
                val origBitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (origBitmap != null) {
                    val cropped = cropToMatchAspectRatio(origBitmap, 100, 100)
                    canvas.save()
                    val circlePath = Path().apply {
                        addCircle(centerX, centerY, frameSize / 2, Path.Direction.CCW)
                    }
                    canvas.clipPath(circlePath)
                    val photoRect = RectF(centerX - frameSize / 2, centerY - frameSize / 2, centerX + frameSize / 2, centerY + frameSize / 2)
                    canvas.drawBitmap(cropped, null, photoRect, paint)
                    canvas.restore()
                    photoDrawSuccess = true
                }
            }
        }

        if (!photoDrawSuccess) {
            // Text letter monogram inside circle for staff placeholder
            paint.color = Color.parseColor("#0E5C3A")
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val initial = ustadz.nama.take(1).uppercase()
            val mWidth = paint.measureText(initial)
            canvas.drawText(initial, centerX - mWidth / 2, centerY + 10f, paint)
        }

        // Name text
        paint.color = Color.parseColor("#D4AF37")
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textWidth = paint.measureText(ustadz.nama.uppercase())
        canvas.drawText(ustadz.nama.uppercase(), x + (width - textWidth) / 2, (y + 200).toFloat(), paint)

        // Jabatan
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textWidth = paint.measureText(ustadz.jabatan)
        canvas.drawText(ustadz.jabatan, x + (width - textWidth) / 2, (y + 218).toFloat(), paint)

        // Gold divider line
        paint.color = Color.parseColor("#D4AF37")
        canvas.drawRect(
            RectF((x + 35).toFloat(), (y + 225).toFloat(), (x + width - 35).toFloat(), (y + 226).toFloat()),
            paint
        )

        // Details Block (Starting at Y = 245)
        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        
        var detY = y + 245f
        val itemStartX = x + 35f

        canvas.drawText("ID STAFF  : ABA-UT-${ustadz.id.toString().padStart(3, '0')}", itemStartX, detY, paint)
        detY += 15f
        canvas.drawText("JABATAN   : ${ustadz.jabatan}", itemStartX, detY, paint)
        detY += 15f
        canvas.drawText("KONTAK    : ${ustadz.hp}", itemStartX, detY, paint)
        detY += 15f
        
        val truncatedAlamat = if (ustadz.alamat.length > 20) ustadz.alamat.take(18) + "..." else ustadz.alamat
        canvas.drawText("ALAMAT    : $truncatedAlamat", itemStartX, detY, paint)

        // Footnote banner decorator
        paint.color = Color.parseColor("#0E5C3A")
        canvas.drawRect(
            RectF((x + 2).toFloat(), (y + height - 60).toFloat(), (x + width - 2).toFloat(), (y + height - 2).toFloat()),
            paint
        )
        // bottom border curve override cover
        paint.color = Color.parseColor("#D4AF37")
        canvas.drawRect(
            RectF((x + 2).toFloat(), (y + height - 60).toFloat(), (x + width - 2).toFloat(), (y + height - 58).toFloat()),
            paint
        )

        // Tiny QR code left bottom inside footnote
        val qrSize = 42
        val qrLeft = x + 18
        val qrTop = y + height - 50
        paint.color = Color.WHITE
        canvas.drawRoundRect(
            RectF(qrLeft.toFloat(), qrTop.toFloat(), (qrLeft + qrSize).toFloat(), (qrTop + qrSize).toFloat()),
            4f, 4f, paint
        )

        val qrData = "https://tpqaba.id/ustadz/${ustadz.id}"
        val qrBitmap = QRCodeGenerator.generateQRCode(qrData, qrSize)
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, qrLeft.toFloat(), qrTop.toFloat(), paint)
        }

        // Motto or contact text on footnote
        paint.color = Color.WHITE
        paint.textSize = 7f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Mencetak Generas Qur'ani", (x + 72).toFloat(), (y + height - 35).toFloat(), paint)
        paint.color = Color.parseColor("#D4AF37")
        paint.textSize = 6f
        canvas.drawText("DAN BERAKHLAK MULIA", (x + 72).toFloat(), (y + height - 25).toFloat(), paint)
        
        // Recurve border stroke
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#D4AF37")
        paint.strokeWidth = 3f
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            18f, 18f, paint
        )
        paint.style = Paint.Style.FILL
    }

    private fun drawTeacherBackCard(
        canvas: Canvas,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        ustadz: com.elshadiqan.tpqaba.data.model.Ustadz,
        paint: Paint,
        appConfig: AppConfig
    ) {
        // Deep Green Emerald Background
        paint.color = Color.parseColor("#05351E")
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            18f, 18f, paint
        )

        // Card border
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#D4AF37")
        paint.strokeWidth = 3f
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            18f, 18f, paint
        )
        paint.style = Paint.Style.FILL

        // Top heading
        paint.color = Color.parseColor("#0E5C3A")
        canvas.drawRoundRect(
            RectF((x + 2).toFloat(), (y + 2).toFloat(), (x + width - 2).toFloat(), (y + 45).toFloat()),
            12f, 12f, paint
        )
        paint.color = Color.parseColor("#D4AF37")
        canvas.drawRect(
            RectF((x + 2).toFloat(), (y + 43).toFloat(), (x + width - 2).toFloat(), (y + 45).toFloat()),
            paint
        )

        // Header Title Text
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        var tW = paint.measureText("TATA TERTIB & KETENTUAN")
        canvas.drawText("TATA TERTIB & KETENTUAN", x + (width - tW) / 2, (y + 25).toFloat(), paint)

        // Guidelines body text
        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        
        var instructY = y + 75f
        val leftX = x + 20f

        val rules = listOf(
            "1. Kartu ini milik asatidzah pengajar resmi " + appConfig.namaTpq + ".",
            "2. Harap selalu mengenakan kartu identitas resmi selama jam pendaftaran & KBM berlangsung.",
            "3. Scan QR Code di sisi depan kartu untuk memvalidasi presensi mengajar harian asatidzah.",
            "4. Penyalahgunaan kartu identitas resmi ini merupakan pelanggaran berat tata tertib TPQ.",
            "5. Jika terjadi kehilangan, silahkan lapor ke bagian Tata Usaha / Pimpinan TPQ."
        )

        for (rule in rules) {
            // Manual word wrapping for nice display
            val lines = splitWordsToFit(rule, width - 40, paint)
            for (line in lines) {
                canvas.drawText(line, leftX, instructY, paint)
                instructY += 13f
            }
            instructY += 4f
        }

        // Pimpinan seal signature sign decorators
        instructY = y + height - 55f
        paint.color = Color.parseColor("#D4AF37")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Surakarta, Juni 2026", x + width - 120f, instructY, paint)
        instructY += 12f
        canvas.drawText("Kepala " + appConfig.namaTpq, x + width - 130f, instructY, paint)
        instructY += 28f
        paint.color = Color.WHITE
        canvas.drawText(appConfig.kepalaTpq, x + width - 125f, instructY, paint)

        // Recurve border stroke
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#D4AF37")
        paint.strokeWidth = 3f
        canvas.drawRoundRect(
            RectF(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat()),
            18f, 18f, paint
        )
        paint.style = Paint.Style.FILL
    }

    private fun splitWordsToFit(text: String, maxWidth: Int, paint: Paint): List<String> {
        val words = text.split(" ")
        val finalLines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    finalLines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            finalLines.add(currentLine)
        }
        return finalLines
    }

    fun exportLaporanSantriToPDF(
        context: Context,
        title: String,
        santriList: List<Santri>,
        kelasMap: Map<Int, String>,
        appConfig: AppConfig
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val file = File(context.cacheDir, "Laporan_Siswa_TPQ_Abubakar.pdf")
            if (file.exists()) file.delete()

            // Standard Portrait Page: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            var currentY = 50f

            // Document Header
            paint.color = Color.parseColor("#0E5C3A")
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(appConfig.namaTpq, 40f, currentY, paint)
            currentY += 20f

            paint.color = Color.parseColor("#D4AF37")
            paint.textSize = 12f
            canvas.drawText(title, 40f, currentY, paint)
            currentY += 25f

            paint.color = Color.GRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawLine(40f, currentY, 555f, currentY, paint)
            currentY += 25f
            paint.style = Paint.Style.FILL

            // Table Columns positions:
            // 1. No (40 - 70)
            // 2. NIS (70 - 140)
            // 3. Nama (140 - 320)
            // 4. J.Kelamin (320 - 390)
            // 5. Kelas (390 - 480)
            // 6. Status (480 - 555)

            // Table Header Background
            paint.color = Color.parseColor("#0E5C3A")
            canvas.drawRect(40f, currentY - 15, 555f, currentY + 10, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("No", 45f, currentY, paint)
            canvas.drawText("NIS", 75f, currentY, paint)
            canvas.drawText("Nama Santri", 145f, currentY, paint)
            canvas.drawText("JK", 325f, currentY, paint)
            canvas.drawText("Kelas", 395f, currentY, paint)
            canvas.drawText("Status", 485f, currentY, paint)

            currentY += 25f

            // Rows
            paint.color = Color.parseColor("#1F2937")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            for (idx in santriList.indices) {
                if (currentY > 800) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    // Redraw sub headers
                    currentY = 50f
                    paint.color = Color.parseColor("#0E5C3A")
                    canvas.drawRect(40f, currentY - 15, 555f, currentY + 10, paint)
                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("No", 45f, currentY, paint)
                    canvas.drawText("NIS", 75f, currentY, paint)
                    canvas.drawText("Nama Santri", 145f, currentY, paint)
                    canvas.drawText("JK", 325f, currentY, paint)
                    canvas.drawText("Kelas", 395f, currentY, paint)
                    canvas.drawText("Status", 485f, currentY, paint)
                    currentY += 25f
                }

                val santri = santriList[idx]

                // Alternating row background for beautiful styling
                if (idx % 2 == 1) {
                    paint.color = Color.parseColor("#F4F6F0")
                    canvas.drawRect(40f, currentY - 15, 555f, currentY + 8, paint)
                }

                paint.color = Color.parseColor("#1F2937")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText((idx + 1).toString(), 45f, currentY, paint)
                canvas.drawText(santri.nis, 75f, currentY, paint)
                canvas.drawText(santri.nama, 145f, currentY, paint)
                canvas.drawText(if (santri.jenisKelamin == "Laki-laki") "L" else "P", 325f, currentY, paint)

                val kelasName = kelasMap[santri.kelasId] ?: "Belum Ada"
                canvas.drawText(kelasName, 395f, currentY, paint)
                canvas.drawText(santri.status, 485f, currentY, paint)

                currentY += 20f
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportAbsensiToPDF(
        context: Context,
        title: String,
        absensiList: List<Absensi>,
        appConfig: AppConfig
    ): File? {
        try {
            val pdfDocument = PdfDocument()
            val file = File(context.cacheDir, "Rekap_Absensi_${System.currentTimeMillis()}.pdf")
            if (file.exists()) file.delete()

            // Standard Portrait Page: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            var currentY = 50f

            // Document Header
            paint.color = Color.parseColor("#0E5C3A")
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(appConfig.namaTpq, 40f, currentY, paint)
            currentY += 20f

            paint.color = Color.parseColor("#D4AF37")
            paint.textSize = 12f
            canvas.drawText(title, 40f, currentY, paint)
            currentY += 25f

            paint.color = Color.GRAY
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawLine(40f, currentY, 555f, currentY, paint)
            currentY += 25f
            paint.style = Paint.Style.FILL

            // Table Columns positions:
            // 1. No (40 - 70)
            // 2. Role (70 - 150)
            // 3. Nama (150 - 325)
            // 4. Detail info (325 - 415)
            // 5. Tanggal & Waktu (415 - 505)
            // 6. Status (505 - 555)

            // Table Header Background
            paint.color = Color.parseColor("#0E5C3A")
            canvas.drawRect(40f, currentY - 15, 555f, currentY + 10, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("No", 45f, currentY, paint)
            canvas.drawText("Role", 75f, currentY, paint)
            canvas.drawText("Nama Lengkap", 145f, currentY, paint)
            canvas.drawText("Detail", 325f, currentY, paint)
            canvas.drawText("Waktu", 415f, currentY, paint)
            canvas.drawText("Status", 505f, currentY, paint)

            currentY += 25f

            // Rows
            paint.color = Color.parseColor("#1F2937")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            for (idx in absensiList.indices) {
                if (currentY > 800) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    // Redraw sub headers
                    currentY = 50f
                    paint.color = Color.parseColor("#0E5C3A")
                    canvas.drawRect(40f, currentY - 15, 555f, currentY + 10, paint)
                    paint.color = Color.WHITE
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("No", 45f, currentY, paint)
                    canvas.drawText("Role", 75f, currentY, paint)
                    canvas.drawText("Nama Lengkap", 145f, currentY, paint)
                    canvas.drawText("Detail", 325f, currentY, paint)
                    canvas.drawText("Waktu", 415f, currentY, paint)
                    canvas.drawText("Status", 505f, currentY, paint)
                    currentY += 25f
                }

                val abs = absensiList[idx]

                // Alternating row background
                if (idx % 2 == 1) {
                    paint.color = Color.parseColor("#F4F6F0")
                    canvas.drawRect(40f, currentY - 15, 555f, currentY + 8, paint)
                }

                paint.color = Color.parseColor("#1F2937")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText((idx + 1).toString(), 45f, currentY, paint)
                canvas.drawText(abs.role, 75f, currentY, paint)
                canvas.drawText(abs.nama, 145f, currentY, paint)
                canvas.drawText(abs.detail, 325f, currentY, paint)
                canvas.drawText("${abs.tanggal} ${abs.waktu}", 415f, currentY, paint)
                
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.color = if (abs.status == "Hadir") Color.parseColor("#0E5C3A") else Color.parseColor("#FA5252")
                canvas.drawText(abs.status, 505f, currentY, paint)

                currentY += 20f
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}

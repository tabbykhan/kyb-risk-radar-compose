package com.nationwide.kyb.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utility for handling PDF operations
 * - Copy PDF from assets to cache/downloads
 * - Open using system PDF viewer intent
 * - Extensible for future API-based PDF download
 */
object PdfUtils {
    private const val PDF_ASSETS_PATH = "abc.pdf"
    private const val PDF_FILENAME = "abc.pdf"
    
    /**
     * Copy PDF from assets to external downloads directory and open it
     * @param context Application context
     * @return true if successful, false otherwise
     */
    suspend fun copyAndOpenPdfFromAssets(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Try to copy to Downloads directory first
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val pdfFile = File(downloadsDir, PDF_FILENAME)
                
                // If Downloads is not available, use app cache
                val targetFile = if (downloadsDir.exists() && downloadsDir.canWrite()) {
                    pdfFile
                } else {
                    File(context.cacheDir, PDF_FILENAME)
                }
                
                // Copy from assets
                context.assets.open(PDF_ASSETS_PATH).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Open with system PDF viewer
                withContext(Dispatchers.Main) {
                    openPdfWithViewer(context, targetFile)
                }
                
                true
            } catch (e: IOException) {
                Logger.logError(
                    eventName = "PDF_COPY_FAILED",
                    error = e,
                    additionalData = mapOf("pdfPath" to PDF_ASSETS_PATH)
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to open PDF: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                false
            }
        }
    }
    
    /**
     * Open PDF file using system PDF viewer intent
     * @param context Application context
     * @param pdfFile File to open
     */
    private fun openPdfWithViewer(context: Context, pdfFile: File) {
        try {
            val uri = Uri.fromFile(pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Logger.logEvent(
                    eventName = "PDF_OPENED",
                    additionalData = mapOf("pdfPath" to pdfFile.absolutePath)
                )
            } else {
                Toast.makeText(
                    context,
                    "No PDF viewer app found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Logger.logError(
                eventName = "PDF_VIEWER_OPEN_FAILED",
                error = e
            )
            Toast.makeText(
                context,
                "Failed to open PDF viewer: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Future: Download PDF from API and open
     * This method is extensible for API-based PDF download
     */
    suspend fun downloadAndOpenPdfFromApi(
        context: Context,
        pdfUrl: String,
        correlationId: String? = null
    ): Boolean {

        Logger.logEvent(
            eventName = "PDF_API_DOWNLOAD_REQUESTED",
            correlationId = correlationId,
            additionalData = mapOf("pdfUrl" to pdfUrl)
        )
        return false
    }
}

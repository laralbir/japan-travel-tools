package com.laralbir.japantraveltools.feature.planner.pdf.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.laralbir.japantraveltools.R
import com.laralbir.japantraveltools.feature.planner.pdf.model.PdfExportResult

/**
 * Helper to obtain the secure content URI of the exported PDF and launch the Android Sharesheet.
 */
object PdfShareHelper {

    /**
     * Resolves the secure FileProvider URI and opens the system share chooser.
     */
    fun sharePdf(context: Context, pdfResult: PdfExportResult) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfResult.file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, pdfResult.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserTitle = context.getString(R.string.share_pdf_title)
        val chooserIntent = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}

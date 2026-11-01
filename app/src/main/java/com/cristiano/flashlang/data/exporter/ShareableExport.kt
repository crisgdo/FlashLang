package com.cristiano.flashlang.data.exporter

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Writes the current export under cache/exports and returns the URI produced by [uriFactory]. */
internal fun <T> writeTemporaryExportAndGetUri(
    cacheDir: File,
    fileName: String,
    content: String,
    uriFactory: (File) -> T,
): T {
    require(fileName.isNotBlank() && File(fileName).name == fileName) { "Nome de arquivo inválido." }

    val exportDirectory = File(cacheDir, "exports")
    if (!exportDirectory.exists() && !exportDirectory.mkdirs()) {
        error("Não foi possível criar a pasta temporária de exportação.")
    }
    exportDirectory.listFiles()?.forEach { oldFile ->
        if (oldFile.isFile && !oldFile.delete()) {
            error("Não foi possível limpar arquivos temporários antigos.")
        }
    }

    val exportFile = File(exportDirectory, fileName)
    exportFile.writeText(content, Charsets.UTF_8)
    return uriFactory(exportFile)
}

fun writeShareableExport(
    context: Context,
    fileName: String,
    content: String,
): Uri = writeTemporaryExportAndGetUri(context.cacheDir, fileName, content) { file ->
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun createJsonShareChooser(uri: Uri, fileName: String, chooserTitle: String): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newRawUri(fileName, uri)
    }
    return Intent.createChooser(sendIntent, chooserTitle)
}

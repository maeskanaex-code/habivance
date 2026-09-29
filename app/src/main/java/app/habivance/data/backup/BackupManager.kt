package app.habivance.data.backup

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

class BackupManager(private val context: Context) {

    private val repository = BackupRepository(context)

    suspend fun exportToUri(uri: Uri): Int {
        val outputStream: OutputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Could not open output stream")
        return repository.export(outputStream)
    }

    suspend fun importFromUri(uri: Uri): ImportResult {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Could not open input stream")
        return repository.import(inputStream)
    }
}

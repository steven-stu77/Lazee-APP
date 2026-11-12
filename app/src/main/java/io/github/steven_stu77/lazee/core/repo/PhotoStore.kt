package io.github.steven_stu77.lazee.core.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Receipt photos: scaled to a 1600px long edge and kept in app-private storage (files/photos). */
@Singleton
class PhotoStore @Inject constructor(@ApplicationContext private val context: Context) {
    val dir: File get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** A temporary file the camera app can write into. */
    fun newCameraUri(): Uri {
        val f = File(context.cacheDir, "camera-${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.files", f)
    }

    /** Copies + compresses the picked image; returns the stored file name, or null on failure. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longEdge = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longEdge / (sample * 2) >= MAX_EDGE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return@runCatching null
            val rotated = rotate(raw, uri)
            val scale = MAX_EDGE.toFloat() / maxOf(rotated.width, rotated.height)
            val out = if (scale < 1f) Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt(), (rotated.height * scale).toInt(), true) else rotated
            val name = "${UUID.randomUUID()}.jpg"
            File(dir, name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            name
        }.getOrNull()
    }

    private fun rotate(b: Bitmap, uri: Uri): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return b
        }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(degrees) }, true)
    }

    fun file(name: String): File = File(dir, name)
    fun delete(name: String?) { if (name != null) File(dir, name).delete() }

    companion object { const val MAX_EDGE = 1600 }
}

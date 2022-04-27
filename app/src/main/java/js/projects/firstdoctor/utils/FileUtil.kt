package js.projects.firstdoctor.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import js.projects.firstdoctor.BuildConfig
import java.io.File
import java.util.*


class FileUtil {
    fun openFile(context: Context, filepath: String): Boolean {
        val uri: Uri = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Uri.fromFile(File(filepath))
        } else {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, File(filepath))
        }
        return openUri(context, uri)
    }

    private fun openUri(context: Context, uri: Uri): Boolean {
        return openUri(context, getOpenIntent(uri, context.contentResolver.getType(uri)))
    }

    private fun openUri(context: Context, intent: Intent): Boolean {
        try {
            context.startActivity(intent)
            return true
        } catch (e: Throwable) {
            Log.d(
                "open file", java.lang.String.format(
                    Locale.US,
                    "Open uri request failed with error message '%s'",
                    e.message
                )
            )
        }
        return false
    }


    private fun getOpenIntent(url: Uri?, type: String?): Intent {
        return Intent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .setDataAndType(url, type)
    }
}
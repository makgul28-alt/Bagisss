package com.example.bagis

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import org.json.JSONObject

/**
 * Uygulamanın tüm mantığı assets/index.html içindedir.
 * Bu sınıf yalnızca iki köprü sağlar:
 *  - BagisBackup.saveToDownloads : yedeği İndirilenler klasörüne yazar (sayfa yenilenmez)
 *  - BagisFile.pick             : geri yükleme için dosya seçer
 */
class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode != RESULT_OK || uri == null) {
            notifyPickerFailed()
            return@registerForActivityResult
        }
        readBackup(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        ViewCompat.setOnApplyWindowInsetsListener(web) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        web.addJavascriptInterface(BackupBridge(), "BagisBackup")
        web.addJavascriptInterface(FileBridge(), "BagisFile")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return loader.shouldInterceptRequest(request.url)
            }
        }
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private inner class BackupBridge {
        /** Yedeği doğrudan İndirilenler klasörüne yazar; sonucu döner. Sayfa yenilenmez. */
        @JavascriptInterface
        fun saveToDownloads(fileName: String, contents: String): Boolean {
            return try {
                val name = safeName(fileName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri == null) {
                        false
                    } else {
                        val ok = contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(contents.toByteArray(Charsets.UTF_8))
                            true
                        } ?: false
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, if (ok) 0 else 1)
                        contentResolver.update(uri, values, null, null)
                        if (!ok) contentResolver.delete(uri, null, null)
                        ok
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!dir.exists()) dir.mkdirs()
                    java.io.FileOutputStream(java.io.File(dir, name)).use {
                        it.write(contents.toByteArray(Charsets.UTF_8))
                    }
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private inner class FileBridge {
        @JavascriptInterface
        fun pick() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        arrayOf("application/json", "text/plain", "text/*", "application/octet-stream")
                    )
                }
                try {
                    filePicker.launch(intent)
                } catch (e: Exception) {
                    toast("Dosya seçici açılamadı: ${e.localizedMessage ?: "bilinmeyen hata"}")
                    notifyPickerFailed()
                }
            }
        }
    }

    private fun notifyPickerFailed() {
        runCatching { web.evaluateJavascript("window.bagisPickerFailed&&window.bagisPickerFailed();", null) }
    }

    private fun safeName(fileName: String): String {
        val cleaned = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            .ifBlank { "bagis-yedek.json" }
        return if (cleaned.contains('.')) cleaned else "$cleaned.json"
    }

    private fun readBackup(uri: Uri) {
        try {
            val contents = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (contents.isNullOrBlank()) {
                toast("Seçilen dosya boş")
                notifyPickerFailed()
                return
            }
            web.evaluateJavascript("window.bagisRestore(${JSONObject.quote(contents)});", null)
        } catch (e: Exception) {
            toast("Yedek okunamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}")
            notifyPickerFailed()
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

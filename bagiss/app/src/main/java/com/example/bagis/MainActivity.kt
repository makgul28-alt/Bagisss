package com.example.bagis

import android.content.ContentValues
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        setContentView(web)

        ViewCompat.setOnApplyWindowInsetsListener(web) { v, insets ->
            val b = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
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

        // Sayfadan çağrılan köprü: yedeği İndirilenler klasörüne yazar.
        web.addJavascriptInterface(Bridge(), "AndroidBackup")

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView, request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)
        }
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    inner class Bridge {
        /** Yedeği İndirilenler klasörüne kaydeder. Başarılıysa true döner. */
        @JavascriptInterface
        fun saveToDownloads(fileName: String, text: String): Boolean {
            return try {
                val safe = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safe)
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return false
                    resolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } else {
                    @Suppress("DEPRECATION")
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!dir.exists()) dir.mkdirs()
                    FileOutputStream(File(dir, safe)).use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    true
                }
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun toast(msg: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show() }
        }
    }
}

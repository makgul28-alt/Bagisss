package com.example.bagis

import android.app.Activity
import android.content.ContentValues
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

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var pendingExport: String? = null

    private val documentPicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            pendingExport = null
            return@registerForActivityResult
        }

        val export = pendingExport
        pendingExport = null
        if (export != null) {
            writeDocument(uri, export)
        } else {
            readBackup(uri)
        }
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
        web.addJavascriptInterface(BackupBridge(), "BagisBackup")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return loader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                view.evaluateJavascript(BACKUP_UI_SCRIPT, null)
            }
        }
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    private inner class BackupBridge {
        @JavascriptInterface
        fun saveBackup(fileName: String, contents: String) {
            runOnUiThread { exportBackup(fileName, contents) }
        }

        /** Yedeği doğrudan İndirilenler klasörüne yazar (web arayüzü bunu çağırır). */
        @JavascriptInterface
        fun saveToDownloads(fileName: String, contents: String): Boolean {
            val safeName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
                .ifBlank { "Bagis_Yedegi.txt" }
                .let { if (it.endsWith(".txt", ignoreCase = true)) it else "$it.txt" }
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return false
                    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(contents) }
                        ?: return false
                    true
                } else {
                    @Suppress("DEPRECATION")
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!dir.exists()) dir.mkdirs()
                    java.io.FileOutputStream(java.io.File(dir, safeName)).use { it.write(contents.toByteArray(Charsets.UTF_8)) }
                    true
                }
            } catch (e: Exception) {
                false
            }
        }

        @JavascriptInterface
        fun chooseBackup() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                }
                documentPicker.launch(intent)
            }
        }
    }

    private fun exportBackup(fileName: String, contents: String) {
        val safeName = fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
            .ifBlank { "Bagis_Yedegi.txt" }
            .let { if (it.endsWith(".txt", ignoreCase = true)) it else "$it.txt" }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Dosya oluşturulamadı")
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(contents) }
                    ?: error("Dosya yazılamadı")
                toast("Yedek İndirilenler klasörüne kaydedildi: $safeName")
            } catch (e: Exception) {
                toast("Yedek kaydedilemedi: ${e.localizedMessage ?: "Bilinmeyen hata"}")
            }
        } else {
            pendingExport = contents
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, safeName)
            }
            documentPicker.launch(intent)
        }
    }

    private fun writeDocument(uri: Uri, contents: String) {
        try {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(contents) }
                ?: error("Dosya yazılamadı")
            toast("Yedek dosyası kaydedildi")
        } catch (e: Exception) {
            toast("Yedek kaydedilemedi: ${e.localizedMessage ?: "Bilinmeyen hata"}")
        }
    }

    private fun readBackup(uri: Uri) {
        try {
            val contents = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Dosya okunamadı")
            web.evaluateJavascript("window.restoreBagisBackup(${JSONObject.quote(contents)});", null)
        } catch (e: Exception) {
            toast("Yedek okunamadı: ${e.localizedMessage ?: "Bilinmeyen hata"}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        val BACKUP_UI_SCRIPT = """
            (function () {
              if (window.bagisBackupUiInstalled) return;
              window.bagisBackupUiInstalled = true;

              var tools = document.querySelector('.tools');
              if (tools) {
                var restoreButton = document.createElement('button');
                restoreButton.type = 'button';
                restoreButton.textContent = 'Yedekten geri yükle';
                restoreButton.onclick = function () { window.BagisBackup.chooseBackup(); };
                tools.appendChild(restoreButton);
              }

              var backupButton = document.getElementById('bak');
              if (backupButton) {
                backupButton.onclick = function () {
                  try {
                    var stored = JSON.parse(localStorage.getItem('bagis-listesi-v2') || 'null');
                    if (!stored || !Array.isArray(stored.lists)) throw new Error('Liste verisi bulunamadı.');
                    var list = stored.lists.find(function (item) { return item.id === stored.cur; });
                    if (!list) throw new Error('Seçili liste bulunamadı.');
                    var backup = {
                      format: 'bagis-listesi-backup-v1',
                      list: {
                        name: list.name,
                        items: list.items.map(function (item) {
                          return { name: item.n, amountKurus: item.k };
                        })
                      }
                    };
                    window.BagisBackup.saveBackup(list.name + '.txt', JSON.stringify(backup, null, 2));
                  } catch (error) {
                    alert('Yedek oluşturulamadı: ' + error.message);
                  }
                };
              }

              window.restoreBagisBackup = function (contents) {
                try {
                  var backup = JSON.parse(contents);
                  if (!backup || backup.format !== 'bagis-listesi-backup-v1' ||
                      !backup.list || typeof backup.list.name !== 'string' ||
                      !Array.isArray(backup.list.items)) throw new Error('Dosya geçerli bir Bağışlar yedeği değil.');

                  var restoredItems = backup.list.items.map(function (item) {
                    if (!item || typeof item.name !== 'string' ||
                        !Number.isSafeInteger(item.amountKurus) || item.amountKurus <= 0) {
                      throw new Error('Yedekte geçersiz bir bağış satırı var.');
                    }
                    return { n: item.name.trim(), k: item.amountKurus };
                  });

                  var stored = JSON.parse(localStorage.getItem('bagis-listesi-v2') || 'null');
                  if (!stored || !Array.isArray(stored.lists) || !stored.lists.length) {
                    throw new Error('Uygulamadaki liste verisi bulunamadı.');
                  }
                  var listName = backup.list.name.trim();
                  if (!listName) throw new Error('Yedek liste adı boş.');

                  var existing = stored.lists.find(function (item) {
                    return item.name.toLocaleLowerCase('tr-TR') === listName.toLocaleLowerCase('tr-TR');
                  });
                  var message = existing
                    ? '“' + listName + '” listesindeki mevcut kayıtlar yedekle değiştirilsin mi?'
                    : '“' + listName + '” listesi yedeğe eklenip açılacak. Devam edilsin mi?';
                  if (!confirm(message)) return;

                  if (existing) {
                    existing.items = restoredItems;
                    stored.cur = existing.id;
                  } else {
                    var id = Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
                    stored.lists.push({ id: id, name: listName, items: restoredItems });
                    stored.cur = id;
                  }
                  localStorage.setItem('bagis-listesi-v2', JSON.stringify(stored));
                  location.reload();
                } catch (error) {
                  alert('Yedek geri yüklenemedi: ' + error.message);
                }
              };
            })();
        """.trimIndent()
    }
}

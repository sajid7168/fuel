package com.fueltrack.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    private val chooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val cb = filePathCallback
            filePathCallback = null
            if (cb == null) return@registerForActivityResult
            var out: Array<Uri>? = null
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                out = when {
                    data?.data != null -> arrayOf(data.data!!)
                    cameraImageUri != null -> arrayOf(cameraImageUri!!)
                    else -> null
                }
            }
            cb.onReceiveValue(out)
        }

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        webView.addJavascriptInterface(OcrBridge(), "AndroidOCR")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                // Camera capture intent
                val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraImageUri = try {
                    val photo = File.createTempFile("meter_", ".jpg", externalCacheDir)
                    FileProvider.getUriForFile(
                        this@MainActivity, "$packageName.fileprovider", photo
                    ).also { camIntent.putExtra(MediaStore.EXTRA_OUTPUT, it) }
                } catch (e: Exception) {
                    null
                }

                // Gallery / files intent
                val pickIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }

                val chooser = Intent(Intent.ACTION_CHOOSER).apply {
                    putExtra(Intent.EXTRA_INTENT, pickIntent)
                    putExtra(Intent.EXTRA_TITLE, "Capture or choose photo")
                    if (cameraImageUri != null) {
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camIntent))
                    }
                }
                chooserLauncher.launch(chooser)
                return true
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Back navigation inside WebView")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    /** Bridge exposed to the web app as window.AndroidOCR. Runs on-device ML Kit
     *  text recognition on a captured photo and returns the text to JS. */
    inner class OcrBridge {
        private val recognizer =
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        @JavascriptInterface
        fun recognize(dataUrl: String, tag: String) {
            try {
                val comma = dataUrl.indexOf(',')
                val b64 = if (comma >= 0) dataUrl.substring(comma + 1) else dataUrl
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp == null) { deliver(tag, ""); return }
                val image = InputImage.fromBitmap(bmp, 0)
                recognizer.process(image)
                    .addOnSuccessListener { result -> deliver(tag, result.text) }
                    .addOnFailureListener { deliver(tag, "") }
            } catch (e: Exception) {
                deliver(tag, "")
            }
        }

        private fun deliver(tag: String, text: String) {
            val safeTag = JSONObject.quote(tag)
            val safeText = JSONObject.quote(text)
            webView.post {
                webView.evaluateJavascript(
                    "window.__ocrResult && window.__ocrResult($safeTag, $safeText);",
                    null
                )
            }
        }
    }
}

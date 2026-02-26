package com.robocar.app.scratch

// ═══════════════════════════════════════════════════════════════════════
// BLOCKLY SCREEN — реальний Blockly у WebView
//
// Архітектура:
//   • WebView завантажує /assets/blockly/index.html
//   • AndroidBridge (@JavascriptInterface) приймає виклики з JS:
//       - sendDrivePacket(m1,m2,m3,m4) → BLE
//       - sendText(str) → BLE
//       - connectBluetooth() → MainViewModel.onConnectClicked()
//       - showToast(msg)
//       - onWebViewReady()
//   • Kotlin → JS через evaluateJavascript:
//       - setConnectedFromAndroid(true/false)
//       - setSensorData(p1,p2,p3,p4)
// ═══════════════════════════════════════════════════════════════════════

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.robocar.app.MainViewModel
import com.robocar.app.ble.BleState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BlocklyScreen(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val bleState    by mainViewModel.bleState.collectAsState()
    val sensorData  by mainViewModel.sensorData.collectAsState()

    // Зберігаємо посилання на WebView для evaluateJavascript
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Оновлюємо статус підключення у Blockly
    LaunchedEffect(bleState) {
        val connected = bleState is BleState.Connected
        webView?.post {
            webView?.evaluateJavascript(
                "window.setConnectedFromAndroid && window.setConnectedFromAndroid($connected);",
                null
            )
        }
    }

    // Оновлюємо сенсори у Blockly
    LaunchedEffect(sensorData) {
        val p1 = sensorData.p1; val p2 = sensorData.p2
        val p3 = sensorData.p3; val p4 = sensorData.p4
        webView?.post {
            webView?.evaluateJavascript(
                "window.setSensorData && window.setSensorData($p1, $p2, $p3, $p4);",
                null
            )
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled       = true
                    domStorageEnabled       = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    mixedContentMode        = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort         = true
                    loadWithOverviewMode    = true
                    setSupportZoom(false)
                    builtInZoomControls     = false
                    displayZoomControls     = false
                }

                // JS Bridge — Kotlin методи доступні з JS як Android.xxx()
                addJavascriptInterface(
                    AndroidBridge(mainViewModel, this),
                    "Android"
                )

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        // Встановлюємо початковий стан підключення
                        val connected = mainViewModel.bleState.value is BleState.Connected
                        view.evaluateJavascript(
                            "window.setConnectedFromAndroid && window.setConnectedFromAndroid($connected);",
                            null
                        )
                    }
                }

                // Вмикаємо консоль логи для дебагу
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                        android.util.Log.d("Blockly", "[${msg.messageLevel()}] ${msg.message()}")
                        return true
                    }
                }

                loadUrl("file:///android_asset/blockly/index.html")
                webView = this
            }
        },
        update = { view ->
            webView = view
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ANDROID BRIDGE — JS → Kotlin
// Всі методи викликаються з JS у фоновому потоці!
// ─────────────────────────────────────────────────────────────────────────────
private class AndroidBridge(
    private val vm: MainViewModel,
    private val webView: WebView,
) {

    /** sendDrivePacket(m1, m2, m3, m4) з Blockly */
    @JavascriptInterface
    fun sendDrivePacket(m1: Int, m2: Int, m3: Int, m4: Int) {
        vm.sendMotorPacket(m1, m2, m3, m4)
    }

    /** sendText(str) — текстові команди (OLED, etc.) */
    @JavascriptInterface
    fun sendText(str: String) {
        vm.sendRawText(str)
    }

    /** Кнопка підключення BLE з Blockly UI */
    @JavascriptInterface
    fun connectBluetooth() {
        webView.post { vm.onConnectClicked() }
    }

    /** Toast повідомлення */
    @JavascriptInterface
    fun showToast(message: String) {
        webView.post {
            android.widget.Toast.makeText(webView.context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /** WebView повністю завантажився */
    @JavascriptInterface
    fun onWebViewReady() {
        android.util.Log.d("Blockly", "WebView ready ✓")
    }
}

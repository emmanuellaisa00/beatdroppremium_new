package com.beatdrop.kt.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.AnimatedVisibility
import com.beatdrop.kt.ui.theme.LocalAppColors

/**
 * Built-in web browser — SnapTube-style.
 * Users can browse YouTube, Facebook, Instagram, etc. and download any video.
 * Auto-detects downloadable media on every page.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String? = null,
    onVideoDetected: (String, String) -> Unit,  // (url, title)
    onBack: () -> Unit,
) {
    val C = LocalAppColors.current
    var url by remember { mutableStateOf(initialUrl ?: "https://m.youtube.com") }
    var inputUrl by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("Browser") }
    var detectedDownloadUrl by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        // Top bar with URL input
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = C.text)
            }
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                placeholder = { Text("Enter URL…", color = C.textTertiary) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.accent,
                    unfocusedBorderColor = C.border,
                    focusedContainerColor = C.bg2,
                    unfocusedContainerColor = C.bg2,
                    cursorColor = C.accent,
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { url = inputUrl }),
                trailingIcon = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = C.accent,
                        )
                    }
                },
            )
        }

        // Navigation buttons
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Quick access buttons for popular platforms
            listOf("YouTube" to "https://m.youtube.com").forEach { (label, siteUrl) ->
                OutlinedButton(
                    onClick = { url = siteUrl; inputUrl = siteUrl },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = C.bg2),
                ) {
                    Text(label, color = C.text, fontSize = 11.sp)
                }
            }
        }

        // WebView
        Box(Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            allowFileAccess = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
                                val u = r.url.toString()
                                // Block ads and tracking
                                return u.contains("doubleclick.net") || u.contains("googleads")
                            }

                            override fun onPageStarted(v: WebView, u: String?, favicon: Bitmap?) {
                                isLoading = true
                                u?.let { inputUrl = it; url = it }
                            }

                            override fun onPageFinished(v: WebView, u: String?) {
                                isLoading = false
                                canGoBack = v.canGoBack()
                                canGoForward = v.canGoForward()
                                pageTitle = v.title ?: "Browser"
                                // Inject video detection script
                                v.evaluateJavascript(detectionScript(), null)
                            }

                            override fun shouldInterceptRequest(v: WebView, req: WebResourceRequest): WebResourceResponse? {
                                val u = req.url.toString()
                                // Detect media streams
                                if (u.contains("googlevideo.com/videoplayback")) {
                                    detectedDownloadUrl = u
                                }
                                return null
                            }
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onVideoFound(url: String, title: String) {
                                detectedDownloadUrl = url
                            }
                        }, "BeatDropBridge")

                        loadUrl(url)
                    }
                },
                update = { webView ->
                    val current = webView.url ?: ""
                    if (current != url && url.isNotBlank()) {
                        webView.loadUrl(url)
                    }
                },
            )
        }

        // Download floating button when media detected
        AnimatedVisibility(visible = detectedDownloadUrl != null) {
            FloatingActionButton(
                onClick = {
                    detectedDownloadUrl?.let { u ->
                        onVideoDetected(u, pageTitle)
                        detectedDownloadUrl = null
                    }
                },
                modifier = Modifier.align(Alignment.End).padding(16.dp),
                containerColor = C.accent,
            ) {
                Icon(Icons.Filled.Download, "Download detected video", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

private fun detectionScript(): String = """
    (function() {
        var videos = document.querySelectorAll('video source, video');
        videos.forEach(function(v) {
            if (v.src && window.BeatDropBridge) {
                window.BeatDropBridge.onVideoFound(v.src, document.title || 'Video');
            }
        });
    })();
    true;
""".trimIndent()

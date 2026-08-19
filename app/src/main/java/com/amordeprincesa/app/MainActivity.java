package com.amordeprincesa.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private CancellationSignal biometricCancellation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        webView.addJavascriptInterface(new BiometricBridge(), "AndroidBiometric");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    } catch (ActivityNotFoundException ignored) {}
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg, FileChooserParams fileChooserParams) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = uploadMsg;
                Intent intent = fileChooserParams.createIntent();
                intent.setType("image/*");
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
            }
        });
        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    public class BiometricBridge {
        @JavascriptInterface
        public void authenticate() {
            runOnUiThread(() -> showBiometricPrompt());
        }
    }

    private void showBiometricPrompt() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            sendBiometricError("La huella digital requiere Android 9 o superior en esta versión.");
            return;
        }
        try {
            biometricCancellation = new CancellationSignal();
            BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                    .setTitle("Amor de Princesa")
                    .setSubtitle("Acceso administrador")
                    .setDescription("Usá tu huella digital para ingresar")
                    .setNegativeButton("Cancelar", getMainExecutor(), (dialog, which) -> sendBiometricError("Acceso biométrico cancelado."))
                    .build();
            prompt.authenticate(biometricCancellation, getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    webView.evaluateJavascript("window.onBiometricSuccess && window.onBiometricSuccess();", null);
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    sendBiometricError(errString != null ? errString.toString() : "No se pudo validar la huella.");
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    sendBiometricError("Huella no reconocida. Intentá nuevamente.");
                }
            });
        } catch (Exception e) {
            sendBiometricError("No hay biometría disponible o configurada en este dispositivo.");
        }
    }

    private void sendBiometricError(String text) {
        if (webView == null) return;
        final String safe = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        webView.evaluateJavascript("window.onBiometricError && window.onBiometricError('" + safe + "');", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}

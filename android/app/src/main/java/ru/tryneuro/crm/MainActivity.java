package ru.tryneuro.crm;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import android.content.ComponentName;

public class MainActivity extends AppCompatActivity {
    private static final Uri START_URI = Uri.parse("https://crm.999crm.ru/");
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private boolean fallbackShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String browserPackage = CustomTabsClient.getPackageName(this, null);
        if (browserPackage == null || !bindCustomTabs(browserPackage)) {
            showWebViewFallback();
        }
    }

    private boolean bindCustomTabs(String browserPackage) {
        return CustomTabsClient.bindCustomTabsService(this, browserPackage,
                new CustomTabsServiceConnection() {
                    @Override
                    public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                        CustomTabsSession session = client.newSession(null);
                        if (session == null || !launchTrustedWebActivity(session)) {
                            showWebViewFallback();
                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        // Сессия TWA уже запущена либо будет заменена fallback-режимом.
                    }
                });
    }

    private boolean launchTrustedWebActivity(CustomTabsSession session) {
        try {
            Intent intent = new TrustedWebActivityIntentBuilder(START_URI)
                    .setToolbarColor(getColor(R.color.primary))
                    .build(session)
                    .getIntent();
            if (intent.resolveActivity(getPackageManager()) == null) {
                return false;
            }
            startActivity(intent);
            finish();
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showWebViewFallback() {
        if (fallbackShown || isFinishing()) {
            return;
        }
        fallbackShown = true;
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme()) && "crm.999crm.ru".equals(uri.getHost())) {
                    return false;
                }
                openExternal(uri);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Офлайн-заглушка для модерации RuStore: вместо системной ошибки показываем свой экран.
                if (request.isForMainFrame()) {
                    view.loadUrl("file:///android_asset/offline.html");
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request.isForMainFrame()) {
                    int status = errorResponse.getStatusCode();
                    if (status >= 500) {
                        view.loadUrl("file:///android_asset/offline.html");
                    }
                }
            }

            // Совместимость со старыми WebView (API < 23).
            @Override
            @SuppressWarnings("deprecation")
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                view.loadUrl("file:///android_asset/offline.html");
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                fileCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, 1001);
                } catch (ActivityNotFoundException exception) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                }
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl(START_URI.toString());
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            // Внешнее приложение может отсутствовать на устройстве.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

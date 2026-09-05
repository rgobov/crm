package ru.tryneuro.crm;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import android.content.ComponentName;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final Uri START_URI = Uri.parse("https://crm.999crm.ru/");
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private boolean fallbackShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Без сети TWA покажет чужую страницу Chrome. Сразу идём в свой WebView —
        // там наша заглушка, меню и встроенная политика (требование RuStore).
        if (!isOnline()) {
            showWebViewFallback();
            return;
        }
        String browserPackage = CustomTabsClient.getPackageName(this, null);
        if (browserPackage == null || !bindCustomTabs(browserPackage)) {
            showWebViewFallback();
        }
    }

    // Проверка сети для выбора режима запуска. Точность не критична:
    // captive-портал всё равно уйдёт в TWA и покажет страницу браузера.
    @SuppressWarnings("deprecation")
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true;
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
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
        // File-доступ нужен только для своих встроенных экранов (assets/offline.html, privacy.html).
        // Доступ file:// к сети и между файлами запрещён по умолчанию (setAllow*FromFileURLs=false).
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                // Встроенные экраны (офлайн-заглушка, политика) всегда открываем внутри приложения.
                if ("file".equals(uri.getScheme())) {
                    return false;
                }
                if ("https".equals(uri.getScheme()) && "crm.999crm.ru".equals(uri.getHost())) {
                    return false;
                }
                openExternal(uri);
                return true;
            }

            // Совместимость со старыми WebView (API < 24): новый колбэк там не вызывается.
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if ("file".equals(uri.getScheme())) {
                    return false;
                }
                if ("https".equals(uri.getScheme()) && "crm.999crm.ru".equals(uri.getHost())) {
                    return false;
                }
                openExternal(uri);
                return true;
            }

            // Политика офлайн: ссылки /privacy из футеров, чекбокса и заглушки отдаём
            // встроенным ассетом, а не сетью. Онлайн идёт свежий текст с сайта.
            // Перехват именно https (а не file://-переходы по клику): часть WebView
            // молча отбрасывает навигацию file:// -> file://, а https-переход работает везде.
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                boolean isPrivacy = "https".equals(uri.getScheme())
                        && "crm.999crm.ru".equals(uri.getHost())
                        && ("/privacy".equals(path) || "/privacy/".equals(path));
                if (isPrivacy && !isOnline()) {
                    try {
                        return new WebResourceResponse("text/html", "utf-8", getAssets().open("privacy.html"));
                    } catch (IOException ignored) {
                        // Ассет на месте всегда; при сбое — обычная сетевая ошибка и заглушка.
                    }
                }
                return super.shouldInterceptRequest(view, request);
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
        setContentView(buildFallbackLayout(webView));
        webView.loadUrl(START_URI.toString());
    }

    // Нативная шапка для WebView-режима: без неё меню (⋮) негде показать (тема NoActionBar).
    private android.view.View buildFallbackLayout(WebView webView) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("999 CRM");
        toolbar.setTitleTextColor(0xFFFFFFFF);
        toolbar.setBackgroundColor(getColor(R.color.primary));
        setSupportActionBar(toolbar);
        root.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
            // Внешнее приложение может отсутствовать на устройстве.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Нативное меню: политика встроена в APK и открывается даже офлайн.
        // Видно в WebView-режиме (в TWA меню рисует браузер, политика там — через ссылки в приложении).
        menu.add(Menu.NONE, 1, Menu.NONE, "Политика конфиденциальности");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            if (webView != null) {
                webView.loadUrl("file:///android_asset/privacy.html");
            } else {
                openExternal(Uri.parse("https://crm.999crm.ru/privacy"));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
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

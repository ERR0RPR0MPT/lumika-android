package com.weclont.lumika;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LumikaMainActivity";
    private WebView webView;
    private WVChromeClient wv = null;
    public ValueCallback<Uri[]> uploadFiles = null;
    private String themeColor = "#ffffff";
    private Handler handler;
    private Runnable colorCheckRunnable;
    private String lastOneThemeColor = "#ffffff";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        handler = new Handler();
        colorCheckRunnable = new Runnable() {
            @Override
            public void run() {
                String currentThemeColor = getThemeColorFromMetaTags(webView);
                if (!Objects.equals(currentThemeColor, lastOneThemeColor)) {
                    setStatusBarColor(currentThemeColor);
                    lastOneThemeColor = currentThemeColor;
                }

                // 每隔200ms检查一次
                handler.postDelayed(this, 100);
            }
        };

        String executablePath = getApplicationInfo().nativeLibraryDir;
        Log.e(TAG, "onCreate: " + executablePath);

        initializeApp();
    }

    private void initializeApp() {
        int port = generateRandomNumber();

        startExecutableService(port);
        setWebViewUrl(port);
        startColorCheckThread();
    }

    public static int generateRandomNumber() {
        Random random = new Random();
        int min = 10000;
        int max = 65535;
        return random.nextInt(max - min + 1) + min;
    }

    private void startExecutableService(int port) {
        Intent serviceIntent = new Intent(this, ExecutableService.class);
        serviceIntent.putExtra("port", port);
        startService(serviceIntent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebViewUrl(int port) {
        WebSettings websettings = webView.getSettings();
        websettings.setUseWideViewPort(true);
        websettings.setDomStorageEnabled(true);  // 开启 DOM storage 功能
        websettings.setAllowFileAccess(true);    // 可以读取文件缓存
        websettings.setJavaScriptEnabled(true);

        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Uri uri = request.getUrl();
                if (uri != null && !uri.toString().contains("/api")) {
                    webView.reload();
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                Uri uri = request.getUrl();
                if (uri != null && !uri.toString().contains("/api")) {
                    webView.reload();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();

                if (url != null && url.toString().contains("github")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    view.getContext().startActivity(intent);
                    return true;
                }

                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 获取theme-color的meta标签值
                String themeColor = getThemeColorFromMetaTags(view);
                // 改变状态栏颜色
                if (themeColor != null) {
                    setStatusBarColor(themeColor);
                }
            }
        };
        webView.setWebViewClient(webViewClient);


        wv = new WVChromeClient(this, MainActivity.this);
        webView.setWebChromeClient(wv);

        // 延迟后加载页面
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> loadWebView(port), 320);
    }

    private String getThemeColorFromMetaTags(WebView webView) {
        String script = "javascript:(function() { " +
                "var metaTags = document.getElementsByTagName('meta');" +
                "for (var i = 0; i < metaTags.length; i++) {" +
                "   var metaTag = metaTags[i];" +
                "   if (metaTag.getAttribute('name') === 'theme-color') {" +
                "       return metaTag.getAttribute('content');" +
                "   }" +
                "}" +
                "return null;" +
                "})()";

        webView.evaluateJavascript(script, value -> {
            if (value != null && !value.equals("null")) {
                // 移除双引号
                themeColor = value.replaceAll("\"", "");
            }
        });

        return themeColor;
    }

    private void setStatusBarColor(String color) {
        if (isColorLight(color)) {
            getWindow().setStatusBarColor(Color.parseColor("#ffffff"));
        } else {
            getWindow().setStatusBarColor(Color.parseColor("#212121"));
        }
        setStatusBarTextColor(color);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WVChromeClient.CHOOSER_REQUEST) { // 处理返回的文件
            wv.onActivityResultFileChooser(requestCode, resultCode, data); // 调用 WVChromeClient 类中的 回调方法
        }
    }

    public static boolean isColorLight(String colorString) {
        if (colorString.startsWith("#")) {
            colorString = colorString.substring(1);
        }
        int color = Integer.parseInt(colorString, 16);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        double brightness = (0.299 * red + 0.587 * green + 0.114 * blue) / 255;
        return brightness >= 0.5;
    }

    private void setStatusBarTextColor(String color) {
        WindowInsetsControllerCompat wic = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (wic != null) {
            wic.setAppearanceLightStatusBars(isColorLight(color));
        }
    }

    private void startColorCheckThread() {
        handler.postDelayed(colorCheckRunnable, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(colorCheckRunnable);
    }

    public final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null) {
                                Uri[] uris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), data);
                                uploadFiles.onReceiveValue(uris);
                            } else {
                                uploadFiles.onReceiveValue(null);
                            }
                            uploadFiles = null;
                        }
                    });

    private void loadWebView(int port) {
        webView.loadUrl("http://localhost:" + port + "/ui/");
    }

}
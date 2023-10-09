package com.weclont.lumika;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LumikaMainActivity";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        String executablePath = getApplicationInfo().nativeLibraryDir;
        Log.e(TAG, "onCreate: " + executablePath);

        initializeApp();
    }

    private void initializeApp() {
        int port = generateRandomNumber();

        startExecutableService(port);
        setWebViewUrl(port);
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

    private void setWebViewUrl(int port) {
        WebSettings websettings = webView.getSettings();
        websettings.setDomStorageEnabled(true);  // 开启 DOM storage 功能
        websettings.setAllowFileAccess(true);    // 可以读取文件缓存
        websettings.setJavaScriptEnabled(true);

        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                refreshWebView();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                writeData();
            }

            private void writeData(){
                String key = "Lumika_API";
                String val = "http://localhost:" + port + "/ui/";
                webView.evaluateJavascript("window.localStorage.setItem('"+ key +"','"+ val +"');", null);
            }
        };

        webView.setWebViewClient(webViewClient);

        // 延迟后加载页面
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> loadWebView(port), 500);
    }

    private void refreshWebView() {
        webView.reload();
    }

    private void loadWebView(int port) {
        webView.loadUrl("http://localhost:" + port + "/ui/");
    }

}
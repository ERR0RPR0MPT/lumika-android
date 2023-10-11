package com.weclont.lumika;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Random;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LumikaMainActivity";
    private WebView webView;
    private WVChromeClient wv = null;
    public ValueCallback<Uri[]> uploadFiles = null;
    private String themeColor = "#ffffff";
    private Handler handler;
    private Runnable colorCheckRunnable;
    private String lastOneThemeColor = "#ffffff";
    private LumikaWebServer webServer;
    private int serverPort;
    private int serverBackendPort;

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
        serverPort = 7859;
        serverBackendPort = serverPort + 1;

        startExecutableService(serverBackendPort);
        startServer(serverPort);
        setWebViewUrl(serverPort);
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
        webView.addJavascriptInterface(new LocalStorageJavaScriptInterface(getApplicationContext()), "LocalStorage");
        LocalStorageJavaScriptInterface localStorageInterface = new LocalStorageJavaScriptInterface(MainActivity.this);
        localStorageInterface.setItem("Lumika_API", "http://localhost:" + serverBackendPort + "/ui/");
        WebSettings websettings = webView.getSettings();
        websettings.setUseWideViewPort(true);
        websettings.setDomStorageEnabled(true);  // 开启 DOM storage 功能
        websettings.setAllowFileAccess(true);    // 可以读取文件缓存
        websettings.setJavaScriptEnabled(true);
        websettings.setDatabaseEnabled(true);

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
                LocalStorageJavaScriptInterface localStorageInterface = new LocalStorageJavaScriptInterface(MainActivity.this);
                localStorageInterface.setItem("Lumika_API", "http://localhost:7860/ui/");
            }
        };
        webView.setWebViewClient(webViewClient);

        wv = new WVChromeClient(this, MainActivity.this);
        webView.setWebChromeClient(wv);

        // 延迟后加载页面
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> loadWebView(port), 1);
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
        stopServer();
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

    // 开启NanoHTTPD服务器
    private void startServer(int serverPort) {
        copyAssetsDir2Phone(MainActivity.this, "ui");
        String privateDirPath = getFilesDir().getAbsolutePath();
        webServer = new LumikaWebServer(privateDirPath, serverPort);
        try {
            webServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  从assets目录中复制整个文件夹内容,考贝到 /data/data/包名/files/目录中
     *  @param  activity  activity 使用CopyFiles类的Activity
     *  @param  filePath  String  文件路径,如：/assets/aa
     */
    public static void copyAssetsDir2Phone(Activity activity, String filePath){
        try {
            String[] fileList = activity.getAssets().list(filePath);
            if(fileList.length>0) {//如果是目录
                File file=new File(activity.getFilesDir().getAbsolutePath()+ File.separator+filePath);
                file.mkdirs();//如果文件夹不存在，则递归
                for (String fileName:fileList){
                    filePath=filePath+File.separator+fileName;

                    copyAssetsDir2Phone(activity,filePath);

                    filePath=filePath.substring(0,filePath.lastIndexOf(File.separator));
                    Log.e("oldPath",filePath);
                }
            } else {//如果是文件
                InputStream inputStream=activity.getAssets().open(filePath);
                File file=new File(activity.getFilesDir().getAbsolutePath()+ File.separator+filePath);
                Log.i("copyAssets2Phone","file:"+file);
                if(!file.exists() || file.length()==0) {
                    FileOutputStream fos=new FileOutputStream(file);
                    int len=-1;
                    byte[] buffer=new byte[1024];
                    while ((len=inputStream.read(buffer))!=-1){
                        fos.write(buffer,0,len);
                    }
                    fos.flush();
                    inputStream.close();
                    fos.close();
                    Log.e("", "copyAssetsDir2Phone: 文件复制完毕");
                } else {
                    Log.e("", "copyAssetsDir2Phone: 文件已存在，无需复制");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将文件从assets目录，考贝到 /data/data/包名/files/ 目录中。assets 目录中的文件，会不经压缩打包至APK包中，使用时还应从apk包中导出来
     * @param fileName 文件名,如aaa.txt
     */
    public static void copyAssetsFile2Phone(Activity activity, String fileName){
        try {
            InputStream inputStream = activity.getAssets().open(fileName);
            //getFilesDir() 获得当前APP的安装路径 /data/data/包名/files 目录
            File file = new File(activity.getFilesDir().getAbsolutePath() + File.separator + fileName);
            if(!file.exists() || file.length()==0) {
                FileOutputStream fos =new FileOutputStream(file);//如果文件不存在，FileOutputStream会自动创建文件
                int len=-1;
                byte[] buffer = new byte[1024];
                while ((len=inputStream.read(buffer))!=-1){
                    fos.write(buffer,0,len);
                }
                fos.flush();//刷新缓存区
                inputStream.close();
                fos.close();
                Log.e("", "copyAssetsDir2Phone: 文件复制完毕");
            } else {
                Log.e("", "copyAssetsDir2Phone: 文件已存在，无需复制");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 停止服务器
    private void stopServer() {
        if (webServer != null) {
            webServer.stop();
        }
    }

    private static class LumikaWebServer extends NanoHTTPD {

        private final String rootDir;

        public LumikaWebServer(String rootDir, int port) {
            super(port); // 传入0表示随机分配端口
            this.rootDir = rootDir;
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            String filePath;

            // 处理以"/"结尾的URI，添加index.html到末尾
            if (uri.endsWith("/")) {
                filePath = rootDir + uri + "index.html";
            } else {
                filePath = rootDir + uri;
            }

            Log.e("", "serve: 当前准备访问：" + filePath);

            File file = new File(filePath);

            if (file.exists() && file.isFile()) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    return newChunkedResponse(Response.Status.OK, getMimeTypeForFile(filePath), fis);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return super.serve(session);
        }
    }

    /**
     * This class is used as a substitution of the local storage in Android webviews
     *
     * @author Diane
     */
    private static class LocalStorageJavaScriptInterface {
        private final Context mContext;
        private final LocalStorage localStorageDBHelper;
        private SQLiteDatabase database;

        LocalStorageJavaScriptInterface(Context c) {
            mContext = c;
            localStorageDBHelper = LocalStorage.getInstance(mContext);
        }

        /**
         * This method allows to get an item for the given key
         * @param key : the key to look for in the local storage
         * @return the item having the given key
         */
        @JavascriptInterface
        public String getItem(String key)
        {
            String value = null;
            if(key != null)
            {
                database = localStorageDBHelper.getReadableDatabase();
                Cursor cursor = database.query(LocalStorage.LOCALSTORAGE_TABLE_NAME,
                        null,
                        LocalStorage.LOCALSTORAGE_ID + " = ?",
                        new String [] {key},null, null, null);
                if(cursor.moveToFirst())
                {
                    value = cursor.getString(1);
                }
                cursor.close();
                database.close();
            }
            return value;
        }

        /**
         * set the value for the given key, or create the set of datas if the key does not exist already.
         * @param key
         * @param value
         */
        @JavascriptInterface
        public void setItem(String key,String value)
        {
            if(key != null && value != null)
            {
                String oldValue = getItem(key);
                database = localStorageDBHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(LocalStorage.LOCALSTORAGE_ID, key);
                values.put(LocalStorage.LOCALSTORAGE_VALUE, value);
                if(oldValue != null)
                {
                    database.update(LocalStorage.LOCALSTORAGE_TABLE_NAME, values, LocalStorage.LOCALSTORAGE_ID + "='" + key + "'", null);
                }
                else
                {
                    database.insert(LocalStorage.LOCALSTORAGE_TABLE_NAME, null, values);
                }
                database.close();
            }
        }

        /**
         * removes the item corresponding to the given key
         * @param key
         */
        @JavascriptInterface
        public void removeItem(String key)
        {
            if(key != null)
            {
                database = localStorageDBHelper.getWritableDatabase();
                database.delete(LocalStorage.LOCALSTORAGE_TABLE_NAME, LocalStorage.LOCALSTORAGE_ID + "='" + key + "'", null);
                database.close();
            }
        }

        /**
         * clears all the local storage.
         */
        @JavascriptInterface
        public void clear()
        {
            database = localStorageDBHelper.getWritableDatabase();
            database.delete(LocalStorage.LOCALSTORAGE_TABLE_NAME, null, null);
            database.close();
        }
    }

}
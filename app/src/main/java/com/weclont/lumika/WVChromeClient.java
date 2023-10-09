package com.weclont.lumika;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class WVChromeClient extends WebChromeClient {
    public final static int CHOOSER_REQUEST = 0x33;
    Context context;
    MainActivity _m;
    public WVChromeClient(Context _context, MainActivity mainActivity)
    {
        context = _context;
        _m = mainActivity;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {
        _m.uploadFiles = filePathCallback;
        Intent i = fileChooserParams.createIntent();
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 设置多选
        _m.fileChooserLauncher.launch(Intent.createChooser(i, "选择文件"));
        return true;
    }

    // 文件选择回调（在 MainActivity.java 的 onActivityResult 中调用此方法）
    public void onActivityResultFileChooser(int requestCode, int resultCode, Intent intent) {
        if (requestCode != CHOOSER_REQUEST || _m.uploadFiles == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null)
                    results = new Uri[]{Uri.parse(dataString)};
            }
        }
        _m.uploadFiles.onReceiveValue(results);
        _m.uploadFiles = null;
    }
}
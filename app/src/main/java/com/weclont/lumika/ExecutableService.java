package com.weclont.lumika;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.webkit.WebSettings;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import fi.iki.elonen.NanoHTTPD;

public class ExecutableService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LumikaExecutableServiceChannel";
    private Thread lumikaCoreThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Lumika", "onCreate: ExecutableService 服务开启");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
        int p = intent.getIntExtra("port", 7860);
        LumikaExecutableTask task = new LumikaExecutableTask(p);
        lumikaCoreThread = new Thread(task);
        lumikaCoreThread.start();
        return START_STICKY;
    }

    public class LumikaExecutableTask implements Runnable {

        private final int port;

        public LumikaExecutableTask(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            startExecutable(port);
        }
    }

    public void startExecutable(int port) {
        String executablePath = getApplicationInfo().nativeLibraryDir;
        String ffmpegPath = executablePath + "/libffmpeg.so";
        String ffprobePath = executablePath + "/libffprobe.so";

        Log.e("Lumika", "startExecutable: 开始运行");
        mobile.Mobile.startWebServer(port, getFilesDir().getAbsolutePath(), ffmpegPath, ffprobePath);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Lumika Executable Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Lumika Executable Service")
                    .setContentText("Running in background")
                    .build();
        }
        return null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
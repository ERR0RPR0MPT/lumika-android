package com.weclont.lumika;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.jaredrummler.ktsh.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExecutableService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LumikaExecutableServiceChannel";
    private Thread t;

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
        t = new Thread(task);
        t.start();
        return START_STICKY;
    }

    public class LumikaExecutableTask implements Runnable {

        private int port;

        public LumikaExecutableTask(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            startExecutable(port);
        }
    }

    public void startExecutable(int port) {
        try {
            String executablePath = getApplicationInfo().nativeLibraryDir;
            String command = String.format("%s/liblumika.so web -p %s -d %s", executablePath, port, getFilesDir().getAbsolutePath());
            Log.e("Lumika", "startExecutable: 开始运行: " + command);

            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // 输出日志到控制台
                Log.e("Lumika", line);
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();

            // 打印命令的退出码
            Log.e("Lumika", "Exit Code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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
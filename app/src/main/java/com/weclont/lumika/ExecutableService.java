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

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.ReturnCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import mobile.Mobile;

public class ExecutableService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LumikaExecutableServiceChannel";
    private Thread lumikaCoreThread;
    private Thread monitorThread;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Lumika", "onCreate: ExecutableService 服务开启");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        int p = 7860;
        try {
            p = intent.getIntExtra("port", 7860);
        } catch (Exception ignored) {
        }
        LumikaExecutableTask task = new LumikaExecutableTask(p);
        lumikaCoreThread = new Thread(task);
        lumikaCoreThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Lumika", "onDestroy: ExecutableService 服务被关闭，准备重启");
        Intent intent = new Intent(getApplicationContext(), ExecutableService.class);
        intent.putExtra("port", 7860);
        startService(intent);
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
        Log.e("Lumika", "startExecutable: 开始运行");
        monitorThread = new Thread(() -> {
            while (true) {
                try {
                    String taskJsonString = Mobile.getInput();
                    if (Objects.equals(taskJsonString, "") || taskJsonString == null) {
                        // 没有检测到输入命令，继续检查
                        Thread.sleep(100);
                        continue;
                    }
                    JSONObject jsonObject = new JSONObject(taskJsonString);
                    String uuid = jsonObject.getString("uuid");
                    String type = jsonObject.getString("type");
                    String command = jsonObject.getString("command");
                    if (!Objects.equals(uuid, "")) {
                        if (Objects.equals(type, "ffmpeg")) {
                            // 检测到输入命令，开始调用 FFmpeg
                            Log.e("Lumika", "run: 检测到输入命令，开始调用 FFmpeg");
                            FFmpegKit.executeAsync(command, session -> {
                                if (!ReturnCode.isSuccess(session.getReturnCode())) {
                                    Log.d("Lumika", "FFmpeg 出现错误");
                                    Mobile.setOutput(uuid, type, session.getOutput());
                                } else {
                                    Log.d("Lumika", "FFmpeg 执行成功");
                                    Mobile.setOutput(uuid, type, "success");
                                }
                            }, log -> {
                            }, statistics -> {
                            });
                        } else if (Objects.equals(type, "ffprobe")) {
                            // 检测到输入命令，开始调用 FFprobe
                            Log.e("Lumika", "run: 检测到输入命令，开始调用 FFprobe");
                            FFprobeKit.executeAsync(command, session -> {
                                if (!ReturnCode.isSuccess(session.getReturnCode())) {
                                    Log.d("Lumika", "FFprobe 出现错误");
                                    Mobile.setOutput(uuid, type, "error");
                                } else {
                                    Log.d("Lumika", "FFprobe 执行成功");
                                    Mobile.setOutput(uuid, type, session.getOutput());
                                }
                            });
                        }
//                        else if (Objects.equals(type, "dlFunc")) {
//                            try {
//                                // 检测到输入命令，开始调用 OkHttp3 下载
//                                Log.e("Lumika", "run: 检测到输入命令，开始调用 OkHttp3");
//                                JSONObject jsonObjectDl = new JSONObject(command);
//                                String url = jsonObjectDl.getString("url");
//                                String filePath = jsonObjectDl.getString("filePath");
//                                String referer = jsonObjectDl.getString("referer");
//                                String origin = jsonObjectDl.getString("origin");
//                                String userAgent = jsonObjectDl.getString("userAgent");
//                                OkHttpClient client = new OkHttpClient.Builder()
//                                        .addInterceptor(chain -> {
//                                            Request originalRequest = chain.request();
//                                            Request.Builder requestBuilder = originalRequest.newBuilder()
//                                                    .header("Connection", "keep-alive")
//                                                    .header("Accept", "*/*")
//                                                    .header("Origin", origin)
//                                                    .header("Pragma", "no-cache")
//                                                    .header("Referer", referer)
//                                                    .header("User-Agent", userAgent);
//                                            Request newRequest = requestBuilder.build();
//                                            return chain.proceed(newRequest);
//                                        })
//                                        .build();
//                                Request request = new Request.Builder()
//                                        .url(url)
//                                        .build();
//
//                                Call call = client.newCall(request);
//                                call.enqueue(new Callback() {
//                                    @Override
//                                    public void onFailure(Call call, IOException e) {
//                                        Log.e("Lumika", "onFailure: 下载失败");
//                                        Mobile.setOutput(uuid, type, e.toString());
//                                    }
//                                    @Override
//                                    public void onResponse(Call call, Response response) {
//                                        if (response.isSuccessful()) {
//                                            try {
//                                                Log.e("Lumika", "onFailure: 开始下载");
//                                                InputStream inputStream = response.body().byteStream();
//                                                FileOutputStream outputStream = new FileOutputStream(filePath);
//                                                byte[] buffer = new byte[4096];
//                                                int bytesRead;
//                                                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                                                    outputStream.write(buffer, 0, bytesRead);
//                                                }
//                                                outputStream.close();
//                                                inputStream.close();
//                                                Mobile.setOutput(uuid, type, "success");
//                                                Log.e("Lumika", "onResponse: 下载成功");
//                                            } catch (Exception e) {
//                                                Log.e("Lumika", "onFailure: 下载失败");
//                                                Mobile.setOutput(uuid, type, e.toString());
//                                            }
//                                        } else {
//                                            Log.e("Lumika", "onResponse: 下载失败");
//                                            Mobile.setOutput(uuid, type, String.valueOf(response.code()));
//                                        }
//                                    }
//                                });
//                            } catch (Exception e) {
//                                Log.e("Lumika", "run: OkHttp3 下载出现错误");
//                                Mobile.setOutput(uuid, type, e.toString());
//                            }
//                        }
                    }
                    Thread.sleep(100);
                } catch (JSONException | InterruptedException ignored) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored1) {
                    }
                }
            }
        });
        monitorThread.start();
        mobile.Mobile.startWebServer(port, getFilesDir().getAbsolutePath());
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
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Lumika 服务通知")
                    .setContentText("正在后台运行中...")
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
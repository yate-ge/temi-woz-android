package com.cdi.temiwoz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.media.MediaRecorder;
import android.graphics.Bitmap;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import android.util.Base64;

public class MainActivity extends AppCompatActivity implements OnRobotReadyListener, SurfaceHolder.Callback {

    private static final String TAG = "MainActivity";
    private Robot robot;
    private RobotApi robotApi;
    Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    TemiWebsocketServer server;
    private WebView webView;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    private static final int REQUEST_PERMISSIONS = 1;
    private String[] permissions = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // 在类定义中添加接口
    public interface PictureCallback {
        void onPictureTaken(String base64Image, String imagePath);
        void onError(String error);
    }

    private View cameraIndicator;
    public boolean isCameraOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Starting application");

        // 检查并请求权限
        checkPermissions();

        // 初始化WebView
        webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("https://www.baidu.com");
        
        // 摄像头相关初始化代码保留但默认不启动
        surfaceView = findViewById(R.id.camera_preview);
        surfaceView.setVisibility(View.GONE); // 默认隐藏摄像头预览
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        robot = Robot.getInstance();
        robotApi = new RobotApi(robot, this);// 传入MainActivity实例
        robotApi.setSurfaceHolder(surfaceHolder);

        cameraIndicator = findViewById(R.id.camera_indicator);
    }

    private void checkPermissions() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            Map<String, Integer> perms = new HashMap<>();
            for (int i = 0; i < permissions.length; i++) {
                perms.put(permissions[i], grantResults[i]);
            }
            if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: All permissions granted");
            } else {
                Log.e(TAG, "onRequestPermissionsResult: Some permissions are not granted");
                showDialogOK("Camera and Storage Permissions are required for this app",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        checkPermissions();
                                        break;
                                    case DialogInterface.BUTTON_NEGATIVE:
                                        // Proceed with logic by disabling the related features or quit the app.
                                        break;
                                }
                            }
                        });
            }
        }
    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Resuming activity");
        robot.addOnRobotReadyListener(this);
        robot.showTopBar();

        // Add WebSocket server
        int port = 8175;
        try {
            server = new TemiWebsocketServer(port);
            Log.d(TAG, "onResume: WebSocket server created on port " + port);
        } catch (UnknownHostException e) {
            Log.e(TAG, "onResume: Failed to create WebSocket server", e);
        }
        server.addActivity(this);
        server.addRobotApi(robotApi);
        server.start();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: Pausing activity");
        robot.removeOnRobotReadyListener(this);
        stopCamera();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Stopping activity");
        robot.removeOnRobotReadyListener(this);
        try {
            server.stop(100);
        } catch (InterruptedException e) {
            Log.e(TAG, "onStop: Failed to stop WebSocket server", e);
        }
        robotApi.stop();
    }

    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            Log.d(TAG, "onRobotReady: Robot is ready");
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                robot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "onRobotReady: Failed to get activity info", e);
                throw new RuntimeException(e);
            }
        } else {
            Log.w(TAG, "onRobotReady: Robot is not ready");
        }
    }

    ////////////////////////////////////////////////////////////////
    // SurfaceHolder.Callback
    ////////////////////////////////////////////////////////////////
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            // 忽略预览没有开始的情况
        }

        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "surfaceChanged: Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    private void startCamera() {
        Log.d(TAG, "startCamera: Starting camera");
        if (camera == null) {
            try {
                camera = Camera.open();
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                setCameraDisplayOrientation();
                isCameraOpen = true;
                cameraIndicator.setVisibility(View.VISIBLE);
                surfaceView.setVisibility(View.GONE);
                Log.d(TAG, "startCamera: Camera started");
            } catch (IOException e) {
                Log.e(TAG, "startCamera: Failed to start camera", e);
                isCameraOpen = false;
                cameraIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void stopCamera() {
        Log.d(TAG, "stopCamera: Stopping camera");
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.release();
                camera = null;
                isCameraOpen = false;
                cameraIndicator.setVisibility(View.GONE);
                Log.d(TAG, "stopCamera: Camera stopped and released");
            } catch (Exception e) {
                Log.e(TAG, "stopCamera: Error stopping camera", e);
            }
        }
    }

    private void setCameraDisplayOrientation() {
        if (camera == null) {
            return;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        Log.d(TAG, "setCameraDisplayOrientation: Camera orientation set to " + result + " degrees");
    }

    // 添加切换显示的方法
    public void showCamera() {
        if (!isCameraOpen) {
            return;
        }
        webView.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
    }

    public void showWebView() {
        surfaceView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        stopCamera();
    }

    // 添加截图功能
    private void takeScreenshot() {
        Log.d(TAG, "takeScreenshot: Taking screenshot");
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);

        // 保存截图到文件
        saveBitmap(bitmap);
    }

    private void saveBitmap(Bitmap bitmap) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(path, "screenshot_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Log.d(TAG, "saveBitmap: Screenshot saved to " + imageFile.getAbsolutePath());
            Toast.makeText(this, "Screenshot saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "saveBitmap: Failed to save screenshot", e);
            Toast.makeText(this, "Failed to save screenshot", Toast.LENGTH_SHORT).show();
        }
    }

    // 添加拍照功能
    private void takePicture() {
        if (camera != null) {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    File pictureFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "photo_" + System.currentTimeMillis() + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(pictureFile)) {
                        fos.write(data);
                        Log.d(TAG, "takePicture: Photo saved to " + pictureFile.getAbsolutePath());
                        Toast.makeText(MainActivity.this, "Photo saved", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Log.e(TAG, "takePicture: Failed to save photo", e);
                        Toast.makeText(MainActivity.this, "Failed to save photo", Toast.LENGTH_SHORT).show();
                    }
                    camera.startPreview(); // Restart preview after taking a picture
                }
            });
        }
    }

    // 添加录像功能
    public void startRecording() {
        if (camera == null) {
            camera = Camera.open();
        }
        camera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        File videoFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "video_" + System.currentTimeMillis() + ".mp4");
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "startRecording: Recording started");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "startRecording: Failed to start recording", e);
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopRecording() {
        if (isRecording) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();
            isRecording = false;
            Log.d(TAG, "stopRecording: Recording stopped");
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }

    // 添加公共方法
    public void takePictureForWebSocket(final PictureCallback callback) {
        if (camera != null) {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        // 保存图片文件
                        File pictureFile = new File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                            "temi_photo_" + System.currentTimeMillis() + ".jpg"
                        );
                        FileOutputStream fos = new FileOutputStream(pictureFile);
                        fos.write(data);
                        fos.close();

                        // 转换为 Base64
                        String base64Image = Base64.encodeToString(data, Base64.DEFAULT);
                        
                        // 回调成功
                        callback.onPictureTaken(base64Image, pictureFile.getAbsolutePath());
                        
                        // 重启预览
                        camera.startPreview();
                    } catch (Exception e) {
                        callback.onError("Failed to process image: " + e.getMessage());
                    }
                }
            });
        } else {
            callback.onError("Camera is not available");
        }
    }

    // 修改隐藏摄像头画面的方法
    public void hideCamera() {
        surfaceView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }
}

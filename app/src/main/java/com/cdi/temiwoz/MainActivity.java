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
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnRobotReadyListener, SurfaceHolder.Callback {

    private static final String TAG = "MainActivity";
    private Robot robot;
    private RobotApi robotApi;
    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    TemiWebsocketServer server;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Starting application");

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Camera permission granted");
                initializeCamera();
            } else {
                Log.e(TAG, "onRequestPermissionsResult: Camera permission denied");
                Toast.makeText(this, "Camera permission is required for this app", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeCamera() {
        Log.d(TAG, "initializeCamera: Initializing camera");
        surfaceView = findViewById(R.id.camera_preview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
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
        Log.d(TAG, "surfaceCreated: Surface created");
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: Surface changed. Width: " + width + ", Height: " + height);
        if (surfaceHolder.getSurface() == null) {
            Log.e(TAG, "surfaceChanged: No valid surface");
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, "surfaceChanged: Error stopping preview", e);
        }

        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size previewSize = getBestPreviewSize(width, height, parameters);
            if (previewSize != null) {
                parameters.setPreviewSize(previewSize.width, previewSize.height);
                Log.d(TAG, "surfaceChanged: Setting preview size to " + previewSize.width + "x" + previewSize.height);
            }
            
            // 设置摄像头方向
            setCameraDisplayOrientation();
            
            camera.setParameters(parameters);//设置摄像头参数   
            camera.setPreviewDisplay(surfaceHolder);//设置预览显示
            camera.startPreview();//开始预览
            Log.d(TAG, "surfaceChanged: Camera preview started");
        } catch (Exception e) {
            Log.e(TAG, "surfaceChanged: Error starting camera preview", e);
        }
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();//获取支持的预览尺寸
        bestSize = sizeList.get(0);
        for(int i = 1; i < sizeList.size(); i++){
            if((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)){
                bestSize = sizeList.get(i);
            }
        }
        Log.d(TAG, "getBestPreviewSize: Best size: " + bestSize.width + "x" + bestSize.height);
        return bestSize;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: Surface destroyed");
        stopCamera();
    }

    private void startCamera() {
        Log.d(TAG, "startCamera: Starting camera");
        if (camera == null) {
            try {
                camera = Camera.open();
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                Log.d(TAG, "startCamera: Camera preview started");
            } catch (IOException e) {
                Log.e(TAG, "startCamera: Failed to start camera", e);
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
        webView.setVisibility(View.GONE);
        surfaceView.setVisibility(View.VISIBLE);
        initializeCamera();
    }

    public void showWebView() {
        surfaceView.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        stopCamera();
    }
}

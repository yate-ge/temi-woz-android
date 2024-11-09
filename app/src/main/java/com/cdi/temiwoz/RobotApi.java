package com.cdi.temiwoz;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.util.Log;
import java.io.IOException;

import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.TtsRequest.Status;


import com.robotemi.sdk.constants.*;
import com.robotemi.sdk.listeners.OnMovementStatusChangedListener;
import com.robotemi.sdk.permission.Permission;
import com.robotemi.sdk.telepresence.*;
import com.robotemi.sdk.Robot.TtsListener;
import com.robotemi.sdk.Robot.AsrListener;
import com.robotemi.sdk.listeners.OnBeWithMeStatusChangedListener;
import com.robotemi.sdk.listeners.OnConstraintBeWithStatusChangedListener;
import com.robotemi.sdk.UserInfo;
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener;
import com.robotemi.sdk.listeners.OnDetectionStateChangedListener;



import org.jetbrains.annotations.NotNull;
import org.json.*;

import java.util.ArrayList;
import java.util.List;

import android.webkit.WebView;
import android.webkit.WebSettings;

public class RobotApi implements TtsListener,
                                 AsrListener,
                                 OnGoToLocationStatusChangedListener,
        OnBeWithMeStatusChangedListener,
        OnConstraintBeWithStatusChangedListener,
        OnDetectionStateChangedListener,
        OnMovementStatusChangedListener
{

    private Robot robot;

    public TemiWebsocketServer server;

    String speak_id;
    String ask_id;
    String goto_id;
    String tilt_id;
    String turn_id;
    String getContact_id;
    String call_id;

    private static final String TAG = "RobotApi";
    private Camera camera;
    private SurfaceHolder surfaceHolder;

    private MainActivity activity;

    private Camera mCamera;

    private boolean isMoving = false;
    private float currentX = 0;
    private float currentY = 0;
    private static final int MOVEMENT_INTERVAL = 500; // 500ms between commands
    private Thread movementThread;

    RobotApi (Robot robotInstance, MainActivity activity) {
        System.out.println("RobotApi: Initializing with robot instance: " + (robotInstance != null ? "valid" : "null"));
        this.robot = robotInstance;
        this.activity = activity;
        robot.addTtsListener(this);
        robot.addAsrListener(this);
        robot.addOnGoToLocationStatusChangedListener(this);
        robot.addOnBeWithMeStatusChangedListener(this);
        robot.addOnConstraintBeWithStatusChangedListener(this);
        robot.addOnMovementStatusChangedListener(this);
        robot.addOnDetectionStateChangedListener(this);
        // robot.toggleNavigationBillboard(false);

    }



    // location related
    public void gotoLocation(String location, String id) {
        robot.goTo(location);
        goto_id = id;
    }
    public void saveLocation(String location, String id){
        boolean finished = robot.saveLocation(location);

        try {
            server.broadcast(new JSONObject().put("saveLocation",finished).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void deleteLocation(String location, String id){
        boolean finished = robot.saveLocation(location);

        try {
            server.broadcast(new JSONObject()
                .put("command", "deleteLocation")
                .put("id", id)
                .put("status", "completed")
                .put("location", location)
                .toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //movement..
    public void stopMovement(String id){
        robot.stopMovement();
    }

    public void beWithMe(String id){
        robot.beWithMe();
    }

    public void constraintBeWith(String id){
        robot.constraintBeWith();
    }


    public void tiltAngle(int angle, String id) {
        robot.tiltAngle(angle);
        tilt_id = id;
        try {
            server.broadcast(new JSONObject().put("id", tilt_id).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void turnBy(int angle, String id) {
        robot.turnBy(angle, 1);
        turn_id = id;
//        try {
//            server.broadcast(new JSONObject().put("id", turn_id).toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        // 在movement回调中发送消息
    }

    // call someone
    public void getContact(String id){
        List<UserInfo> list = new ArrayList<>();
        list = robot.getAllContact();

        getContact_id = id;

        try{
            server.broadcast(new JSONObject().put("id", getContact_id).put("userinfo",list).toString());
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void startCall(String userId, String id){
        call_id = id;
        robot.startTelepresence("孙老师", userId);


    }

    // user interaction
    public void wakeup(String id){
        robot.wakeup();
    }
    public void speak(String sentence, String id) {
        robot.speak(TtsRequest.create(sentence, false));
        speak_id = id;
    }

    public void askQuestion(String sentence, String id) {
        robot.askQuestion(sentence);
        ask_id = id;
    }

    public void setDetectionMode(boolean on, String id){
        robot.setDetectionModeOn(on);
        System.out.println("set detection mode on");
        try {
            server.broadcast(new JSONObject()
                .put("command", "setDetectionMode")
                .put("id", id)
                .put("status", "completed")
                .put("isOn", on)
                .toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void checkDetectionMode(String id){
        boolean isDetectionModeOn = robot.isDetectionModeOn();
        try {
            server.broadcast(new JSONObject()
                .put("command", "checkDetectionMode")
                .put("id", id)
                .put("isOn", isDetectionModeOn)
                .toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setTrackUserOn(boolean on, String id){
        int ispermise = robot.checkSelfPermission(Permission.SETTINGS);
        System.out.println(ispermise);
        robot.setTrackUserOn(on);
        System.out.println("set track user mode on");
        try {
            server.broadcast(new JSONObject()
                .put("command", "setTrackUserOn")
                .put("id", id)
                .put("status", "success")
                .toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void beWithMe(){
        robot.beWithMe();
    }

    public void constraintBeWith(){
        robot.constraintBeWith();
    }


    @Override
    public void onDetectionStateChanged(int state){
        try {
            server.broadcast(new JSONObject().put("event", "onDetectionStateChanged").put("state",state).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    @Override
    public void onBeWithMeStatusChanged(String status ){
        try {
            server.broadcast(new JSONObject().put("event", "onBeWithMeStatusChanged").put("status",status).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 改为用来替代 detectmode，当检测到人的时候返回消息

//        if (status.equals("start")){
//            try{
//                server.broadcast(new JSONObject().put("event","onBeWithMeStatusChanged").put("status",status).toString());
//            }catch (JSONException e ){
//                e.printStackTrace();
//            }
//        }
    }


    @Override
    public void onConstraintBeWithStatusChanged(boolean isConstraint) {

        try{
            server.broadcast(new JSONObject().put("event","onConstraintBeWithStatusChanged").put("state",isConstraint).toString());
        }catch (JSONException e ){
            e.printStackTrace();
        }

    }



    @Override
    public void onTtsStatusChanged(TtsRequest ttsRequest) {
        if (ttsRequest.getStatus() == TtsRequest.Status.COMPLETED) {
            try {
                server.broadcast(new JSONObject()
                    .put("command", "speak")
                    .put("id", speak_id)
                    .put("status", "completed")
                    .toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGoToLocationStatusChanged(@NotNull String location, @GoToLocationStatus String status, int descriptionId, String description) {
        if (status.equals("complete")) {
            try {
                server.broadcast(new JSONObject()
                    .put("command", "goto")
                    .put("id", goto_id)
                    .put("status", "completed")
                    .put("location", location)
                    .toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }



//    @Override
//    public void onMovementStatusChanged(String type, String status){
//
//    }

    @Override
    public void onMovementStatusChanged(@NotNull String type, String status){
        if(status.equals("complete")){
            try {
                server.broadcast(new JSONObject()
                    .put("command", "turn")
                    .put("id", turn_id)
                    .put("status", "completed")
                    .toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onAsrResult(@NotNull String text) {
        try {
            server.broadcast(new JSONObject()
                .put("command", "ask")
                .put("id", ask_id)
                .put("reply", text)
                .put("status", "completed")
                .toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        robot.finishConversation();
    }

    public void stop() {
        stopMoving();
        stopCamera(null); // 关闭摄像头
        //robot.removeTtsListener(this);
        //robot.removeAsrListener(this);
        //robot.removeOnGoToLocationStatusChangedListener(this);
    }

    public void setSurfaceHolder(SurfaceHolder holder) {
        this.surfaceHolder = holder;
    }

    public void startCameraWithId(final String id) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!activity.isCameraOpen) {
                        activity.startCamera();
                    }
                    try {
                        JSONObject result = new JSONObject();
                        result.put("command", "startCamera");
                        result.put("id", id);
                        result.put("status", "success");
                        server.broadcast(result.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        try {
                            JSONObject error = new JSONObject();
                            error.put("command", "startCamera");
                            error.put("error", "Invalid command format");
                            server.broadcast(error.toString());
                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    public void stopCameraWithId(final String id) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.stopCamera();
                    try {
                        JSONObject result = new JSONObject();
                        result.put("command", "stopCamera");
                        result.put("id", id);
                        result.put("status", "success");
                        server.broadcast(result.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void showCameraPreviewWithId(final String id) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject result = new JSONObject();
                        result.put("command", "showCameraPreview");
                        result.put("id", id);
                        
                        if (!activity.isCameraOpen) {
                            result.put("status", "error");
                            result.put("message", "Camera is not open");
                        } else {
                            activity.showCamera();
                            result.put("status", "success");
                        }
                        server.broadcast(result.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void hideCameraPreviewWithId(final String id) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.hideCamera();
                    try {
                        JSONObject result = new JSONObject();
                        result.put("command", "hideCameraPreview");
                        result.put("id", id);
                        result.put("status", "success");
                        server.broadcast(result.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void takePictureWithId(final String id) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.takePictureForWebSocket(new MainActivity.PictureCallback() {
                        @Override
                        public void onPictureTaken(String base64Image, String imagePath) {
                            try {
                                JSONObject result = new JSONObject();
                                result.put("command", "takePicture");
                                result.put("id", id);
                                result.put("path", imagePath);
                                result.put("imageData", base64Image);
                                server.broadcast(result.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            try {
                                JSONObject result = new JSONObject();
                                result.put("command", "takePicture");
                                result.put("id", id);
                                result.put("error", error);
                                server.broadcast(result.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        }
    }

    public void startRecordingWithId(final String id) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.startRecording();
                    try {
                        JSONObject result = new JSONObject();
                        result.put("command", "startRecording");
                        result.put("id", id);
                        result.put("status", "success");
                        server.broadcast(result.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void stopRecordingWithId(final String id) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.stopRecording();
                    try {
                        JSONObject result = new JSONObject();
                        result.put("command", "stopRecording");
                        result.put("id", id);
                        result.put("status", "success");
                        server.broadcast(result.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 手动控制temi持续移动
     * @param x 线速度,范围-1~1,正值前进,负值后退
     * @param y 角速度,范围-1~1,正值左转,负值右转
     */
    public void skidJoy(float x, float y) {
        if (x == 0 && y == 0) {
            stopMoving();
            try {
                server.broadcast(new JSONObject()
                    .put("command", "move")
                    .put("status", "stopped")
                    .put("x", 0)
                    .put("y", 0)
                    .toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

        currentX = x;
        currentY = y;

        if (isMoving) {
            try {
                server.broadcast(new JSONObject()
                    .put("command", "move")
                    .put("status", "updated")
                    .put("x", currentX)
                    .put("y", currentY)
                    .toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }

        isMoving = true;
        try {
            server.broadcast(new JSONObject()
                .put("command", "move")
                .put("status", "started")
                .put("x", currentX)
                .put("y", currentY)
                .toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        movementThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isMoving) {
                    if (robot != null) {
                        robot.skidJoy(currentX, currentY);
                    }
                    try {
                        Thread.sleep(MOVEMENT_INTERVAL);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        movementThread.start();
    }

    /**
     * 停止移动
     */
    private void stopMoving() {
        isMoving = false;
        if (robot != null) {
            robot.skidJoy(0, 0);
        }
        if (movementThread != null) {
            movementThread.interrupt();
            movementThread = null;
        }
        try {
            server.broadcast(new JSONObject()
                .put("command", "move")
                .put("status", "stopped")
                .put("x", 0)
                .put("y", 0)
                .toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void stopCamera(Void unused) {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    // 添加新的方法来处理界面加载
    public void display(final String url, final String commandId) {
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        WebView webView = activity.findViewById(R.id.webview);
                        if (webView != null) {
                            // 配置WebView设置
                            WebSettings webSettings = webView.getSettings();
                            webSettings.setJavaScriptEnabled(true);
                            
                            // 加载URL
                            webView.loadUrl(url);
                            
                            // 发送成功响应
                            JSONObject response = new JSONObject();
                            response.put("command", "display");
                            response.put("id", commandId);
                            response.put("status", "loaded");
                            
                            server.broadcast(response.toString());
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error creating response: " + e.getMessage());
                    }
                }
            });
        }
    }

}

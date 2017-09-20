package com.aviconics.petrobot.petrobotbody.module.ui.activity;

/*******************************************************************************************
 * Modifed by Han Dong
 * 12/8/2013
 * <p>
 * This is a modified version of the Camera Preview sample app provided by OpenCV. The
 * BackgroundSubtractorMOG algorithm was used to identify objects in motion from the videos
 * and draws overlays over them. The ideal is to have good contours drawn around the objects
 * that are moving.
 *******************************************************************************************/

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.configs.AcConfig;
import com.aviconics.petrobot.petrobotbody.em.DemoHelper;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.Event;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventBusUtil;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventCode;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.MotionEvent;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.ReportEvent;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.UIEvent;
import com.aviconics.petrobot.petrobotbody.util.CameraMonitorHelper;
import com.aviconics.petrobot.petrobotbody.util.CameraTimeHelper;
import com.aviconics.petrobot.petrobotbody.util.DialogControl;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MoveShowActivity extends BaseActivity implements CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java");
    }

    private static final String TAG = "MoveShow";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;

    private BackgroundSubtractorMOG sub;
    private Mat mGray;
    private Mat mRgb;
    private Mat mFGMask;
    private List<MatOfPoint> contours;
    private double lRate = 0.5;

    boolean isRemind = false;//标记触发
    boolean isCameraFrame = false;//检测画面渲染的标记
    private int timeCount = 0;
    private int triggerTimes;
    private IsMonitorRunThread thread;
    private Handler handler = new Handler();

    private Runnable timeCountTask = new Runnable() {
        @Override
        public void run() {
            if (timeCount <= 10) {
                handler.postDelayed(this, 1000);
            }
            timeCount++;
        }
    };

    // Initialization required by apps using OpenCV Manager
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
            handler.postDelayed(timeCountTask, 1000);
        }
    };

    @Override
    protected void beforeInit() {
        Log.i(TAG, "called onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_move_show;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void initData() {
        if (thread == null) {
            thread = new IsMonitorRunThread();
            thread.start();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onResume() {
        super.onResume();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        handler.removeCallbacks(timeCountTask);
    }

    public void onCameraViewStarted(int width, int height) {
        //creates a new BackgroundSubtractorMOG class with the arguments
        sub = new BackgroundSubtractorMOG(3, 4, 0.8, 0.5);

        //creates matrices to hold the different frames
        mRgb = new Mat();
        mFGMask = new Mat();
        mGray = new Mat();

        //arraylist to hold individual contours
        contours = new ArrayList<MatOfPoint>();
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        contours.clear();
        //gray frame because it requires less resource to process
        mGray = inputFrame.gray();

        //this function converts the gray frame into the correct RGB format for the BackgroundSubtractorMOG apply function
        Imgproc.cvtColor(mGray, mRgb, Imgproc.COLOR_GRAY2RGB);

        //apply detects objects moving and produces a foreground mask
        //the lRate updates dynamically dependent upon seekbar changes
        sub.apply(mRgb, mFGMask, lRate);

        //erode and dilate are used  to remove noise from the foreground mask
        Imgproc.erode(mFGMask, mFGMask, new Mat());
        Imgproc.dilate(mFGMask, mFGMask, new Mat());

        //drawing contours around the objects by first called findContours and then calling drawContours
        //RETR_EXTERNAL retrieves only external contours
        //CHAIN_APPROX_NONE detects all pixels for each contour
        Imgproc.findContours(mFGMask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        //draws all the contours in red with thickness of 2
        //Imgproc.drawContours(mRgb, contours, -1, new Scalar(255, 0, 0), 2);


        isCameraFrame = true;

        int redPointCount = contours.size();
        LogUtil.i("mMove", "运动检测不匹配点：" + redPointCount);

        if (timeCount > 3) { //运动检测3s之后才做计算
            //当 红点个数 单次超过200,或连续3次超过80视为触发
            if (redPointCount > 200 || triggerTimes > 2) {
                CameraTimeHelper helper = new CameraTimeHelper(getApplicationContext());
                if (helper.isInMultiCameraMonitorTime(new Date()) && !isRemind) {
                    ReportEvent reportEvent = new ReportEvent(AcConfig.Type.MOVEMENT_TYPE, "");
                    EventBusUtil.sendEvent(new Event(EventCode.REPORT,reportEvent));
                    CameraMonitorHelper.openRecorder(handler);
                    isRemind = true;
                }
            } else {
                triggerTimes = redPointCount > 80 ? triggerTimes + 1 : 0;
            }
        }
        return mRgb;
    }


    private class IsMonitorRunThread extends Thread {

        @Override
        public void run() {
            SystemClock.sleep(15 * 1000);//30s后检测一次画面监控画面是否启动，否则重新唤醒
            if (!isCameraFrame) {
                resumeActivity();
            }
        }
    }

    private void resumeActivity() {
        LogUtil.i("NICK", "运动检测渲染失败，重新onPause -> onResume");
        startActivity(new Intent(this, HintActivity.class).putExtra("info", "reStartMovement"));
    }

    @Override
    protected boolean isRegisterEventBus() {
        return true;
    }

    @Override
    protected void receiveEvent(Event event) {
        switch (event.getCode()) {
            case EventCode.MOTION:
                MotionEvent motion = (MotionEvent) event.getData();
                int from = motion.getFrom();

                finishAndToCall(from);
                break;
            default:
                break;
        }
    }

    private void finishAndToCall(final int from) {
        finish();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (DemoHelper.getInstance().isCallArrive()) {
                   /* Intent callIntent = new Intent(MoveShowActivity.this, VideoCallActivity.class);
                    callIntent.putExtra("username", from).putExtra("isComingCall", true);
                    startActivity(callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));*/
                } else {
                    if (DialogControl.getInstance().getDialogType() == DialogControl.Type.CALL) {
                        EventBusUtil.sendEvent(new Event(EventCode.UI,new UIEvent(DialogControl.Type.DISS)));
                    }
                }
            }
        }, 3000);
    }
}

package com.bignerdranch.android.visual_control_by_bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class VisualControlActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    /** 不需要动 **
     ** 范围开始{ **/
    //这是MainFragment启动此Activity并附加信息所需要用到的Intent实现
    public static final String EXTRA_VISUAL_CONTROL_ID =
            "com.bignerdranch.android.visual_control_by_bluetooth.visual_control_id";
    public static Intent newIntent(Context packageContext, String address) {
        Intent intent = new Intent(packageContext, VisualControlActivity.class);
        intent.putExtra(EXTRA_VISUAL_CONTROL_ID, address);
        return intent;
    }
    //蓝牙对象的声明
    private SimpleBluetooth mSimpleBluetooth;
    private String mAddress;
    /** 范围结束} **/

    private static final String TAG = "MTCNNSample::Activity";
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    // 申请OpenCV
    private CameraBridgeViewBase mOpenCvCameraView;
    // 初始化模型
    MTCNN mtcnn;

    //OpenCV初始化
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    //显示结果
    TextView result;
    //开关
    Switch mSwitch = new Switch(false, false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //这是强制屏幕不翻转
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //强制不灭屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.camera_view);
        /** 下面是从前面的fragment获取蓝牙对象，不需要动 **/
        mAddress = (String) getIntent()
                .getSerializableExtra(VisualControlActivity.EXTRA_VISUAL_CONTROL_ID);
        mSimpleBluetooth = new SimpleBluetooth();
        if (!mSimpleBluetooth.ConnectDevice(mAddress)) {
            Log.d("Fail", "Bluetooth doesn't connected\n" + mSimpleBluetooth.isConnectSuccess());
            finish();
        }

        //显示结果
        result = findViewById(R.id.result);

        mtcnn=new MTCNN(getAssets());

        mOpenCvCameraView = findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(640, 640);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //申请权限
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

    }

    //OpenCV初始化
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //程序销毁时关闭该关闭的
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat img_rgba = inputFrame.rgba();
//        Mat img_t = new Mat();
//        Mat hold = new Mat();

        Core.transpose(img_rgba, img_rgba);
//        Imgproc.resize(img_t, img_rgba, img_rgba.size(), 0.0D, 0.0D, 0);
        Core.flip(img_rgba, img_rgba, -1);

        if (img_rgba != null) {
            Bitmap bitmap = Bitmap.createBitmap(img_rgba.width(), img_rgba.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img_rgba, bitmap);
            bitmap = mtcnn.processImage(bitmap, mSimpleBluetooth, result, mSwitch);
            Utils.bitmapToMat(bitmap,img_rgba);
        }

//        img_t.release();
//        hold.release();
        return img_rgba;
    }

}

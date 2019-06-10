package com.bignerdranch.android.visual_control_by_bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Trace;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

@SuppressLint("Registered")
public class VisualControlActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }
    static {
        System.loadLibrary("tensorflow_inference");
    }

    /*******************************
     * 这是MainFragment启动此Activity并附加信息所需要用到的Intent实现，不需要动
     ******************************/
    //范围开始{
    public static final String EXTRA_VISUAL_CONTROL_ID =
            "com.bignerdranch.android.criminalintent.visual_control_id";
    public static Intent newIntent(Context packageContext, SimpleBluetooth simpleBluetooth) {
        Intent intent = new Intent(packageContext, VisualControlActivity.class);
        intent.putExtra(EXTRA_VISUAL_CONTROL_ID, simpleBluetooth);
        return intent;
    }
    //蓝牙对象的声明
    private SimpleBluetooth mSimpleBluetooth;
    //范围结束}

    //下面是视觉识别的声明
    private static final String OCV_TAG = "OCVSample::Activity";
    private CameraBridgeViewBase mOpenCvCameraView;
    //OpenCV初始化时需要用到的一个对象
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(OCV_TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    //TensorFlow部分设置
    private TensorFlowInferenceInterface tensorFlowInferenceInterface = null;
    private String ans = "";
    private static final String mode_file = "file:///android_asset/test.pb";
    private static final String INPUT_NODE = "input_images:0";
    private static final String OUTPUT_NODE = "InceptionV3/Logits/SpatialSqueeze:0";
    private float[] inputs_data = new float[299 * 299 * 3];
    private float[] outputs_data = new float[3];
    private int steps = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //这是强制屏幕不翻转
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.camera_view);
        /**这个是从前面的fragment获取蓝牙对象，不需要动**/
        mSimpleBluetooth = (SimpleBluetooth) getIntent()
                .getSerializableExtra(VisualControlActivity.EXTRA_VISUAL_CONTROL_ID);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tensorFlowInferenceInterface = new TensorFlowInferenceInterface(getAssets(), mode_file);
        mOpenCvCameraView = findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
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
            Log.d(OCV_TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(OCV_TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    //程序销毁时关闭该关闭的
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    //应该是OpenCV接口的默认方法，预处理和显示结果
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //得到当前帧
        Mat img_rgba = inputFrame.rgba();
        Mat img_rgb = new Mat();
        Mat img_t = new Mat();
        steps++;

        Core.transpose(img_rgba, img_t);//转置函数，可以水平的图像变为垂直

        Imgproc.resize(img_t, img_rgba, img_rgba.size(), 0.0D, 0.0D, 0);

        Core.flip(img_rgba, img_rgba, 1);  //flipCode>0将mRgbaF水平翻转（沿Y轴翻转）得到mRgba

        //------------------预处理-----begin--------------------

        //四通道转成三通道
        Imgproc.cvtColor(img_rgba, img_rgb, Imgproc.COLOR_RGBA2RGB);

        //浮点化+限制像素范围
        Mat hold = new Mat();
        img_rgb.convertTo(hold, CvType.CV_32FC3,1 / 255.0);
        //reshape
        Imgproc.resize(hold,img_rgb,new Size(299,299));

        if(steps == 5)
        {
            //遍历Mat
            img_rgb.get(0,0,inputs_data);

            int number = toNumber();
            ans = "";
            /*****************************
             * 使用 mSimpleBluetooth.getConnectedThread().write() 来传输数据
             *****************************/
            switch (number)
            {
                case 0:
                    ans = ans + "front";
                    mSimpleBluetooth.getConnectedThread().write("A");
                    break;
                case 1:
                    ans = ans + "right";
                    mSimpleBluetooth.getConnectedThread().write("D");
                    break;
                case 2:
                    ans = ans + "left";
                    mSimpleBluetooth.getConnectedThread().write("C");
                    break;
                default:
                    ans = ans + "stop";
                    mSimpleBluetooth.getConnectedThread().write("I");
                    break;
            }
            Log.d("tag","cky---"+number);
            steps = 0;
        }

        Point p = new Point(200, 200);
        Imgproc.putText(img_rgba, ans, p, Core.FONT_HERSHEY_DUPLEX,
                2, new Scalar(0, 0, 255), 1);


        // Log.d("tag","cky--------width: "+img_rgb.width() +"  height: "+img_rgb.height() + " channels: "+inputs_data[299*298+1]);

            /*
            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                double contourArea = Imgproc.contourArea(contours.get(contourIdx));
                Rect rect = Imgproc.boundingRect(contours.get(contourIdx));
                if (contourArea < 1500 || contourArea > 20000)
                    continue;

                Mat roi = new Mat(img_gray, rect);
                Imgproc.resize(roi, roi, new Size(28, 28));

                Bitmap bitmap2 = Bitmap.createBitmap(roi.width(), roi.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(roi, bitmap2);

                Log.d("tag", "cky -----width" + roi.width() + "  height:" + roi.height()+"channels: " + roi.channels() );

                //------------------预处理-----end--------------------
                //将处理好后的Bitmap传给模型

                //Log.d("tag","the number is "+ number);
                if (number >= 0) {
                    //tl左上角顶点  br右下角定点
                    //字体大小  字体颜色  字体粗度
                    double x = rect.tl().x;
                    double y = rect.br().y;
                    Point p = new Point(x, y);
                    Imgproc.rectangle(img_rgb, rect.tl(), rect.br(), new Scalar(0, 0, 255));
                    Imgproc.putText(img_rgb, Integer.toString(number), p, Core.FONT_HERSHEY_DUPLEX,
                            6, new Scalar(0, 0, 255), 2);
                }
            }
            */

        img_rgb.release();
        img_t.release();
        hold.release();

        return img_rgba;
    }

    //TensorFlow得到结果
    public int toNumber() {
        //传参
        Trace.beginSection("feed");
        //输入节点名称 输入数据  数据大小
        //填充数据 1，784为神经网络输入层的矩阵大小
        //tensorFlowInferenceInterface.feed(INPUT_NODE, inputs_data, 1,784);
        tensorFlowInferenceInterface.feed(INPUT_NODE, inputs_data, 1, 299, 299, 3);
        Trace.endSection();

        Trace.beginSection("run");
        //运行
        tensorFlowInferenceInterface.run(new String[]{OUTPUT_NODE});
        Trace.endSection();

        Trace.beginSection("fetch");
        //取出数据
        //输出节点名称 输出数组
        tensorFlowInferenceInterface.fetch(OUTPUT_NODE, outputs_data);
        Trace.endSection();

        int logit = 0;
        //找出预测的结果
        for (int i = 1; i < 3; i++) {
            if (outputs_data[i] > outputs_data[logit])
                logit = i;
        }
        Log.d("tag","cky        "+outputs_data[0]+"  "+outputs_data[1]+ "  "+outputs_data[2]);

        return logit;
    }

}

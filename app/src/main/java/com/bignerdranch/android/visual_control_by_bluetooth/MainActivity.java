package com.bignerdranch.android.visual_control_by_bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Trace;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    static {
        System.loadLibrary("opencv_java3");
    }
    static {
        System.loadLibrary("tensorflow_inference");
    }

    private TextView mStatus;

    //下面是蓝牙传输的声明
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mDevicesItemArrayAdapter;
    private String mName;
    private String mAddress;
    // bluetooth background worker thread to send and receive data
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;
    private boolean mAskConnect = false;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        mStatus = findViewById(R.id.Status);
        //下面是蓝牙传输的方法调用
        boolean mCheckOn = checkOn();
        if (mCheckOn)
            askConnect();

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

        mConnectThread.cancel();
        mConnectedThread.cancel();

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
            switch (number)
            {
                case 0:
                    ans = ans + "front";
                    if (mAskConnect)
                        mConnectedThread.write("A");
                    break;
                case 1:
                    ans = ans + "right";
                    if (mAskConnect)
                        mConnectedThread.write("D");
                    break;
                case 2:
                    ans = ans + "left";
                    if (mAskConnect)
                        mConnectedThread.write("C");
                    break;
                default:
                    ans = ans + "stop";
                    if (mAskConnect)
                        mConnectedThread.write("I");
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


    //下面是蓝牙传输的方法与类
    private boolean checkOn() {
        //判断蓝牙是否打开
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
            mDialog.setTitle("Can't open Program.")
                    .setMessage("Bluetooth isn't on.\nTry again later.")
                    .setPositiveButton("Fire", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
            return false;
        }
        return true;
    }

    private void askConnect() {
        //询问是否已连接设备
        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        mDialog.setTitle("Have you connected the device?")
                .setMessage("\"Yes\" to go on.\n\"No\" to close and try again later.")
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectDevice();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void selectDevice() {
        //已在系统设置里连接设备，因此只需要找到连接的设备。但是无法查询已连接设备列表，所以在此手动在已配对设备中选择
        //把配对的设备导到数组中
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() < 1) {
            AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
            mDialog.setTitle("Can't open Program.")
                    .setMessage("you connect no device.\nTry again later.")
                    .setPositiveButton("Fire", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
        mDevicesItemArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        for (BluetoothDevice device : pairedDevices)
            mDevicesItemArrayAdapter.add(device.getName() + "\n\t" + device.getAddress());
        //在对话框中显示与选择后再显示出来
        AlertDialog.Builder mDialog2 = new AlertDialog.Builder(this);
        mDialog2.setTitle("Select the device.")
                .setAdapter(mDevicesItemArrayAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String mDeviceString = mDevicesItemArrayAdapter.getItem(which);
                        mName = mDeviceString.substring(0, mDeviceString.indexOf("\n"));
                        mAddress = mDeviceString.substring(mDeviceString.indexOf("\t") + 1);
                        mStatus.setText(mName + "\n\t" + mAddress);
                        //建立传输信息通道
                        mConnectThread = new ConnectThread(mBluetoothAdapter.getRemoteDevice(mAddress));
                        mConnectThread.run();
                    }
                })
                .setNegativeButton("None", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(BT_UUID);
            } catch (IOException ignored) { }
            mmSocket = tmp;
        }

        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                //下面处理是在 StackOverflow 找的，貌似没用上
                Log.e("Socket connect:",connectException.getMessage());
                try {
                    Log.e("","trying fallback...");
                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
                    mmSocket.connect();
                    Log.e("","Connected");
                } catch (Exception e2) {
                    Log.e("", "Couldn't establish Bluetooth connection!\n" + e2.getMessage());
                    try {
                        mmSocket.close();
                    } catch (IOException ignored) { }
                    return;
                }
            }
            Toast.makeText(getBaseContext(), "Connected",
                    Toast.LENGTH_SHORT).show();
            // Do work to manage the connection (in a separate thread)
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
            mAskConnect = true;
        }

        /** Will cancel an in-progress connection, and close the socket */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;

            // Get the output streams, using temp objects because
            // member streams are final
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) { }

            mmOutStream = tmpOut;
        }

        /* Call this from the main activity to send data to the remote device */
        void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("OutStream write:",e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }
}

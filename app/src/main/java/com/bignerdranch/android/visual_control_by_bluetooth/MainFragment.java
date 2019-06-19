package com.bignerdranch.android.visual_control_by_bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainFragment extends Fragment implements View.OnClickListener {

    private static final String DIALOG_PAIRED = "DialogPaired";
    private static final String DIALOG_DISCOVER = "DialogPaired";
    private static final int REQUEST_PAIRED = 0;
    private static final int REQUEST_DISCOVER = 1;
    private SimpleBluetooth mSimpleBluetooth;
    private String mAddress;

    private TextView mStatus;
    private Button mPairedButton;
    private Button mDiscoverButton;
    private Button go;
    private Button right;
    private Button left;
    private Button back;
    private Button stop;
    private Button start;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        mStatus = v.findViewById(R.id.Status);
        mPairedButton = v.findViewById(R.id.connect_paired_devices);
        mDiscoverButton = v.findViewById(R.id.discover_new_devices);
        go = v.findViewById(R.id.go);
        left = v.findViewById(R.id.left);
        stop = v.findViewById(R.id.stop);
        right = v.findViewById(R.id.right);
        back = v.findViewById(R.id.back);
        start = v.findViewById(R.id.start_visual_control);


        //下面是蓝牙传输的方法调用
        //检查蓝牙状态
        mSimpleBluetooth = new SimpleBluetooth();
        mStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOn();
            }
        });
        //连接配对设备
        mPairedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager manager = getFragmentManager();
                ConnectPairedFragment dialog = ConnectPairedFragment
                        .newInstance(mSimpleBluetooth);
                dialog.setTargetFragment(MainFragment.this, REQUEST_PAIRED);
                dialog.show(manager, DIALOG_PAIRED);
            }
        });
        //搜索新设备
        mDiscoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                FragmentManager manager = getFragmentManager();
//                ConnectDiscoveredFragment dialog = ConnectDiscoveredFragment
//                        .newInstance(mSimpleBluetooth);
//                dialog.setTargetFragment(MainFragment.this, REQUEST_DISCOVER);
//                dialog.show(manager, DIALOG_DISCOVER);
                mDiscoverButton.setText("功能部署中，请去系统设置连接后再用上面按钮进行连接");
            }
        });

        go.setOnClickListener(this);
        right.setOnClickListener(this);
        left.setOnClickListener(this);
        back.setOnClickListener(this);
        stop.setOnClickListener(this);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = VisualControlActivity.newIntent(getContext(), mAddress);
                startActivity(intent);
            }
        });

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mSimpleBluetooth.isConnectSuccess())
            mSimpleBluetooth.ConnectCancel();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.go:
                mSimpleBluetooth.getConnectedThread().write("A");
                break;
            case R.id.right:
                mSimpleBluetooth.getConnectedThread().write("D");
                break;
            case R.id.left:
                mSimpleBluetooth.getConnectedThread().write("C");
                break;
            case R.id.back:
                mSimpleBluetooth.getConnectedThread().write("B");
                break;
            case R.id.stop:
                mSimpleBluetooth.getConnectedThread().write("I");
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        //返回想要连接的设备的地址
        if (requestCode == REQUEST_PAIRED) {
            String deviceString = (String) data
                    .getSerializableExtra(ConnectPairedFragment.EXTRA_PAIRED);
            mAddress = deviceString.substring(deviceString.indexOf("\t") + 1);
            //连接设备！
            if (!BluetoothAdapter.checkBluetoothAddress(mAddress)) {
                mStatus.setText("蓝牙地址有误，不能连接！");
                Toast.makeText(getActivity(), "蓝牙地址有误，不能连接！",
                        Toast.LENGTH_LONG).show();
            }
            else if (mSimpleBluetooth.ConnectDevice(mAddress)) {
                mStatus.setText(deviceString);
                Toast.makeText(getActivity(), "连接设备成功！",
                    Toast.LENGTH_LONG).show();
                go.setEnabled(true);
                left.setEnabled(true);
                stop.setEnabled(true);
                right.setEnabled(true);
                back.setEnabled(true);
                start.setEnabled(true);
            } else {
                mStatus.setText("连接设备失败！请重试吧！");
                Toast.makeText(getActivity(), "连接设备失败！请重试吧！",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    //下面是蓝牙传输部分
    //判断蓝牙是否打开
    private void checkOn() {
        if (!mSimpleBluetooth.getBluetoothAdapter().isEnabled()) {
            mStatus.setText("蓝牙未打开，点一下再检查一次");
        }
        else {
            mStatus.setText("蓝牙已打开，连接设备吧");
            mPairedButton.setEnabled(true);
            mDiscoverButton.setEnabled(true);
            mStatus.setEnabled(false);
        }
    }
}

package com.bignerdranch.android.visual_control_by_bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;

import java.util.Set;

public class ConnectPairedFragment extends DialogFragment {

    public static final String EXTRA_PAIRED =
            "com.bignerdranch.android.criminalintent.date";

    private static final String ARG_PAIRED = "date";

    public static ConnectPairedFragment newInstance(SimpleBluetooth mSimpleBluetooth) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_PAIRED, mSimpleBluetooth);
        Bundle args = new Bundle();
        args.putSerializable(ARG_PAIRED, mSimpleBluetooth);
        ConnectPairedFragment fragment = new ConnectPairedFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SimpleBluetooth mSimpleBluetooth = (SimpleBluetooth) getArguments().getSerializable(ARG_PAIRED);

        Set<BluetoothDevice> pairedDevices = mSimpleBluetooth.getBluetoothAdapter().getBondedDevices();
        final ArrayAdapter<String> mDevicesItemArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        for (BluetoothDevice device : pairedDevices)
            mDevicesItemArrayAdapter.add(device.getName() + "\n\t" + device.getAddress());
        if (pairedDevices.size() == 0)
            mDevicesItemArrayAdapter.add("没有设备\n\t没有地址");
        return new AlertDialog.Builder(getActivity())
                .setTitle("Hello, dialog")
                .setAdapter(mDevicesItemArrayAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String deviceString = mDevicesItemArrayAdapter.getItem(which);
                        sendResult(Activity.RESULT_OK, deviceString);
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    private void sendResult(int resultCode, String deviceString) {
        if (getTargetFragment() == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_PAIRED, deviceString);
        getTargetFragment()
                .onActivityResult(getTargetRequestCode(), resultCode, intent);
    }
}

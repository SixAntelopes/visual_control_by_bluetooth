package com.bignerdranch.android.visual_control_by_bluetooth;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.ArrayAdapter;


public class ConnectDiscoveredFragment extends DialogFragment {

    public static final String EXTRA_DISCOVER =
            "com.bignerdranch.android.criminalintent.date";

    private static final String ARG_DISCOVER = "date";

    private ArrayAdapter<String> mArrayAdapter;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n\t" + device.getAddress());
            }
        }
    };

    public static ConnectDiscoveredFragment newInstance(SimpleBluetooth mSimpleBluetooth) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DISCOVER, mSimpleBluetooth);
        Bundle args = new Bundle();
        args.putSerializable(ARG_DISCOVER, mSimpleBluetooth);
        ConnectDiscoveredFragment fragment = new ConnectDiscoveredFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        SimpleBluetooth mSimpleBluetooth = (SimpleBluetooth) getArguments().getSerializable(ARG_DISCOVER);

        mArrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
        // Create a BroadcastReceiver for ACTION_FOUND

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getContext().registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        mSimpleBluetooth.getBluetoothAdapter().startDiscovery();

        mArrayAdapter.add("其实运行过这段");

        return new AlertDialog.Builder(getActivity())
                .setTitle("Hello, dialog")
                .setAdapter(mArrayAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String deviceString = mArrayAdapter.getItem(which);
                        sendResult(Activity.RESULT_OK, deviceString);
                    }
                })
                .setPositiveButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getContext().unregisterReceiver(mReceiver);
    }

    private void sendResult(int resultCode, String deviceString) {
        if (getTargetFragment() == null) {
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DISCOVER, deviceString);
        getTargetFragment()
                .onActivityResult(getTargetRequestCode(), resultCode, intent);
    }
}

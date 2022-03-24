package com.example.hardintfhub;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "USB_HOST";
    private final static int VENDOR_ID = 0x483;
    private final static int PRODUCT_ID = 0x5750;

    private InterfaceTerminal mIntfTerm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleButton ledButton = findViewById(R.id.toggleButton);
        ledButton.setOnCheckedChangeListener(this);

        /*
        if (myDeviceConnection == null || myEndPointOut == null || myEndPointIn == null) {
            Log.e(TAG, "Usb hid init failed");
            return;
        }

        ret = myDeviceConnection.bulkTransfer(myEndPointOut, sendBuffer, sendBuffer.length, 0);
        if (ret < 0) {
            Log.e(TAG, "Usb hid send failed: " + ret);
            return;
        }

        int inMax = myEndPointIn.getMaxPacketSize();
        Log.d(TAG, "input packet length: " + inMax);

        byte[] receiveBuffer = new byte[inMax];
        ret = myDeviceConnection.bulkTransfer(myEndPointIn, receiveBuffer, receiveBuffer.length,2000);
        if (ret < 0) {
            Log.e(TAG, "Usb hid receive failed: " + ret);
            return;
        }
        Log.d(TAG, "Usb hid get data: " + Arrays.toString(receiveBuffer));

         */
        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        mIntfTerm = new InterfaceTerminal(usbManager);

        try {
            mIntfTerm.connect(VENDOR_ID, PRODUCT_ID);
        } catch (Exception e) {
            Log.e(TAG, "Usb connect failed\n");
            return;
        };

        mIntfTerm.start();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton.isChecked()) {
            mIntfTerm.gpioWrite(UsbCmdPacket.Group.C, 13, false);
        } else {
            mIntfTerm.gpioWrite(UsbCmdPacket.Group.C, 13, true);
        }
    }
}
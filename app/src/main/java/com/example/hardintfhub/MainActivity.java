package com.example.hardintfhub;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "USB_HOST";
    private final static int VENDOR_ID = 0x483;
    private final static int PRODUCT_ID = 0x5750;
    private ToggleButton mLedButton;
    private ImageView mLedView;

    private InterfaceTerminal mIntfTerm;

    private class LedControlThread extends Thread {
        @Override
        public void run() {
            while (mLedView != null && mIntfTerm != null) {
                boolean isPress = !mIntfTerm.gpioRead(UsbCmdPacket.Group.A, 0);
                if (isPress) {
                    mLedView.clearColorFilter();
                } else {
                    mLedView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                }

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLedButton = findViewById(R.id.toggleButton);
        mLedButton.setOnCheckedChangeListener(this);

        mLedView = findViewById(R.id.imageView);
        mLedView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        mIntfTerm = new InterfaceTerminal(usbManager);

        try {
            mIntfTerm.connect(VENDOR_ID, PRODUCT_ID);
        } catch (Exception e) {
            Log.e(TAG, "Usb connect failed\n");
            return;
        };

        mIntfTerm.start();

        LedControlThread ledControlThread = new LedControlThread();
        ledControlThread.start();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton.isChecked()) {
            mIntfTerm.gpioWrite(UsbCmdPacket.Group.C, 13, false);
            //Log.d(TAG, "Read PA0: " + mIntfTerm.gpioRead(UsbCmdPacket.Group.A, 0));
        } else {
            mIntfTerm.gpioWrite(UsbCmdPacket.Group.C, 13, true);
        }
    }
}
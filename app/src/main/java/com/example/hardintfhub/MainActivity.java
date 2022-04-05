package com.example.hardintfhub;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Arrays;

class UartReceiveThread extends Thread {
    private InterfaceTerminal mIntfTerm;
    private TextView mUartTest;

    public UartReceiveThread(InterfaceTerminal intfTerm, TextView uartTest) {
        this.mIntfTerm = intfTerm;
        this.mUartTest = uartTest;
    }

    @Override
    public void run() {
        StringBuffer contentBuffer = new StringBuffer("");
        while (true) {
            byte[] buf = new byte[10];
            int readLen = mIntfTerm.uartRead(2, buf);
            if (readLen > 0) {
                contentBuffer.append(new String(Arrays.copyOfRange(buf, 0, readLen)));
                if (mUartTest.getLineCount() > mUartTest.getMaxLines()) {
                    int end = contentBuffer.indexOf("\n");
                    contentBuffer.delete(0, end + 1);
                }

                mUartTest.setText(contentBuffer);
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "USB_HOST";
    private final static int VENDOR_ID = 0x483;
    private final static int PRODUCT_ID = 0x5750;
    private ToggleButton mLedButton;
    private ImageView mLedView;
    private TextView mUartTestView;
    private Button mSendButton;
    private EditText mSendEditText;

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

        UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
        mIntfTerm = new InterfaceTerminal(usbManager);
        try {
            mIntfTerm.connect(VENDOR_ID, PRODUCT_ID);
        } catch (Exception e) {
            Log.e(TAG, "Usb connect failed\n");
            return;
        };

        mIntfTerm.start();

        mLedButton = findViewById(R.id.toggleButton);
        mLedButton.setOnCheckedChangeListener(this);

        mLedView = findViewById(R.id.imageView);
        mLedView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        LedControlThread ledControlThread = new LedControlThread();
        ledControlThread.start();

        mUartTestView = findViewById(R.id.textView);
        UartReceiveThread uartThread = new UartReceiveThread(mIntfTerm, mUartTestView);
        uartThread.start();

        mSendButton = findViewById(R.id.sendBotton);
        mSendEditText = findViewById(R.id.editTextContent);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = mSendEditText.getText().toString();
                mIntfTerm.uartWrite(2, str.getBytes());
            }
        });
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
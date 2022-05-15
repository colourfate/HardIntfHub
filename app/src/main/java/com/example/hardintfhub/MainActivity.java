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

import com.example.intfdefine.HardGpio;
import com.example.intfdefine.HardIntf;
import com.example.intfdefine.HardSerial;
import com.example.intfdefine.InterfaceFactory;

import java.util.Arrays;

class UartReceiveThread extends Thread {
    private final HardSerial mSerial;
    private final TextView mUartTest;

    public UartReceiveThread(HardSerial serial, TextView uartTest) {
        this.mSerial = serial;
        this.mUartTest = uartTest;
    }

    @Override
    public void run() {
        StringBuffer contentBuffer = new StringBuffer("");
        while (true) {
            byte[] buf = new byte[10];
            int readLen = mSerial.read(buf);
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
    private final static int PRODUCT_ID = 0x5740;
    private ToggleButton mLedButton;
    private ImageView mLedView;
    private TextView mUartTestView;
    private Button mSendButton;
    private EditText mSendEditText;
    private HardGpio PC13, PA0;

    private InterfaceFactory mIntfFactory;

    private class LedControlThread extends Thread {
        @Override
        public void run() {
            while (mLedView != null) {
                boolean isPress = PA0.read();
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
        mIntfFactory = new InterfaceFactory(usbManager);
        mIntfFactory.connect(VENDOR_ID, PRODUCT_ID);
        try {
            mIntfFactory.start();
        } catch (Exception e) {
            Log.e(TAG, "Usb connect failed\n");
            return;
        };

        PA0 = (HardGpio)mIntfFactory.createHardIntf(InterfaceFactory.IntfType.GPIO);
        PA0.setPort(HardIntf.Group.A, 0);
        PA0.setType(HardIntf.Type.GPIO);
        PA0.setDir(HardIntf.Dir.IN);
        PA0.config();
        mLedView = findViewById(R.id.imageView);
        mLedView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        LedControlThread ledControlThread = new LedControlThread();
        ledControlThread.start();

        PC13 = (HardGpio)mIntfFactory.createHardIntf(InterfaceFactory.IntfType.GPIO);
        PC13.setPort(HardIntf.Group.C, 13);
        PC13.setType(HardIntf.Type.GPIO);
        PC13.setDir(HardIntf.Dir.OUT);
        PC13.config();
        mLedButton = findViewById(R.id.toggleButton);
        mLedButton.setOnCheckedChangeListener(this);


        mUartTestView = findViewById(R.id.textView);
        HardSerial serial2 = (HardSerial)mIntfFactory.createHardIntf(InterfaceFactory.IntfType.Serial);
        serial2.setTx(HardIntf.Group.A, 2);
        serial2.setRx(HardIntf.Group.A, 3);
        serial2.setType(HardIntf.Type.SERIAL);
        serial2.setUartNum(2);
        serial2.setBuadRate(HardSerial.BuadRate.BUAD_115200);
        serial2.setWordLen(HardSerial.WordLen.LEN_8);
        serial2.setStopBit(HardSerial.StopBit.BIT_1);
        serial2.config();

        UartReceiveThread uartThread = new UartReceiveThread(serial2, mUartTestView);
        uartThread.start();

        mSendButton = findViewById(R.id.sendBotton);
        mSendEditText = findViewById(R.id.editTextContent);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = mSendEditText.getText().toString();
                serial2.write(str.getBytes());
            }
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        PC13.write(!compoundButton.isChecked());
    }
}
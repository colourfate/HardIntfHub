package com.example.hardintfhub;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USB_HOST";
    private ImageView mLedView;
    private TextView mUartTestView;
    private EditText mSendEditText;
    private UsbCmdService mUsbCmdService;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(this, UsbCmdService.class);
        bindService(intent, mServiceConnection,  Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ToggleButton mLedButton = findViewById(R.id.toggleButton);
        mLedButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.e(TAG, "onCheckedChanged: " + b);
                mUsbCmdService.setLedState(compoundButton.isChecked());
            }
        });

        Button mSendButton = findViewById(R.id.sendBotton);
        mSendEditText = findViewById(R.id.editTextContent);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str = mSendEditText.getText().toString();
                mUsbCmdService.sendString(str);
            }
        });

        mLedView = findViewById(R.id.imageView);
        mLedView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        mUartTestView = findViewById(R.id.textView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConnection);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            UsbCmdService.UsbCmdBinder binder = (UsbCmdService.UsbCmdBinder)service;
            mUsbCmdService = binder.getService();

            if (mUsbCmdService == null) {
                Log.e(TAG, "get Service failed");
                return;
            }

            mUsbCmdService.registerEvent(UsbCmdService.BUTT_CHANGE_EVENT, new UsbEventIntf() {
                @Override
                public void callBack(byte[] data) {
                    if (data[0] == 0) {
                        mLedView.clearColorFilter();
                    } else {
                        mLedView.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                    }
                }
            });

            mUsbCmdService.registerEvent(UsbCmdService.STR_READ_EVENT, new UsbEventIntf() {
                final StringBuffer contentBuffer = new StringBuffer("");
                @Override
                public void callBack(byte[] data) {
                    contentBuffer.append(new String(Arrays.copyOfRange(data, 0, data.length)));
                    if (mUartTestView.getLineCount() > mUartTestView.getMaxLines()) {
                        int end = contentBuffer.indexOf("\n");
                        contentBuffer.delete(0, end + 1);
                    }

                    mUartTestView.setText(contentBuffer);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
}
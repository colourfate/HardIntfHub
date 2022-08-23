package com.example.intfdefine;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.security.auth.login.LoginException;

public class InterfaceFactory {
    private final static String TAG = "USB_FACTORY";
    private final static int MAX_INFO_LEN = 128;

    private final UsbManager myUsbManager;
    private UsbDevice myUsbDevice;
    private UsbDeviceConnection myDeviceConnection;
    private UsbSerialDevice mSerial;
    private final ConcurrentHashMap<Integer, HardIntfEvent> mEventMap =
            new ConcurrentHashMap<Integer, HardIntfEvent>();

    public enum IntfType {
        GPIO, Serial, PWM, ADC;
    }

    public InterfaceFactory(UsbManager usbManager) {
        this.myUsbManager = usbManager;
    }

    private void enumerateDevice(int vendorId, int productId) {
        if (myUsbManager == null) {
            Log.e(TAG, "Class not instantiate");
            return;
        }

        HashMap<String, UsbDevice> deviceList = myUsbManager.getDeviceList();
        if (deviceList.isEmpty()) {
            Log.e(TAG, "Not usb device connect");
            return;
        }

        for (UsbDevice device : deviceList.values()) {
            Log.d(TAG, "DeviceInfo: " + device.getVendorId() + " , "
                    + device.getProductId());

            if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                myUsbDevice = device;
                Log.d(TAG, "enumerate device success");
            }
        }
    }

    private void openDevice() {
        if (myUsbDevice == null) {
            Log.e(TAG, "Not find device");
            return;
        }

        // specified by AndroidManifest.xml and res/xml/device_filter.xml
        if (myUsbManager.hasPermission(myUsbDevice)) {
            myDeviceConnection = myUsbManager.openDevice(myUsbDevice);
        } else {
            Log.e(TAG, "openDevice: Not permission");
        }
    }

    private final UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            if (arg0.length < 3) {
                Log.e(TAG, "Protocol error: " + Arrays.toString(arg0));
                return;
            }

            if ((HardIntf.Mode.INFO.getPacket() ^ arg0[0]) == 0) {
                int dataLen = arg0[2];
                if (dataLen > MAX_INFO_LEN) {
                    Log.e(TAG, "Invalid Data len: " + dataLen);
                    return;
                }

                Log.i("USB_SLAVE" ,new String(arg0, 3, dataLen));
                return;
            }

            int key = HardIntf.getPacketId(arg0);
            HardIntfEvent event = mEventMap.get(key);
            if (event == null) {
                Log.e(TAG, "The event is not registered: 0x" + Integer.toHexString(key));
                return;
            } else {
                Log.i(TAG, "get event: 0x" + Integer.toHexString(key));
            }
            event.handle(arg0);
        }
    };

    public void connect(int vendorId, int productId) {
        enumerateDevice(vendorId, productId);
        openDevice();
    }

    public void start() throws IOException {
        if (myUsbDevice == null || myDeviceConnection == null) {
            throw new IOException("Not connect terminal");
        }

        mSerial = UsbSerialDevice.createUsbSerialDevice(myUsbDevice, myDeviceConnection);
        if (mSerial == null) {
            Log.e(TAG, "createUsbSerialDevice failed");
            throw new IOException("Not support terminal");
        }

        Log.i(TAG, "Create CDC devices success");
        // No physical serial port, no need to set parameters
        mSerial.open();
        mSerial.read(mCallback);
    }

    public HardIntf createHardIntf(IntfType intfType) {
        if (intfType == IntfType.Serial) {
            return new HardSerial(mSerial, mEventMap);
        } else if (intfType == IntfType.GPIO) {
            return new HardGpio(mSerial, mEventMap);
        } else if (intfType == IntfType.PWM) {
            return new HardPwm(mSerial, mEventMap);
        } else if (intfType == IntfType.ADC) {
            return new HardAdc(mSerial, mEventMap);
        } else {
            Log.e(TAG, "createHardIntf: Not support intf type");
        }

        return null;
    }
}

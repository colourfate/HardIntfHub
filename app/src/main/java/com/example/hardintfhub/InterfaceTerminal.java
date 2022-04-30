package com.example.hardintfhub;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

abstract class IntfEventListener {
    private int mUserRet = 0;
    private boolean mIsHandled = false;
    private UsbCmdPacket mUsbCmdPacket;

    IntfEventListener(UsbCmdPacket usbCmdPacket) {
        this.mUsbCmdPacket = usbCmdPacket;
    }

    public int getUserRet() { return mUserRet; }
    public boolean isHandled() { return mIsHandled; }

    abstract int userHandle(UsbCmdPacket receivePacket);
    public void handleIntfEvent(UsbCmdPacket receivePacket) {
        if (!mUsbCmdPacket.comparePack(receivePacket)) {
            return;
        }
        mUserRet = userHandle(receivePacket);
        mIsHandled = true;
    }
}

public class InterfaceTerminal {
    private final static String TAG = "USB_HOST";

    private UsbManager myUsbManager;
    private UsbDevice myUsbDevice;
    private UsbDeviceConnection myDeviceConnection;
    private UsbSerialDevice mSerial;
    private ConcurrentLinkedQueue<IntfEventListener> mListenerList = new ConcurrentLinkedQueue<IntfEventListener>();

    InterfaceTerminal(UsbManager usbManager) {
        this.myUsbManager = usbManager;
    }

    public void gpioWrite(UsbCmdPacket.Group group, int pin, boolean value) {
        if (pin > UsbCmdPacket.GPIO_PIN_CNT) {
            Log.e(TAG, "Not support pin: " + pin);
            return;
        }

        byte[] data = { (byte)(value ? 1 : 0) };
        UsbCmdPacket usbCmdPacket = new UsbCmdPacket(UsbCmdPacket.USB_PACKET_MIN);
        usbCmdPacket.setType(UsbCmdPacket.Type.GPIO);
        usbCmdPacket.setDir(UsbCmdPacket.Dir.OUT);
        usbCmdPacket.setGroup(group);
        usbCmdPacket.setPin(pin);
        usbCmdPacket.setData(data);
        mSerial.write(usbCmdPacket.mPacket);
    }

    public boolean gpioRead(UsbCmdPacket.Group group, int pin) {
        if (pin > UsbCmdPacket.GPIO_PIN_CNT) {
            Log.e(TAG, "Not support pin: " + pin);
            return false;
        }

        byte[] data = new byte[1];
        UsbCmdPacket usbCmdPacket = new UsbCmdPacket(UsbCmdPacket.USB_PACKET_MIN);
        usbCmdPacket.setType(UsbCmdPacket.Type.GPIO);
        usbCmdPacket.setDir(UsbCmdPacket.Dir.IN);
        usbCmdPacket.setGroup(group);
        usbCmdPacket.setPin(pin);
        usbCmdPacket.setData(data);
        IntfEventListener gpioReadListener = new IntfEventListener(usbCmdPacket) {
            @Override
            public int userHandle(UsbCmdPacket receivePacket) {
                byte[] receiveData = new byte[1];
                receivePacket.getData(receiveData);
                return receiveData[0];
            }
        };

        mListenerList.add(gpioReadListener);
        mSerial.write(usbCmdPacket.mPacket);
        while (!gpioReadListener.isHandled()) {};
        mListenerList.remove(gpioReadListener);

        return gpioReadListener.getUserRet() == 1;
    }

    public void uartWrite(int uartNum, byte[] data) {
        if (uartNum != 2) {
            Log.e(TAG, "Not support uartNum: " + uartNum);
            return;
        }

        if (data.length == 0) {
            return;
        }

        UsbCmdPacket usbCmdPacket = new UsbCmdPacket(UsbCmdPacket.USB_PACKET_MIN + data.length - 1);
        usbCmdPacket.setType(UsbCmdPacket.Type.SERIAL);
        usbCmdPacket.setDir(UsbCmdPacket.Dir.OUT);
        usbCmdPacket.setGroup(UsbCmdPacket.Group.MUL_FUNC);
        usbCmdPacket.setPin(uartNum);
        usbCmdPacket.setData(data);
        mSerial.write(usbCmdPacket.mPacket);
    }

    public int uartRead(int uartNum, byte[] data) {
        if (uartNum != 2) {
            Log.e(TAG, "Not support uartNum: " + uartNum);
            return 0;
        }

        UsbCmdPacket usbCmdPacket = new UsbCmdPacket(UsbCmdPacket.USB_PACKET_MIN + data.length - 1);
        usbCmdPacket.setType(UsbCmdPacket.Type.SERIAL);
        usbCmdPacket.setDir(UsbCmdPacket.Dir.IN);
        usbCmdPacket.setGroup(UsbCmdPacket.Group.MUL_FUNC);
        usbCmdPacket.setPin(uartNum);
        usbCmdPacket.setData(data);
        IntfEventListener uartReadListener = new IntfEventListener(usbCmdPacket) {
            @Override
            public int userHandle(UsbCmdPacket receivePacket) {
                return receivePacket.getData(data);
            }
        };

        mListenerList.add(uartReadListener);
        mSerial.write(usbCmdPacket.mPacket);
        while (!uartReadListener.isHandled()) {};
        mListenerList.remove(uartReadListener);

        return uartReadListener.getUserRet();
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

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            UsbCmdPacket usbCmdPacket = new UsbCmdPacket(arg0.length);
            for (int i = 0; i < arg0.length; i++) {
                usbCmdPacket.put(arg0[i]);
            }

            for (IntfEventListener listener : mListenerList) {
                listener.handleIntfEvent(usbCmdPacket);
            }
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

        Log.d(TAG, "Create CDC devices success");
        // No physical serial port, no need to set parameters
        mSerial.open();
        mSerial.read(mCallback);
    }
}

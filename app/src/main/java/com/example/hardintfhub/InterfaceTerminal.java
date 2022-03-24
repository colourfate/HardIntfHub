package com.example.hardintfhub;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

class UsbSendThread extends Thread {
    private final static String TAG = "USB_HOST";
    private Queue<Byte> mOutQueue = new ArrayBlockingQueue<Byte>(8 * UsbCmdPacket.USB_PACKET_MAX);
    private SpinLock mSendLock = new SpinLock();
    private UsbDeviceConnection mUsbDeviceConn;
    private UsbEndpoint mEndPointOut;

    UsbSendThread(UsbDeviceConnection usbDeviceConn, UsbEndpoint endPointOut) {
        this.mUsbDeviceConn = usbDeviceConn;
        this.mEndPointOut = endPointOut;
    }

    public void sendPacket(UsbCmdPacket usbCmdPacket) {
        mSendLock.lock();
        while (usbCmdPacket.hasNext()) {
            mOutQueue.add((byte)usbCmdPacket.next());
        }
        mSendLock.unlock();
    }

    @Override
    public void run() {
        int ret;

        while (true) {
            byte[] sendBuffer = new byte[UsbCmdPacket.USB_PACKET_MAX];
            int i = 0;

            mSendLock.lock();
            try {
                for (i = 0; i < UsbCmdPacket.USB_PACKET_MAX; i++) {
                    sendBuffer[i] = mOutQueue.remove();
                }
            } catch (Exception e) {}
            mSendLock.unlock();

            if (i == 0) {
                continue;
            }

            Log.d(TAG, "send buffer: " + Arrays.toString(sendBuffer));
            ret = mUsbDeviceConn.bulkTransfer(mEndPointOut, sendBuffer, i, 0);
            if (ret < 0) {
                Log.e(TAG, "Usb hid send failed: " + ret);
                return;
            }
        }
    }
}

public class InterfaceTerminal {
    private final static String TAG = "USB_HOST";

    private UsbManager myUsbManager;
    private UsbDevice myUsbDevice;
    private UsbInterface myInterface;
    private UsbDeviceConnection myDeviceConnection;
    private UsbEndpoint myEndPointOut;
    private UsbEndpoint myEndPointIn;

    private UsbSendThread mSendThread;

    InterfaceTerminal(UsbManager usbManager) {
        this.myUsbManager = usbManager;
    }

    public void gpioWrite(UsbCmdPacket.Group group, int pin, boolean value) {
        if (pin > UsbCmdPacket.GPIO_PIN_CNT) {
            Log.e(TAG, "Not support pin: " + pin);
            return;
        }

        byte[] data = { (byte)(value ? 1 : 0) };
        UsbCmdPacket usbCmdPacket = new UsbCmdPacket(4);
        usbCmdPacket.setType(UsbCmdPacket.Type.GPIO);
        usbCmdPacket.setDir(UsbCmdPacket.Dir.OUT);
        usbCmdPacket.setGroup(group);
        usbCmdPacket.setPin(pin);
        usbCmdPacket.setData(data);
        mSendThread.sendPacket(usbCmdPacket);
    }

    private void enumerateDevice(int vendorId, int productId) {
        if (myUsbManager == null) {
            Log.e(TAG, "Class not instantiate");
            return;
        }

        HashMap<String, UsbDevice> deviceList = myUsbManager.getDeviceList();
        if (!deviceList.isEmpty()) { // deviceList不为空
            for (UsbDevice device : deviceList.values()) {
                Log.d(TAG, "DeviceInfo: " + device.getVendorId() + " , "
                        + device.getProductId());

                if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                    myUsbDevice = device;
                    Log.d(TAG, "enumerate device success");
                }
            }
        }
    }

    private void findInterface() {
        if (myUsbDevice != null) {
            Log.d(TAG, "interfaceCounts : " + myUsbDevice.getInterfaceCount());
            for (int i = 0; i < myUsbDevice.getInterfaceCount(); i++) {
                UsbInterface intf = myUsbDevice.getInterface(i);
                if (intf.getInterfaceClass() == 3 && intf.getInterfaceSubclass() == 0
                        && intf.getInterfaceProtocol() == 0) {
                    myInterface = intf;
                    Log.d(TAG, "find service interface");
                }
                break;
            }
        }
    }

    private void openDevice() {
        if (myInterface != null) {
            UsbDeviceConnection conn = null;
            // specified by AndroidManifest.xml and res/xml/device_filter.xml
            if (myUsbManager.hasPermission(myUsbDevice)) {
                conn = myUsbManager.openDevice(myUsbDevice);
            }

            if (conn == null) {
                Log.e(TAG, "open Devices failed");
                return;
            }

            if (conn.claimInterface(myInterface, true)) {
                myDeviceConnection = conn; // 到此你的android设备已经连上HID设备
                Log.d(TAG, "open devices success");
            } else {
                conn.close();
            }
        }
    }

    /*
     #define USB_ENDPOINT_XFER_CONTROL 0 --控制传输
     #define USB_ENDPOINT_XFER_ISOC 1 --等时传输
     #define USB_ENDPOINT_XFER_BULK 2 --块传输
     #define USB_ENDPOINT_XFER_INT 3 --中断传输
     * */
    private void assignEndpoint() {
        if (myInterface != null) {
            for (int i = 0; i < myInterface.getEndpointCount(); i++) {
                UsbEndpoint ep = myInterface.getEndpoint(i);
                Log.d(TAG, "Type: " + ep.getType() + ", Dir: " + ep.getDirection());

                if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                    if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        myEndPointOut = ep;
                    } else {
                        myEndPointIn = ep;
                    }
                }
            }
        }
    }

    public void connect(int vendorId, int productId) throws Exception {
        enumerateDevice(vendorId, productId);
        findInterface();
        openDevice();
        assignEndpoint();

        if (myDeviceConnection == null || myEndPointOut == null || myEndPointIn == null) {
            throw new Exception("Usb connect failed\n");
        }
    }

    public void start () {
        if (mSendThread == null) {
            mSendThread = new UsbSendThread(myDeviceConnection, myEndPointOut);
            mSendThread.start();
        }
    }
}

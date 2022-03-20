package com.example.hardintfhub;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "USB_HOST";
    private static final byte[] sendBuffer = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    private TextView info;

    private final int VendorID = 0x483;
    private final int ProductID = 0x5750;
    private UsbManager myUsbManager;
    private UsbDevice myUsbDevice;
    private UsbInterface myInterface;
    private UsbDeviceConnection myDeviceConnection;
    private UsbEndpoint myEndPointOut;
    private UsbEndpoint myEndPointIn;

    private void enumerateDevice() {
        if (myUsbManager == null) {
            return;
        }

        HashMap<String, UsbDevice> deviceList = myUsbManager.getDeviceList();
        if (!deviceList.isEmpty()) { // deviceList不为空
            StringBuffer sb = new StringBuffer();
            for (UsbDevice device : deviceList.values()) {
                sb.append(device.toString());
                sb.append("\n");
                info.setText(sb);
                // 输出设备信息
                Log.d(TAG, "DeviceInfo: " + device.getVendorId() + " , "
                        + device.getProductId());

                // 枚举到设备
                if (device.getVendorId() == VendorID
                        && device.getProductId() == ProductID) {
                    myUsbDevice = device;
                    Log.d(TAG, "枚举设备成功");
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
                    Log.d(TAG, "找到我的设备接口");
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
                Log.d(TAG, "打开设备成功");
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
        if (myInterface != null) { //这一句不加的话 很容易报错  导致很多人在各大论坛问:为什么报错呀
            //这里的代码替换了一下 按自己硬件属性判断吧
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int ret;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = (TextView) findViewById(R.id.info);

        myUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        enumerateDevice();
        findInterface();
        openDevice();
        assignEndpoint();

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
        InterfaceTerminal intfTerm = new InterfaceTerminal();
        intfTerm.start();
        intfTerm.gpioWrite(InterfaceTerminal.Group.C, 13, true);
        intfTerm.gpioWrite(InterfaceTerminal.Group.C, 13, false);
    }
}
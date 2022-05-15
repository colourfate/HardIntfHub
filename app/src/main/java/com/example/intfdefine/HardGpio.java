package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.util.concurrent.ConcurrentLinkedQueue;

public class HardGpio extends HardIntf {
    private Dir mDir;

    HardGpio(UsbSerialDevice usbSerial, ConcurrentLinkedQueue<IntfEventListener> listenerList) {
        super(usbSerial, listenerList, 1);
    }

    public void setPort(Group group, int pin) {
        super.setPort(0, new Port(group, pin));
    }

    public void setDir(Dir dir) { mDir = dir; }

    public void config() {
        byte[] gpio_config = new byte[1];
        super.config(gpio_config, mDir);
    }

    public void write(boolean value) {
        byte[] buf = new byte[1];
        Port port = super.getPort(0);

        if (mDir.getValue() != Dir.OUT.getValue()) {
            Log.e("HOST_USB", "write: Not support dir");
            return;
        }
        buf[0] = (byte)(value ? 1 : 0);
        super.write(buf, port.group, port.pin);
    }

    public boolean read() {
        byte[] buf = new byte[1];
        Port port = super.getPort(0);

        if (mDir.getValue() != Dir.IN.getValue()) {
            Log.e("HOST_USB", "write: Not support dir");
            return false;
        }

        int ret = super.read(buf, port.group, port.pin, new IntfEventListener() {
            @Override
            int userHandle(byte[] receivePakcet) {
                return receivePakcet[3];
            }
        });

        return ret == 1;
    }
}

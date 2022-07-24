package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HardGpio extends HardIntf {
    private Dir mDir;

    HardGpio(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 1);
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

    public byte read() {
        byte[] buf = new byte[1];
        Port port = super.getPort(0);

        if (mDir.getValue() != Dir.IN.getValue()) {
            Log.e("HOST_USB", "read: Not support dir");
            return 0;
        }

        int ret = super.read(buf, port.group, port.pin, new HardIntfEvent() {
            @Override
            int userHandle(byte[] receivePakcet) {
                return receivePakcet[3];
            }
        });

        return (byte)ret;
    }
}

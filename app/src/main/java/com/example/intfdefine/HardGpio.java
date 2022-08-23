package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class HardGpio extends HardIntf {
    public HardGpio(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 1);
        super.setType(Type.GPIO);
    }

    public void setPort(Group group, int pin) {
        super.setPort(0, group, pin);
    }

    public void config() throws IOException {
        byte[] gpio_config = new byte[1];
        super.config(gpio_config);
    }

    public void write(boolean value) {
        byte[] buf = new byte[1];

        buf[0] = (byte)(value ? 1 : 0);
        super.write(buf, 0);
    }

    public byte read() {
        byte[] buf = new byte[1];

        int ret = super.read(buf, 0, new HardIntfEvent() {
            @Override
            int userHandle(byte[] receivePacket) {
                return receivePacket[3];
            }
        });

        return (byte)ret;
    }
}

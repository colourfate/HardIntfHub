package com.example.intfdefine;

import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class HardAdc extends HardIntf {
    public HardAdc(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 1);
        super.setType(Type.ADC);
    }

    public void setPort(Group group, int pin) {
        super.setPort(0, group, pin);
    }

    public void config() throws IOException {
        byte[] attr = new byte[1];
        super.config(attr);
    }

    public int read() {
        byte[] buf = new byte[2];

        return super.read(0, buf, new HardIntfEvent() {
            @Override
            public int userHandle(byte[] receivePacket) {
                if (receivePacket[2] != 2) {
                    Log.e(TAG, "adc read data length: " + receivePacket[2]);
                    return 0;
                }
                return (receivePacket[3] | receivePacket[4] << 8) & 0xffff;
            }
        });
    }
}

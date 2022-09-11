package com.example.intfdefine;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class HardPwm extends HardIntf {
    private int frequency = 500;
    private final static int PWM_MAX_FREQ = 50000;
    private final static int PWM_MIN_FREQ = 1;
    private final static int PWM_MAX_PULSE = 1000;
    private final static int PWM_MIN_PULSE = 0;

    protected HardPwm(UsbSerialDevice usbSerial, ConcurrentHashMap<Integer, HardIntfEvent> eventMap) {
        super(usbSerial, eventMap, 1);
        super.setType(Type.PWM);
    }

    public void setPort(Group group, int pin) {
        super.setPort(0, group, pin);
    }

    public void setPwmFreq(int freq) {
        if (freq > PWM_MAX_FREQ) {
            freq = PWM_MAX_FREQ;
        } else if (freq < PWM_MIN_FREQ) {
            freq = PWM_MIN_FREQ;
        }

        this.frequency = freq;
    }

    public void config() throws IOException {
        byte[] attr = new byte[2];

        attr[0] = (byte)frequency;
        attr[1] = (byte)(frequency >> 8);
        super.config(attr);
    }

    public void write(int value) {
        byte[] data = new byte[2];

        if (value > PWM_MAX_PULSE) {
            value = PWM_MAX_PULSE;
        } else if (value < PWM_MIN_PULSE) {
            value = PWM_MIN_PULSE;
        }

        data[0] = (byte)value;
        data[1] = (byte)(value >> 8);
        super.write(0, data);
    }
}

package com.example.hardintfhub;

import java.util.Iterator;

class UsbCmdPacket implements Iterator {
    public final static int GPIO_PIN_CNT = 16;
    public final static int USB_PACKET_MAX = 64;
    private int mIndex;
    byte[] mPacket;

    public enum Type {
        GPIO(0), ADC(1), SERIAL(2), I2C(3), SPI(4), CAN(5);
        private int value;
        Type(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public enum Dir {
        IN(0), OUT(1);
        private int value;
        Dir(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public enum Group {
        A(0), B(1), C(2), D(3), E(4), F(5), G(6), H(7), MUL_FUNC(8);
        private int value;
        Group(int i) {
            this.value = i;
        }
        public int getValue() {
            return value;
        }
    }

    public void setType(Type type) {
        mPacket[0] |= (type.getValue() << 1);
    }

    public void setDir(Dir dir) {
        mPacket[0] |= dir.getValue();
    }

    public void setGroup(Group group) {
        mPacket[1] |= (group.getValue() << 4);
    }

    public void setPin(int pin) {
        if (pin >= GPIO_PIN_CNT) {
            pin = 15;
        }
        mPacket[1] |= pin;
    }

    public void setData(byte[] data) {
        byte dataLen;

        if (data == null) {
            return;
        }

        dataLen = (byte)data.length;
        if (data.length > USB_PACKET_MAX - 4) {
            dataLen = USB_PACKET_MAX - 4;
        }

        mPacket[2] = dataLen;
        for (int i = 0; i < dataLen; i++) {
            mPacket[3 + i] = data[i];
        }
    }

    UsbCmdPacket (int packetLen) {
        if (packetLen < 4) {
            packetLen = 4;
        }

        mPacket = new byte[packetLen];
        mIndex = 0;
    }

    @Override
    public boolean hasNext() {
        if (mIndex < mPacket.length) {
            return true;
        }

        return false;
    }

    @Override
    public Object next() {
        return mPacket[mIndex++];
    }
}

package com.example.intfdefine;

public class ByteTool {
    public static int s8Tou8(byte x) {
        return ((int) x) & 0xff;
    }

    public static int s8Tou16 (byte msb, byte lsb) {
        return (s8Tou8(msb) << 8 | s8Tou8(lsb));
    }

    public static int s8Tos16(byte msb, byte lsb) {
        return (short)s8Tou16(msb, lsb);
    }
}

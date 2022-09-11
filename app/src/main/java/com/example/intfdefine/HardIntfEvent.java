package com.example.intfdefine;

abstract class HardIntfEvent {
    private volatile boolean mIsHandled = false;
    private int mUserRet = 0;

    protected abstract int userHandle(byte[] receivePacket);
    protected void handle(byte[] receivePacket) {
        mUserRet = userHandle(receivePacket);
        mIsHandled = true;
    }

    protected int getUserRet() {
        // FIXME: 添加超时处理
        while (!mIsHandled);
        return mUserRet;
    }
}

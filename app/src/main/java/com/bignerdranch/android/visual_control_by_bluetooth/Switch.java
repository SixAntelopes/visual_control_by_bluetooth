package com.bignerdranch.android.visual_control_by_bluetooth;

public class Switch {
    private boolean mSwitch;
    private boolean wait;
    private int lastResult;
    private int nowResult;
    Switch(boolean mSwitch, boolean wait) {
        this.mSwitch = mSwitch;
        this.wait = wait;
        this.lastResult = 4;
    }

    boolean ismSwitch() {
        return mSwitch;
    }

    void setmSwitch(boolean mSwitch) {
        this.mSwitch = mSwitch;
    }

    public int getLastResult() {
        return lastResult;
    }

    public void setLastResult(int lastResult) {
        this.lastResult = lastResult;
    }

    public int getNowResult() {
        return nowResult;
    }

    public void setNowResult(int nowResult) {
        this.nowResult = nowResult;
    }

    public boolean isWait() {
        return wait;
    }

    public void setWait(boolean wait) {
        this.wait = wait;
    }
}

package org.openhab.binding.philtest.handler;

public interface SerialTestHandler {

    void onBrightnessChanged(int brightness);

    void onLedOnOffStateChanged(boolean on);
}

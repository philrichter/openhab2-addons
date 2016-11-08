package org.openhab.binding.serialroomsensor.handler;

import java.io.IOException;

import jssc.SerialPort;
import jssc.SerialPortEventListener;

public interface SerialListener extends SerialPortEventListener {

    void setSerialPort(SerialPort port) throws IOException;

    /**
     * Removes all resources, closes the streams and the serial port.
     */
    void close();
}

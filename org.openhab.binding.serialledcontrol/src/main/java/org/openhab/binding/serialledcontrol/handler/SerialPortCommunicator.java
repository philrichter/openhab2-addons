package org.openhab.binding.serialledcontrol.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class SerialPortCommunicator {

    private static final Logger logger = LoggerFactory.getLogger(SerialPortCommunicator.class);

    /** The port we're normally going to use. */
    private static final String PORT_NAMES[] = { "/dev/tty.usbserial-A9007UX1", // Mac OS X
            "/dev/ttyACM0", // Raspberry Pi
            "/dev/ttyUSB0", // Linux
            "COM3", // Windows
    };

    SerialPort serialPort;

    /** The output stream to the port */
    private OutputStream output;
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 9600;

    public SerialPortCommunicator() {
    }

    /**
     * @param serialPortToUse <code>null</code> if the default ports should be used, otherwise the port name (i.e.
     *            /dev/ttyAMC0)
     * @return
     */
    public String initialize(String serialPortToUse) {

        // First, Find an instance of serial port as set in PORT_NAMES.
        CommPortIdentifier port = findSerialPort(serialPortToUse);

        if (port == null) {
            logger.info("initialize: Could not find serial port");
            return "Could not find serial port";
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = port.open(this.getClass().getName(), TIME_OUT);

            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            output = serialPort.getOutputStream();

            return null;
        } catch (Exception e) {
            System.err.println(e.toString());
            return e.getMessage();
        }
    }

    /**
     * @param serialPortToUse <code>null</code> if the default ports should be used, otherwise the port name (i.e.
     *            /dev/ttyAMC0)
     */
    private CommPortIdentifier findSerialPort(String serialPortToUse) {

        logger.info("findSerialPort: "
                + (serialPortToUse != null ? "use specific serial port configured by user: '" + serialPortToUse + "'"
                        : "no special port defined. Try to find right port..."));

        Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();

        if (!ports.hasMoreElements()) {
            // If Raspberry Pi is used need s special property have to be set:
            // http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
            System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");
            System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyUSB0");
            ports = CommPortIdentifier.getPortIdentifiers();
            logger.info("findSerialPort: add system property for raspi...");
        }

        while (ports.hasMoreElements()) {
            CommPortIdentifier currPort = (CommPortIdentifier) ports.nextElement();

            logger.info("findSerialPort: port found: " + currPort.getName() + " (type: " + currPort.getPortType()
                    + ", owner: "
                    + (currPort.isCurrentlyOwned() ? currPort.getCurrentOwner() : "[currently not owned]"));

            if (serialPortToUse != null) {
                if (currPort.getName().equals(serialPortToUse)) {
                    logger.info("findSerialPort: '" + currPort.getName() + "' matches port to use. Use it!");
                    return currPort;
                }
            } else {
                for (String portName : PORT_NAMES) {
                    if (currPort.getName().equals(portName)) {
                        logger.info(
                                "findSerialPort: '" + currPort.getName() + "' matches with a default port. Use it!");
                        return currPort;
                    }
                }
            }
        }
        return null;
    }

    /**
     * This should be called when you stop using the port.
     * This will prevent port locking on platforms like Linux.
     */
    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    public void switchON() {
        try {
            output.write("ON\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void switchOFF() {
        try {
            output.write("OFF\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

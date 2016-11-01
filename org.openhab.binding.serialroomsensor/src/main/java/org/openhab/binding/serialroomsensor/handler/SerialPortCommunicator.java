package org.openhab.binding.serialroomsensor.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.serialroomsensor.SerialRoomSensorBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class SerialPortCommunicator implements SerialPortEventListener {

    private static final String LINE_DELIMITER = "\r\n";

    private static final Logger LOG = LoggerFactory.getLogger(SerialPortCommunicator.class);

    /** The port we're normally going to use. */
    private static final String PORT_NAMES[] = { "/dev/tty.usbserial-A9007UX1", // Mac OS X
            "/dev/ttyACM0", // Raspberry Pi
            "/dev/ttyUSB0", // Linux
            "COM3", // Windows
    };

    public interface SerialTestHandler {
        void onBrightnessChanged(int brightness);

        void onTemperatureChanged(int temperature);

        void onHumidityChanged(int humidity);
    }

    public interface SerialThing {
        String PORT = "port";

        String getId();

        String getLabel();

        /** The serial port name. */
        String getPort();

        ThingUID getUID();
    }

    SerialPort serialPort;
    /**
     * A BufferedReader which will be fed by a InputStreamReader
     * converting the bytes into characters
     * making the displayed results codepage independent
     */
    private BufferedReader input;
    /** The output stream to the port */
    private OutputStream output;
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 9600;

    /** This handler gets the value of an incoming serial event. */
    private SerialTestHandler handler;

    public SerialPortCommunicator(SerialTestHandler handler) {
        this.handler = handler;
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
            LOG.info("initialize: Could not find serial port");
            return "Could not find serial port";
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = port.open(this.getClass().getName(), TIME_OUT);

            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // open the streams
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

            return null;
        } catch (Exception e) {
            System.err.println(e.toString());
            return e.toString();
        }
    }

    public static final Map<String, SerialThing> searchSerialThings() {

        Map<String, SerialThing> serialThings = new HashMap<String, SerialThing>();

        Enumeration<?> ports = findAllPorts();

        while (ports.hasMoreElements()) {
            CommPortIdentifier currPort = (CommPortIdentifier) ports.nextElement();
            SerialThing serialThing = createSerialThing(currPort);
            serialThings.put(serialThing.getId(), serialThing);
        }
        return serialThings;
    }

    private static SerialThing createSerialThing(CommPortIdentifier currPort) {
        return new SerialThing() {

            private ThingUID uid = null;

            @Override
            public String getPort() {
                return currPort.getName();
            }

            @Override
            public String getLabel() {
                return "SerialRoomSensor an Port " + currPort.getName();
            }

            @Override
            public String getId() {
                return currPort.getName();
            }

            @Override
            public ThingUID getUID() {
                if (uid == null) {
                    uid = new ThingUID(SerialRoomSensorBindingConstants.THING_TYPE_STATE,
                            SerialRoomSensorBindingConstants.BINDING_ID, getId());
                }
                return uid;
            }
        };
    }

    /**
     * @param serialPortToUse <code>null</code> if the default ports should be used, otherwise the port name (i.e.
     *            /dev/ttyAMC0)
     */
    private CommPortIdentifier findSerialPort(String serialPortToUse) {

        LOG.info("findSerialPort: "
                + (serialPortToUse != null ? "use specific serial port configured by user: '" + serialPortToUse + "'"
                        : "no special port defined. Try to find right port..."));

        Enumeration<?> ports = findAllPorts();

        while (ports.hasMoreElements()) {
            CommPortIdentifier currPort = (CommPortIdentifier) ports.nextElement();

            LOG.info("findSerialPort: port found: " + currPort.getName() + " (type: " + currPort.getPortType()
                    + ", owner: "
                    + (currPort.isCurrentlyOwned() ? currPort.getCurrentOwner() : "[currently not owned]"));

            if (serialPortToUse != null) {
                if (currPort.getName().equals(serialPortToUse)) {
                    LOG.info("findSerialPort: '" + currPort.getName() + "' matches port to use. Use it!");
                    return currPort;
                }
            } else {
                for (String portName : PORT_NAMES) {
                    if (currPort.getName().equals(portName)) {
                        LOG.info("findSerialPort: '" + currPort.getName() + "' matches with a default port. Use it!");
                        return currPort;
                    }
                }
            }
        }
        return null;
    }

    private static Enumeration<?> findAllPorts() {
        Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();

        if (!ports.hasMoreElements()) {
            // If Raspberry Pi is used need s special property have to be set:
            // http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
            System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");
            System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyUSB0");
            ports = CommPortIdentifier.getPortIdentifiers();
            LOG.info("findSerialPort: add system property for raspi...");
        }
        return ports;
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
        if (serialPort != null) {
            try {
                input.close();
            } catch (IOException e) {
                LOG.error("error during close input stream", e);
            }
            try {
                output.close();
            } catch (IOException e) {
                LOG.error("error during close output stream", e);
            }
        }
    }

    char[] buffer = new char[64];
    String serialInputBuffer = "";

    /**
     * Handle an event on the serial port. Read the data and print it.
     */
    @Override
    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                int count;
                while (input.ready() && (count = input.read(buffer)) > -1) {

                    serialInputBuffer += new String(buffer, 0, count);

                    int newLineIndex;
                    while ((newLineIndex = serialInputBuffer.indexOf(LINE_DELIMITER)) > -1) {

                        if (newLineIndex > -1) {

                            String command = serialInputBuffer.substring(0, newLineIndex);
                            serialInputBuffer = serialInputBuffer.substring(command.length() + LINE_DELIMITER.length());
                            if (handler != null) {
                                if (command.startsWith("BRIGHTNESS=")) {
                                    handler.onBrightnessChanged(parseBrightness(command));
                                } else if (command.startsWith("TEMPERATURE=")) {
                                    handler.onTemperatureChanged(parseTemperature(command));
                                } else if (command.startsWith("HUMIDITY=")) {
                                    handler.onHumidityChanged(parseHumidity(command));
                                } else if (command.startsWith("LOG=")) {
                                    LOG.debug(parseLogMessage(command));
                                } else {
                                    LOG.info("unknown incoming serial event: " + command + "; currently in queue: "
                                            + serialInputBuffer);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("error during serial input processing", e);
            }
        }
    }

    private int parseBrightness(String inputLine) {
        return Integer.parseInt(inputLine.substring(inputLine.indexOf("=") + 1));
    }

    private int parseTemperature(String inputLine) {
        return Integer.parseInt(inputLine.substring(inputLine.indexOf("=") + 1));
    }

    private int parseHumidity(String inputLine) {
        return Integer.parseInt(inputLine.substring(inputLine.indexOf("=") + 1));
    }

    private String parseLogMessage(String inputLine) {
        return inputLine.substring(inputLine.indexOf("=") + 1);
    }

    public void sendUpdateState() {
        try {
            output.write("REFRESH\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendRequestCurrentValues() {
        try {
            output.write("CURRENTVALUES\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

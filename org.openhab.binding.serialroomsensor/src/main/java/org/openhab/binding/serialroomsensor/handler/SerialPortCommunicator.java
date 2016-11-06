package org.openhab.binding.serialroomsensor.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.UnsupportedCommOperationException;

public class SerialPortCommunicator {

    private static final String LINE_DELIMITER = "\r\n";

    private static final Logger LOG = LoggerFactory.getLogger(SerialPortCommunicator.class);

    /** The port we're normally going to use. */
    private static final String PORT_NAMES[] = { "/dev/tty.usbserial-A9007UX1", // Mac OS X
            "/dev/ttyACM0", // Raspberry Pi
            "/dev/ttyUSB0", // Linux
            "COM3", // Windows
    };

    private final class SerialCommunication implements SerialListener {

        private char[] buffer = new char[64];
        private String serialInputBuffer = "";
        private SerialPort serialPort = null;
        /**
         * A BufferedReader which will be fed by a InputStreamReader
         * converting the bytes into characters
         * making the displayed results codepage independent
         */
        private BufferedReader input;
        /** The output stream to the port */
        private OutputStream output;

        @Override
        public void serialEvent(SerialPortEvent oEvent) {
            if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {

                    if (input == null) {
                        throw new Exception("inputReader not initialized");
                    }

                    int count;
                    while (input.ready() && (count = input.read(buffer)) > -1) {

                        serialInputBuffer += new String(buffer, 0, count);

                        int newLineIndex;
                        while ((newLineIndex = serialInputBuffer.indexOf(LINE_DELIMITER)) > -1) {

                            if (newLineIndex > -1) {

                                String command = serialInputBuffer.substring(0, newLineIndex);
                                serialInputBuffer = serialInputBuffer
                                        .substring(command.length() + LINE_DELIMITER.length());
                                if (handler != null) {
                                    if (command.startsWith("BRIGHTNESS=")) {
                                        handler.onBrightnessChanged(parseBrightness(command));
                                    } else if (command.startsWith("TEMPERATURE=")) {
                                        handler.onTemperatureChanged(parseTemperature(command));
                                    } else if (command.startsWith("HUMIDITY=")) {
                                        handler.onHumidityChanged(parseHumidity(command));
                                    } else if (command.startsWith("LOG=")) {
                                        LOG.debug(parseLogMessage(command));
                                    } else if (command.startsWith("BUTTONPRESSED=")) {
                                        handler.onDoorbellPressed(parseButtonPressed(command));
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

        @Override
        public void setSerialPort(SerialPort port) throws IOException {
            serialPort = port;
            input = new BufferedReader(new InputStreamReader(port.getInputStream()));
            output = port.getOutputStream();
        }

        public void write(byte[] data) throws IOException {
            if (output != null) {
                output.write(data);
            } else {
                LOG.error("output stream already closed! Data '" + data + "' could not be send.");
            }
        }

        @Override
        public void close() {
            if (serialPort != null) {
                serialPort.removeEventListener();
                try {
                    input.close();
                    input = null;
                } catch (IOException e) {
                    LOG.error("error during close input stream", e);
                }
                try {
                    output.close();
                    output = null;
                } catch (IOException e) {
                    LOG.error("error during close output stream", e);
                }
                serialPort.close();
                serialPort = null;
            }
        }
    }

    public interface SerialTestHandler {
        void onBrightnessChanged(int brightness);

        void onTemperatureChanged(int temperature);

        void onHumidityChanged(int humidity);

        void onDoorbellPressed(boolean pressed);
    }

    public interface SerialThingListener {
        void onFound(SerialThing thing);
    }

    public interface SerialThing {
        String PORT = "port";

        String getId();

        String getLabel();

        /** The serial port name. */
        String getPort();

        ThingUID getThingUID();

        ThingTypeUID getTypeUID();
    }

    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 4000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 9600;

    /** This handler gets the value of an incoming serial event. */
    private SerialTestHandler handler;

    private SerialCommunication serialCommunication = null;

    public SerialPortCommunicator(SerialTestHandler handler) {
        this.handler = handler;
    }

    /**
     * @param serialPortToUse <code>null</code> if the default ports should be used, otherwise the port name (i.e.
     *            /dev/ttyAMC0)
     * @return
     * @throws IOException
     * @throws TooManyListenersException
     * @throws UnsupportedCommOperationException
     * @throws PortInUseException
     */
    public void initialize(String serialPortToUse)
            throws IOException, PortInUseException, UnsupportedCommOperationException, TooManyListenersException {

        serialCommunication = new SerialCommunication();
        initializePortAccess(findSerialPort(serialPortToUse), serialCommunication);
    }

    public static final Map<String, SerialThing> searchSerialThings(Set<ThingTypeUID> supportedThingTypes,
            SerialThingListener listener) {

        Map<String, SerialThing> serialThings = new HashMap<String, SerialThing>();

        Enumeration<?> ports = findAllPorts();

        while (ports.hasMoreElements()) {
            CommPortIdentifier currPort = (CommPortIdentifier) ports.nextElement();
            if (!currPort.isCurrentlyOwned()) {
                LOG.info("searchSerialThings: serial thing found at port " + currPort.getName()
                        + " send identify request...");
                identifySerialDevice(currPort, listener, supportedThingTypes);
            } else {
                LOG.info("searchSerialThings: port " + currPort.getName() + " currently in use -> ignore");
            }
        }
        return serialThings;
    }

    private static ThingTypeUID getSupportedTypeUid(String typeId, Set<ThingTypeUID> supportedThingTypes) {
        for (ThingTypeUID thingTypeUID : supportedThingTypes) {
            if (thingTypeUID.getId().equals(typeId)) {
                return thingTypeUID;
            }
        }
        return null;
    }

    private static void identifySerialDevice(CommPortIdentifier currPort, SerialThingListener listener,
            Set<ThingTypeUID> supportedThingTypes) {

        try {
            SerialPort serialPort = initializePortAccess(currPort,
                    createSerialEventListener(currPort, listener, supportedThingTypes));

            // request the type of the serial device
            serialPort.getOutputStream().write("TYPEID\n".getBytes());

        } catch (Exception e) {
            LOG.error("error during identify serial port device on port %s", currPort.getName(), e);
        }
    }

    private static SerialListener createSerialEventListener(CommPortIdentifier currPort, SerialThingListener listener,
            Set<ThingTypeUID> supportedThingTypes) {

        return new SerialListener() {

            private final static int TIMEOUT_IDENTIFICATION = 10000;

            private char[] buffer = new char[64];
            private String serialInputBuffer = "";
            private BufferedReader inputReader = null;
            private SerialPort serialPort = null;

            private long startTime = -1;

            @Override
            public void serialEvent(SerialPortEvent oEvent) {

                // terminate identification after timeout
                if (startTime == -1) {
                    startTime = System.currentTimeMillis();
                } else if (startTime + TIMEOUT_IDENTIFICATION < System.currentTimeMillis()) {
                    close();
                    return;
                }

                if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                    try {

                        if (inputReader == null) {
                            throw new Exception("inputReader not initialized");
                        }

                        int count;
                        while (inputReader.ready() && (count = inputReader.read(buffer)) > -1) {

                            serialInputBuffer += new String(buffer, 0, count);

                            int newLineIndex;
                            while ((newLineIndex = serialInputBuffer.indexOf(LINE_DELIMITER)) > -1) {

                                if (newLineIndex > -1) {

                                    String command = serialInputBuffer.substring(0, newLineIndex);
                                    serialInputBuffer = serialInputBuffer
                                            .substring(command.length() + LINE_DELIMITER.length());

                                    if (command.startsWith("TYPEID=")) {

                                        ThingTypeUID typeUid = getSupportedTypeUid(parseTypeId(command),
                                                supportedThingTypes);

                                        if (typeUid != null) {
                                            listener.onFound(createSerialThing(currPort, typeUid)); // TODO label
                                        }
                                        close();
                                    } else if (command.startsWith("LOG=")) {
                                        LOG.debug(parseLogMessage(command));
                                    } else {
                                        LOG.debug("unknown incoming serial event: " + command + "; currently in queue: "
                                                + serialInputBuffer);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("error during serial input processing ", e);
                    }
                }
            }

            @Override
            public void setSerialPort(SerialPort port) throws IOException {
                serialPort = port;
                inputReader = new BufferedReader(new InputStreamReader(port.getInputStream()));
            }

            @Override
            public void close() {
                if (serialPort != null) {
                    serialPort.removeEventListener();
                    try {
                        inputReader.close();
                    } catch (IOException e) {
                        LOG.error("error during close input stream", e);
                    }
                    serialPort.close();
                }
            }
        };
    }

    private static SerialPort initializePortAccess(CommPortIdentifier port, SerialListener listener)
            throws IOException, PortInUseException, UnsupportedCommOperationException, TooManyListenersException {

        if (port == null) {
            throw new IOException("initialize: Could not find serial port");
        }

        SerialPort serialPort = port.open(SerialPortCommunicator.class.getName(), TIME_OUT);

        serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        serialPort.addEventListener(listener);
        serialPort.notifyOnDataAvailable(true);

        listener.setSerialPort(serialPort);

        return serialPort;
    }

    private static SerialThing createSerialThing(CommPortIdentifier currPort, ThingTypeUID typeUid) {

        final ThingUID thingUid = new ThingUID(typeUid, currPort.getName());

        return new SerialThing() {

            @Override
            public String getPort() {
                return currPort.getName();
            }

            @Override
            public String getLabel() {
                // TODO Zugriff auf Label aus thing-types.xml aktuell nicht möglich
                return ("roomsensor".equals(typeUid.getId()) ? "Raumsensor" : "Türklingel") + " an Port " + getPort();
            }

            @Override
            public String getId() {
                return currPort.getName();
            }

            @Override
            public ThingUID getThingUID() {
                return thingUid;
            }

            @Override
            public ThingTypeUID getTypeUID() {
                return typeUid;
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
            LOG.info("findAllPorts: add system property for raspi...");
        }
        return ports;
    }

    /**
     * This should be called when you stop using the port.
     * This will prevent port locking on platforms like Linux.
     */
    public synchronized void close() {
        if (serialCommunication != null) {
            serialCommunication.close();
            serialCommunication = null;
        }
    }

    private boolean parseButtonPressed(String command) {
        return command.endsWith(Boolean.TRUE.toString());
    }

    private static String parseTypeId(String inputLine) {
        return inputLine.substring(inputLine.indexOf("=") + 1);
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

    private static String parseLogMessage(String inputLine) {
        return inputLine.substring(inputLine.indexOf("=") + 1);
    }

    public void sendRequestCurrentValues() {
        try {
            if (serialCommunication != null) {
                serialCommunication.write("CURRENTVALUES\n".getBytes());
            } else {
                LOG.warn("serialCommunication already closed! Current values could not be requested.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

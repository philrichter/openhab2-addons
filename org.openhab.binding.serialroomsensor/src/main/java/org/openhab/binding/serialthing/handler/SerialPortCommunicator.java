package org.openhab.binding.serialthing.handler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TooManyListenersException;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortList;

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

        private String serialInputBuffer = "";
        private SerialPort serialPort = null;

        @Override
        public void serialEvent(SerialPortEvent oEvent) {
            if (serialPort != null && oEvent.isRXCHAR() && oEvent.getEventValue() > 0) {
                try {

                    byte[] buffer;
                    while ((buffer = serialPort.readBytes()) != null) {

                        serialInputBuffer += new String(buffer);

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
                                    } else if (command.startsWith("DOORBELL_PRESSED=")) {
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
        }

        public void write(byte[] data) throws SerialPortException {
            if (serialPort != null) {
                serialPort.writeBytes(data);
            } else {
                LOG.error("output stream already closed! Data '" + data + "' could not be send.");
            }
        }

        @Override
        public void close() {
            if (serialPort != null) {
                try {
                    serialPort.removeEventListener();
                    serialPort.closePort();
                } catch (SerialPortException e) {
                    LOG.error("error during closing port", e);
                }
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

        /**
         * @return <code>false</code> if the thing with the given port is already discovered, otherwise
         *         <code>true</code>
         */
        boolean isNew(String port);
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
     * @throws SerialPortException
     */
    public void initialize(String serialPortToUse) throws IOException, SerialPortException {

        serialCommunication = new SerialCommunication();

        SerialPort serialPort = findSerialPort(serialPortToUse);
        serialPort.openPort();
        initializePortAccess(serialPort, serialCommunication);
    }

    public static final Set<String> searchSerialThings(Set<ThingTypeUID> supportedThingTypes,
            SerialThingListener listener) {

        Set<String> serialThings = new HashSet<String>();

        String[] ports = findAllPorts();

        for (String port : ports) {
            SerialPort serialPort = new SerialPort(port);

            try {
                if (!serialPort.isOpened() && listener.isNew(serialPort.getPortName()) && serialPort.openPort()) {
                    LOG.info("searchSerialThings: serial thing found at port " + serialPort.getPortName()
                            + " send identify request...");
                    identifySerialDevice(serialPort, listener, supportedThingTypes);
                    serialThings.add(serialPort.getPortName());
                } else {
                    LOG.info("searchSerialThings: port " + serialPort.getPortName() + " currently in use -> ignore");
                }
            } catch (SerialPortException e) {
                LOG.info("searchSerialThings: port " + serialPort.getPortName() + " currently in use -> ignore");
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

    private static void identifySerialDevice(SerialPort currPort, SerialThingListener listener,
            Set<ThingTypeUID> supportedThingTypes) {

        try {
            initializePortAccess(currPort, createSerialEventListener(currPort, listener, supportedThingTypes));
            requestTypeId(currPort);

        } catch (Exception e) {
            LOG.error("error during identify serial port device on port %s", currPort.getPortName(), e);
        }
    }

    private static void requestTypeId(SerialPort currPort) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    // request the type of the serial device
                    currPort.writeBytes("TYPEID\n".getBytes());
                } catch (Exception e) {
                    LOG.error("error during requesting type id of serial thing", e);
                }
            }
        }).start();
    }

    private static SerialListener createSerialEventListener(SerialPort currPort, SerialThingListener listener,
            Set<ThingTypeUID> supportedThingTypes) {

        return new SerialListener() {

            private final static int TIMEOUT_IDENTIFICATION = 30000;

            private String serialInputBuffer = "";
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

                if (oEvent.isRXCHAR()) {
                    try {

                        byte[] buffer;
                        while ((buffer = serialPort.readBytes()) != null) {

                            serialInputBuffer += new String(buffer);

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
                                            listener.onFound(createSerialThing(currPort, typeUid));
                                        }
                                        close();
                                        return;
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
            }

            @Override
            public void close() {
                if (serialPort != null) {
                    try {
                        serialPort.removeEventListener();
                        serialPort.closePort();
                    } catch (SerialPortException e) {
                        LOG.error("error during close input stream", e);
                    }
                    serialPort = null;
                }
            }
        };
    }

    private static void initializePortAccess(SerialPort port, SerialListener listener)
            throws SerialPortException, IOException {

        if (port == null) {
            throw new IOException("initialize: Could not find serial port");
        }

        port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;// Prepare mask
        port.setEventsMask(mask);
        port.addEventListener(listener);

        listener.setSerialPort(port);
    }

    private static SerialThing createSerialThing(SerialPort currPort, ThingTypeUID typeUid) {

        final ThingUID thingUid = new ThingUID(typeUid, currPort.getPortName());

        return new SerialThing() {

            @Override
            public String getPort() {
                return currPort.getPortName();
            }

            @Override
            public String getLabel() {
                // TODO Zugriff auf Label aus thing-types.xml aktuell nicht möglich
                return ("roomsensor".equals(getTypeUID().getId()) ? "Raumsensor" : "Türklingel") + " an Port "
                        + getPort();
            }

            @Override
            public String getId() {
                return /* getTypeUID().getId() + ":" + TODO */currPort.getPortName();
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
    private SerialPort findSerialPort(String serialPortToUse) {

        LOG.info("findSerialPort: "
                + (serialPortToUse != null ? "use specific serial port configured by user: '" + serialPortToUse + "'"
                        : "no special port defined. Try to find right port..."));

        String[] ports = findAllPorts();

        for (String port : ports) {

            SerialPort currPort = new SerialPort(port);

            LOG.info("findSerialPort: port found: " + currPort.getPortName() + ", opened: " + currPort.isOpened());

            if (serialPortToUse != null) {
                if (currPort.getPortName().equals(serialPortToUse)) {
                    LOG.info("findSerialPort: '" + currPort.getPortName() + "' matches port to use. Use it!");
                    return currPort;
                }
            } else {
                for (String portName : PORT_NAMES) {
                    if (currPort.getPortName().equals(portName)) {
                        LOG.info("findSerialPort: '" + currPort.getPortName()
                                + "' matches with a default port. Use it!");
                        return currPort;
                    }
                }
            }
        }
        return null;
    }

    private static String[] findAllPorts() {
        return SerialPortList.getPortNames();
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
        } catch (Exception e) {
            LOG.error("error during requesting current values", e);
        }

    }
}

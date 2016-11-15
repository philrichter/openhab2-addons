package org.openhab.binding.serialthing.handler;

import static org.openhab.binding.serialthing.SerialThingBindingConstants.LINE_DELIMITER;

import java.io.IOException;
import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;

public class SerialListenerImpl implements SerialListener {

    private static final Logger LOG = LoggerFactory.getLogger(SerialListenerImpl.class);

    private final static int TIMEOUT_IDENTIFICATION = 30000;

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

    private String serialInputBuffer = "";
    private SerialPort serialPort = null;

    private long startTime = -1;

    private SerialThingListener listener;

    private Set<ThingTypeUID> supportedThingTypes;

    public SerialListenerImpl(final SerialPort serialPort, final SerialThingListener listener,
            final Set<ThingTypeUID> supportedThingTypes) {
        this.serialPort = serialPort;
        this.listener = listener;
        this.supportedThingTypes = supportedThingTypes;
    }

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
                            serialInputBuffer = serialInputBuffer.substring(command.length() + LINE_DELIMITER.length());

                            if (command.startsWith("TYPEID=")) {

                                ThingTypeUID typeUid = getSupportedTypeUid(parseTypeId(command), supportedThingTypes);

                                if (typeUid != null) {
                                    SerialThing thing = createSerialThing(serialPort, typeUid);
                                    LOG.debug(serialPort.getPortName() + ": typeId = " + thing.getLabel());
                                    listener.onFound(thing);
                                }
                                close();
                                return;
                            } else if (command.startsWith("LOG=")) {
                                LOG.debug(serialPort.getPortName() + ": " + parseLogMessage(command));
                            } else {
                                LOG.debug(serialPort.getPortName() + ": unknown incoming identify event: " + command
                                        + "; currently in queue: " + serialInputBuffer);
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
                // serialPort.removeEventListener();
                serialPort.closePort();
                LOG.debug("close: serial port '" + serialPort.getPortName() + "' after type identification closed");
            } catch (SerialPortException e) {
                LOG.error("error during close input stream", e);
            }
            // serialPort = null;
        }
    }

    private static ThingTypeUID getSupportedTypeUid(String typeId, Set<ThingTypeUID> supportedThingTypes) {
        for (ThingTypeUID thingTypeUID : supportedThingTypes) {
            if (thingTypeUID.getId().equals(typeId)) {
                return thingTypeUID;
            }
        }
        return null;
    }

    private static SerialThing createSerialThing(final SerialPort currPort, final ThingTypeUID typeUid) {

        final ThingUID thingUid = new ThingUID(typeUid, createUid(currPort.getPortName()));

        return new SerialThing() {

            @Override
            public String getPort() {
                return currPort.getPortName();
            }

            @Override
            public String getLabel() {
                // TODO Zugriff auf Label aus thing-types.xml aktuell nicht möglich
                return ("roomsensor".equals(getTypeUID().getId()) ? "Raumsensor" : "Klingel");
            }

            @Override
            public String getId() {
                return getTypeUID().getId();
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

    private static String createUid(final String value) {
        return value.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private static String parseTypeId(String inputLine) {
        return inputLine.substring(inputLine.indexOf("=") + 1);
    }

    private static String parseLogMessage(String inputLine) {
        return inputLine.substring(inputLine.indexOf("=") + 1);
    }
}

package org.openhab.binding.serialroomsensor.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.serialroomsensor.SerialRoomSensorBindingConstants;
import org.openhab.binding.serialroomsensor.handler.SerialPortCommunicator;
import org.openhab.binding.serialroomsensor.handler.SerialPortCommunicator.SerialThing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

public class SerialRoomSensorDiscoveryService extends AbstractDiscoveryService {

    private static Logger LOG = LoggerFactory.getLogger(SerialRoomSensorDiscoveryService.class);

    private final static int INITIAL_DELAY = 5;
    private final static int SCAN_INTERVAL = 6;
    private static final int DISCOVER_TIMEOUT = 30;

    private Map<String, SerialThing> discoveredThings = new HashMap<String, SerialThing>();

    private ScheduledFuture<?> scanningJob;

    public SerialRoomSensorDiscoveryService() throws IllegalArgumentException {
        super(ImmutableSet.of(new ThingTypeUID(SerialRoomSensorBindingConstants.THING_TYPE_STATE.getAsString())),
                DISCOVER_TIMEOUT, true);
        activate(null);
    }

    @Override
    protected void startScan() {
        // scan(); TODO test
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
    }

    /**
     * Starts background scanning for attached devices.
     */
    @Override
    protected void startBackgroundDiscovery() {
        if (scanningJob == null || scanningJob.isCancelled()) {
            scanningJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    LOG.debug("start scanning process...");
                    scan();
                }
            }, INITIAL_DELAY, SCAN_INTERVAL, TimeUnit.SECONDS);
            LOG.debug("discovery service started");
        } else {
            LOG.debug("discovery service active");
        }
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {

        return super.getSupportedThingTypes();
    }

    /**
     * Stops background scanning for attached devices.
     */
    @Override
    protected void stopBackgroundDiscovery() {
        if (scanningJob != null && !scanningJob.isCancelled()) {
            scanningJob.cancel(false);
            scanningJob = null;
            LOG.debug("discovery service stopped");
        }
    }

    private synchronized void scan() {

        Map<String, SerialThing> serialThings = SerialPortCommunicator.searchSerialThings();

        List<SerialThing> thingsToRemove = new ArrayList<SerialThing>();
        for (SerialThing discoveredThing : discoveredThings.values()) {
            if (!serialThings.containsKey(discoveredThing.getId())) {
                thingsToRemove.add(discoveredThing);
            }
        }
        for (SerialThing thing : thingsToRemove) {
            thingRemoved(thing.getUID());
            discoveredThings.remove(thing.getId());
        }

        for (SerialThing thing : serialThings.values()) {
            thingDiscovered(createDiscoveryResult(thing));
            discoveredThings.put(thing.getId(), thing);
        }
        removeOlderResults(getTimestampOfLastScan());
    }

    private DiscoveryResult createDiscoveryResult(SerialThing thing) {
        return DiscoveryResultBuilder.create(thing.getUID()).withLabel(thing.getLabel())
                .withProperty(SerialThing.PORT, thing.getPort())
                .withThingType(SerialRoomSensorBindingConstants.THING_TYPE_STATE).build();
    }
}

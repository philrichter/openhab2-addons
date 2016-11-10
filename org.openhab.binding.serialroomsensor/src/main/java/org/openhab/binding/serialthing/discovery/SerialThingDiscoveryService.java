package org.openhab.binding.serialthing.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.serialthing.SerialThingBindingConstants;
import org.openhab.binding.serialthing.handler.SerialPortCommunicator;
import org.openhab.binding.serialthing.handler.SerialPortCommunicator.SerialThing;
import org.openhab.binding.serialthing.handler.SerialPortCommunicator.SerialThingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialThingDiscoveryService extends AbstractDiscoveryService {

    private static Logger LOG = LoggerFactory.getLogger(SerialThingDiscoveryService.class);

    private final static int INITIAL_DELAY = 15;
    private final static int SCAN_INTERVAL = 10;
    private static final int DISCOVER_TIMEOUT = 10;

    private Map<String, SerialThing> discoveredThings = new HashMap<String, SerialThing>();

    private ScheduledFuture<?> scanningJob;

    public SerialThingDiscoveryService() throws IllegalArgumentException {
        super(SerialThingBindingConstants.SUPPORTED_THING_TYPES, DISCOVER_TIMEOUT, true);
        activate(null);
    }

    @Override
    protected void startScan() {
        scan();
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

        HashMap<String, SerialThing> oldDiscoveredThings = new HashMap<>(discoveredThings);

        LOG.error("scan: oldDiscoveredThings.size = " + oldDiscoveredThings.size() + ", discoveredThings.size = "
                + discoveredThings.size());
        Set<String> serialThings = SerialPortCommunicator.searchSerialThings(getSupportedThingTypes(),
                new SerialThingListener() {

                    @Override
                    public void onFound(SerialThing thing) {
                        thingDiscovered(createDiscoveryResult(thing));
                        discoveredThings.put(thing.getPort(), thing);
                        LOG.debug("thing discovered: " + thing.getThingUID());
                    }

                    @Override
                    public boolean isNew(String port) {
                        boolean isNew = !oldDiscoveredThings.containsKey(port);
                        LOG.debug("isNew: " + port + " = " + isNew);
                        return isNew;
                    }
                });

        for (Entry<String, SerialThing> discoveredThing : oldDiscoveredThings.entrySet()) {
            if (!serialThings.contains(discoveredThing.getKey())) {
                thingRemoved(discoveredThing.getValue().getThingUID());
                discoveredThings.remove(discoveredThing.getKey());
                LOG.debug("thing removed: " + discoveredThing.getValue().getThingUID());
            }
        }
    }

    private DiscoveryResult createDiscoveryResult(SerialThing thing) {
        return DiscoveryResultBuilder.create(thing.getThingUID()).withThingType(thing.getTypeUID())
                .withProperty(SerialThing.PORT, thing.getPort()).withLabel(thing.getLabel()).build();
    }
}

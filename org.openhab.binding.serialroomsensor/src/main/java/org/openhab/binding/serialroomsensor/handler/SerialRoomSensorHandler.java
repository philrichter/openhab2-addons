/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialroomsensor.handler;

import static org.openhab.binding.serialroomsensor.SerialRoomSensorBindingConstants.*;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.serialroomsensor.handler.SerialPortCommunicator.SerialTestHandler;
import org.openhab.binding.serialroomsensor.handler.SerialPortCommunicator.SerialThing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SerialRoomSensorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Philipp - Initial contribution
 */
public class SerialRoomSensorHandler extends BaseThingHandler {

    private static Logger LOG = LoggerFactory.getLogger(SerialRoomSensorHandler.class);

    private SerialPortCommunicator serialPortComm;

    private ScheduledFuture<?> refreshJob;

    private BigDecimal refreshRate;

    public SerialRoomSensorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_REFRESH)) {
            serialPortComm.sendUpdateState();
        }
    }

    @Override
    public void dispose() {
        reset();
        super.dispose();
    }

    private void reset() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
        if (serialPortComm != null) {
            serialPortComm.close();
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.INITIALIZING);

        reset();

        String serialPortToUse = getThing().getProperties().get(SerialThing.PORT); // getConfiguredSerialPort();

        if (serialPortToUse != null) {
            LOG.info("initialize: use specific serial port configured by user: '" + serialPortToUse + "'");
        }
        LOG.info("initialize: use update rate of: " + getRefreshRate());

        serialPortComm = new SerialPortCommunicator(createSerialPortHandler());
        String errorMesssage = serialPortComm.initialize(serialPortToUse);

        if (errorMesssage == null) {
            updateStatus(ThingStatus.ONLINE);
            if (isAutomaticRefreshActivated()) {
                startAutomaticRefresh();
            } else {
                // Request current values for initial thing ui values
                serialPortComm.sendRequestCurrentValues();
            }
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, errorMesssage);
        }
    }

    private SerialTestHandler createSerialPortHandler() {
        return new SerialTestHandler() {

            @Override
            public void onTemperatureChanged(int temperature) {
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE), new DecimalType(temperature));
            }

            @Override
            public void onHumidityChanged(int humidity) {
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_HUMIDITY),
                        new PercentType(new BigDecimal(humidity)));
            }

            @Override
            public void onBrightnessChanged(int brightness) {
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS),
                        new PercentType(new BigDecimal(brightness)));
            }
        };
    }

    private String getConfiguredSerialPort() {
        Object portObj = getThing().getConfiguration().get(PARAM_SERIALPORT);
        if (portObj != null) {
            return (String) portObj;
        }
        return null;
    }

    private BigDecimal getRefreshRate() {

        if (refreshRate == null) {

            try {
                refreshRate = (BigDecimal) getThing().getConfiguration().get(PARAM_REFRESHRATE);
            } catch (Exception e) {
                LOG.debug("Cannot set refresh rate parameter.", e);
            }

            if (refreshRate == null) {
                refreshRate = new BigDecimal(0);
            }
        }
        return refreshRate;
    }

    private void startAutomaticRefresh() {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                serialPortComm.sendUpdateState();
            }
        };

        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, getRefreshRate().intValue(), TimeUnit.SECONDS);
    }

    private boolean isAutomaticRefreshActivated() {
        return getRefreshRate().intValue() > 0;
    }
}

/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialroomsensor.handler;

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.serialroomsensor.SerialRoomSensorBindingConstants;
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

    private static final long CURRENTVALUES_DELAY = 2;

    private static Logger LOG = LoggerFactory.getLogger(SerialRoomSensorHandler.class);

    private SerialPortCommunicator serialPortComm;

    private ScheduledFuture<?> refreshJob;

    public SerialRoomSensorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

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

        String serialPortToUse = getThing().getProperties().get(SerialThing.PORT);

        if (serialPortToUse != null) {
            LOG.info("initialize: use specific serial port configured by user: '" + serialPortToUse + "'");
        }

        try {
            serialPortComm = new SerialPortCommunicator(createSerialPortHandler());
            serialPortComm.initialize(serialPortToUse);

            updateStatus(ThingStatus.ONLINE);
            requestInitialThingValues();
        } catch (Exception e) {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, e.getMessage());
        }
    }

    private SerialTestHandler createSerialPortHandler() {
        return new SerialTestHandler() {

            @Override
            public void onTemperatureChanged(int temperature) {
                updateState(new ChannelUID(getThing().getUID(), SerialRoomSensorBindingConstants.CHANNEL_TEMPERATURE),
                        new DecimalType(temperature));
            }

            @Override
            public void onHumidityChanged(int humidity) {
                updateState(new ChannelUID(getThing().getUID(), SerialRoomSensorBindingConstants.CHANNEL_HUMIDITY),
                        new PercentType(new BigDecimal(humidity)));
            }

            @Override
            public void onBrightnessChanged(int brightness) {
                updateState(new ChannelUID(getThing().getUID(), SerialRoomSensorBindingConstants.CHANNEL_BRIGHTNESS),
                        new PercentType(new BigDecimal(brightness)));
            }

            @Override
            public void onDoorbellPressed(boolean pressed) {
                updateState(new ChannelUID(getThing().getUID(), SerialRoomSensorBindingConstants.CHANNEL_DOORBELL),
                        pressed ? OnOffType.ON : OnOffType.OFF);
            }
        };
    }

    private void requestInitialThingValues() {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                serialPortComm.sendRequestCurrentValues();
            }
        };

        refreshJob = scheduler.schedule(runnable, CURRENTVALUES_DELAY, TimeUnit.SECONDS);
    }
}

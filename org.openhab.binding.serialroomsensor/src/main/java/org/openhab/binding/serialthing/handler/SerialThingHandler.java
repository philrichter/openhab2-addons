/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialthing.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.serialthing.SerialThingBindingConstants;
import org.openhab.binding.serialthing.handler.SerialListenerImpl.SerialThing;
import org.openhab.binding.serialthing.handler.SerialPortCommunicator.SerialTestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SerialThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Philipp - Initial contribution
 */
public class SerialThingHandler extends BaseThingHandler {

    private static final long CURRENTVALUES_DELAY = 2;

    private static Logger LOG = LoggerFactory.getLogger(SerialThingHandler.class);

    private SerialPortCommunicator serialPortComm;

    private ScheduledFuture<?> refreshJob;

    public SerialThingHandler(Thing thing) {
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
            serialPortComm = null;
        }
    }

    @Override
    public void initialize() {

        updateStatus(ThingStatus.INITIALIZING);

        reset();

        String serialPortToUse = getThing().getProperties().get(SerialThing.PORT);

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
                updateState(new ChannelUID(getThing().getUID(), SerialThingBindingConstants.CHANNEL_TEMPERATURE),
                        new DecimalType(temperature));
            }

            @Override
            public void onHumidityChanged(int humidity) {
                updateState(new ChannelUID(getThing().getUID(), SerialThingBindingConstants.CHANNEL_HUMIDITY),
                        new DecimalType(humidity));
            }

            @Override
            public void onBrightnessChanged(int brightness) {
                updateState(new ChannelUID(getThing().getUID(), SerialThingBindingConstants.CHANNEL_BRIGHTNESS),
                        new DecimalType(brightness));
            }

            @Override
            public void onDoorbellPressed(boolean pressed) {
                updateState(new ChannelUID(getThing().getUID(), SerialThingBindingConstants.CHANNEL_DOORBELL),
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

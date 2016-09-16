/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.philtest.handler;

import static org.openhab.binding.philtest.PhilTestBindingConstants.*;

import java.math.BigDecimal;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PhilTestHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Philipp - Initial contribution
 */
public class PhilTestHandler extends BaseThingHandler implements SerialTestHandler {

    private Logger logger = LoggerFactory.getLogger(PhilTestHandler.class);
    // private SerialPort portToUse = null;
    private SerialPortCommunicator serialPortComm;

    public PhilTestHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_1)) {
            if (command instanceof OnOffType) {
                OnOffType onOffType = (OnOffType) command;
                switch (onOffType) {
                    case ON:
                        serialPortComm.switchON();
                        break;
                    case OFF:
                        serialPortComm.switchOFF();
                        break;
                }
            }

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        }
    }

    @Override
    public void dispose() {
        serialPortComm.close();
        super.dispose();
    }

    @Override
    public void initialize() {

        updateStatus(ThingStatus.INITIALIZING);

        String serialPortToUse = getConfiguredSerialPort();

        if (serialPortToUse != null) {
            logger.info("initialize: use specific serial port configured by user: '" + serialPortToUse + "'");
        }

        serialPortComm = new SerialPortCommunicator(this);
        String errorMesssage = serialPortComm.initialize(serialPortToUse);

        if (errorMesssage == null) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, errorMesssage);
        }

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    private String getConfiguredSerialPort() {
        Object portObj = getThing().getConfiguration().get(PARAM_SERIALPORT);
        if (portObj != null) {
            return (String) portObj;
        }
        return null;
    }

    @Override
    public void onBrightnessChanged(int brightness) {
        updateState(new ChannelUID(getThing().getUID(), CHANNEL_BRIGHTNESS),
                new PercentType(new BigDecimal(brightness / 1024d * 100)));
    }

    @Override
    public void onLedOnOffStateChanged(boolean on) {
        updateState(new ChannelUID(getThing().getUID(), CHANNEL_LEDONOFFSTATE), on ? OnOffType.ON : OnOffType.OFF);
    }
}

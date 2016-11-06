/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialroomsensor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link SerialRoomSensorBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Philipp - Initial contribution
 */
public class SerialRoomSensorBindingConstants {

    public static final String BINDING_ID = "serialroomsensor";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_ROOMSENSOR = new ThingTypeUID(BINDING_ID, "roomsensor");
    public final static ThingTypeUID THING_TYPE_DOORBELL = new ThingTypeUID(BINDING_ID, "doorbell");

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<ThingTypeUID>();

    static {
        SUPPORTED_THING_TYPES.add(THING_TYPE_ROOMSENSOR);
        SUPPORTED_THING_TYPES.add(THING_TYPE_DOORBELL);
    }

    // Room sensor
    public final static String CHANNEL_BRIGHTNESS = "brightness";
    public final static String CHANNEL_TEMPERATURE = "temperature";
    public final static String CHANNEL_HUMIDITY = "humidity";

    // Door bell
    public final static String CHANNEL_DOORBELL = "doorbell";

    public final static String PARAM_SERIALPORT = "serialport";
    public final static String PARAM_REFRESHRATE = "refreshrate";
}

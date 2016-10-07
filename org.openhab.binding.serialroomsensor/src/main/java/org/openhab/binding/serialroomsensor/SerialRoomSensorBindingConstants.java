/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialroomsensor;

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
    public final static ThingTypeUID THING_TYPE_STATE = new ThingTypeUID(BINDING_ID, "roomsensorstate");

    // List of all Channel ids
    public final static String CHANNEL_BRIGHTNESS = "brightness";
    public final static String CHANNEL_TEMPERATURE = "temperature";
    public final static String CHANNEL_HUMIDITY = "humidity";
    public final static String CHANNEL_REFRESH = "refresh";

    public final static String PARAM_SERIALPORT = "serialport";
    public final static String PARAM_REFRESHRATE = "refreshrate";
}

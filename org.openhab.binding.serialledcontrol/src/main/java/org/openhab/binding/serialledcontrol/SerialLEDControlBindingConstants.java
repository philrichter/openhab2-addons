/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialledcontrol;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link SerialLEDControlBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Phil - Initial contribution
 */
public class SerialLEDControlBindingConstants {

    public static final String BINDING_ID = "serialledcontrol";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_SAMPLE = new ThingTypeUID(BINDING_ID, "sample2");

    // List of all Channel ids
    public final static String CHANNEL_LEDONOFFSWITCH = "ledonoffswitch";

    public final static String PARAM_SERIALPORT = "serialport";

}

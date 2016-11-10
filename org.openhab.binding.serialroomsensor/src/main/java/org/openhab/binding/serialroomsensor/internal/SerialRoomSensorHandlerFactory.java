/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialroomsensor.internal;

import static org.openhab.binding.serialroomsensor.SerialRoomSensorBindingConstants.*;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.serialroomsensor.handler.SerialRoomSensorHandler;

/**
 * The {@link SerialRoomSensorHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Philipp - Initial contribution
 */
public class SerialRoomSensorHandlerFactory extends BaseThingHandlerFactory {

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_ROOMSENSOR)) {
            return new SerialRoomSensorHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_DOORBELL)) {
            return new SerialRoomSensorHandler(thing);
        }

        return null;
    }
}

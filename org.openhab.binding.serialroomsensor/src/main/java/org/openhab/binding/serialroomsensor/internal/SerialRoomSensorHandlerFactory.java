/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serialroomsensor.internal;

import static org.openhab.binding.serialroomsensor.SerialRoomSensorBindingConstants.*;

import java.util.Collections;
import java.util.Set;

import org.openhab.binding.serialroomsensor.handler.SerialRoomSensorHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

/**
 * The {@link SerialRoomSensorHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Philipp - Initial contribution
 */
public class SerialRoomSensorHandlerFactory extends BaseThingHandlerFactory {
    
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_STATE);
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_STATE)) {
            return new SerialRoomSensorHandler(thing);
        }

        return null;
    }
}


/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver.internal;

import static org.openhab.binding.ihcserver.IHCServerBindingConstants.*;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.ihcserver.handler.IHCServerBridgeHandler;
import org.openhab.binding.ihcserver.handler.IHCServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul Dhaene
 */
public class IHCServerHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_DEVICE_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return new IHCServerHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_BRIDGE_ETH)) {
            return new IHCServerBridgeHandler((Bridge) thing);
        } else {
            logger.error("Thing {} cannot be configured, is this thing supported by the binding?", thingTypeUID);
        }

        return null;
    }
}

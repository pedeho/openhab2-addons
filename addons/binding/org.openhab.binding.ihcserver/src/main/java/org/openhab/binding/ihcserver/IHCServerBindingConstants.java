/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * @author Paul Dhaene
 */
public class IHCServerBindingConstants {

    public static final String BINDING_ID = "ihcserver";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_INPUT = new ThingTypeUID("ihcserver", "input");
    public static final ThingTypeUID THING_TYPE_OUTPUT = new ThingTypeUID("ihcserver", "output");

    // List of all Channel ids
    public static final String SWITCH_CHANNEL = "switch";
    public static final String STATE_CHANNEL = "state";
    //public static final List<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new ArrayList();

    // bridge
    public static final ThingTypeUID THING_TYPE_BRIDGE_ETH = new ThingTypeUID(BINDING_ID, "bridge-eth");

//  static
//  {
//    SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_INPUT);
//    SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_OUTPUT);
//  }
    /**
     * Supported Things without bridge
     */
    public static final List<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new ArrayList<>();

    static {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_INPUT);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_OUTPUT);
    }

    public static final List<ThingTypeUID> SUPPORTED_DEVICE_TYPES_UIDS = new ArrayList<>();
    
    static{
        SUPPORTED_DEVICE_TYPES_UIDS.add(THING_TYPE_BRIDGE_ETH);
        SUPPORTED_DEVICE_TYPES_UIDS.add(THING_TYPE_INPUT);
        SUPPORTED_DEVICE_TYPES_UIDS.add(THING_TYPE_OUTPUT);
    }
           
    public final static String INPUTS = "inputs";
    public final static String OUTPUTS = "outputs";

}

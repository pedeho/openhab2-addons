/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver.handler;

import java.io.IOException;
import static org.openhab.binding.ihcserver.IHCServerBindingConstants.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.ihcserver.Client;
import org.openhab.binding.ihcserver.Tasks;

import org.openhab.binding.ihcserver.config.IHCServerBridgeConfiguration;
import org.openhab.binding.ihcserver.config.IHCServerIOConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IHCServerBridgeHandler connects to and communicates with ihcserver (Martin
 * Hejnfelt (martin@hejnfelt.com)) via it's web interface.
 *
 * @author Paul Dhaene
 *
 */
public class IHCServerBridgeHandler extends BaseBridgeHandler {

    /**
     * state update poll rate in seconds
     */
    private final int refresh = 2;
    /**
     * Future to poll for updated
     */
    private ScheduledFuture<?> pollFuture;
    /**
     * Task service executes state update, input & output commands
     */
    private final Tasks tasks = new Tasks();
    /**
     * is our config correct
     */
    private boolean properlyConfigured;

    private final Logger logger = LoggerFactory.getLogger(IHCServerBridgeHandler.class);
    private final Map<Integer, Map<Integer, Integer>> outputStates = new HashMap<>();

    private Client client;
    private IHCServerBridgeConfiguration myBridgeConfiguration;

    public IHCServerBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Initialization of the IHCServer bridge");

        myBridgeConfiguration = getConfigAs(IHCServerBridgeConfiguration.class);

        properlyConfigured = true;

        client = new Client(myBridgeConfiguration.url,
                myBridgeConfiguration.tcpPort);
        if (client.login(myBridgeConfiguration.password)) {
            logger.debug("Initialization of the IHCServer bridge DONE!");
        } else {
            logger.error("Failed to initialize IHCServer bridge");
            return;
        }

        tasks.initialize(this, client);
        initPolling();

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void dispose() {
        stopPolling();
        tasks.shutdown();
        outputStates.clear();
        super.dispose();
    }

    public void setOutput(int moduleId, int ioId, boolean state) {
        tasks.executeOutputTask(moduleId, ioId, state);
    }

    public void activateInput(int moduleId, int ioId,
            int stateModId, int stateIoId, boolean state) {
        tasks.executeInputTask(moduleId, ioId, stateModId, stateIoId, state);
    }

    public IHCServerBridgeConfiguration getConfiguration() {
        return myBridgeConfiguration;
    }

    /**
     * starts this things polling future
     */
    private void initPolling() {
        stopPolling();
        pollFuture = scheduler.scheduleAtFixedRate(() -> {
            if (properlyConfigured) {
                tasks.executeStateTask();
            }
        }, 0, refresh, TimeUnit.SECONDS);
    }

    /**
     * Stops this thing's polling future
     */
    private void stopPolling() {
        if (pollFuture != null && !pollFuture.isCancelled()) {
            pollFuture.cancel(true);
            pollFuture = null;
        }
    }

    public synchronized void updateStates() throws IOException {
        client.mapAllStates(outputStates);

        Collection<Thing> allThings = getThing().getThings();
        for (Thing aThing : allThings) {
            String thingType = aThing.getThingTypeUID().getId();
            String bindingId = aThing.getThingTypeUID().getBindingId();
            IHCServerHandler handler = (IHCServerHandler) aThing.getHandler();
            if (handler == null) {
                continue;
            }
            IHCServerIOConfiguration conf = handler.getConfiguration();
            if (thingType.equals(THING_TYPE_OUTPUT.getId())) {
                for (Channel channel : aThing.getChannels()) {
                    Integer newState = outputStates.get(conf.module).get(conf.output);

                    State state = null;
                    switch (channel.getUID().getId()) {
                        case SWITCH_CHANNEL:
                            state = toState("Switch", newState.toString());
                            break;
                        case STATE_CHANNEL:
                            state = toState("Number", newState.toString());
                            break;
                    }
                    updateState(channel.getUID(), state);
                }
            } else if (thingType.equals(THING_TYPE_INPUT.getId())) {
                aThing.getChannels().stream().filter((channel) -> (conf.stateModule > 0
                        && conf.stateOutput > 0)).forEachOrdered((channel) -> {
                    Integer newState = outputStates.get(conf.stateModule).get(conf.stateOutput);
                    updateState(channel.getUID(), toState("Switch", newState.toString()));
                });
            }
        }
    }

    /**
     * Converts a String value to State a for a given type
     *
     * @param type
     * @param value
     * @return State
     */
    public State toState(String type, String value) throws NumberFormatException {
        switch (type) {
            case "Number":
                return new DecimalType(value);
            case "Switch":
                return Integer.parseInt(value) > 0 ? OnOffType.ON : OnOffType.OFF;
            default:
                return StringType.valueOf(value);
        }
    }

    @Override
    public void handleCommand(ChannelUID cuid, Command cmnd) {
    }
}

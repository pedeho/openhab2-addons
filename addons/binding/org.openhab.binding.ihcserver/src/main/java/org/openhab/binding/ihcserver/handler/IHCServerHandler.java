/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver.handler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static org.openhab.binding.ihcserver.IHCServerBindingConstants.*;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.ihcserver.config.IHCServerIOConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul Dhaene
 */
public class IHCServerHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(IHCServerHandler.class);
    private IHCServerIOConfiguration conf;
    private IHCServerBridgeHandler bridgeHandler;
    private boolean isInitialized;

    public IHCServerHandler(@NonNull Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            return;
        }

        bridgeHandler = getBridgeHandler();
        if (getThing().getThingTypeUID().getId().equals(THING_TYPE_OUTPUT.getId())) {
            switch (channelUID.getId()) {
                case SWITCH_CHANNEL:
                    bridgeHandler.setOutput(conf.module, conf.output,
                            (command == OnOffType.ON));
                    break;
                default:
            }

        } else { // input         
            switch (channelUID.getId()) {
                case SWITCH_CHANNEL:
                    if(conf.button){
                        bridgeHandler.activateInput(conf.module, conf.input,
                            0, 0, command==OnOffType.ON);
                        delayedUpdateState(channelUID, (State) OnOffType.OFF, 500);
                    }else{
                        bridgeHandler.activateInput(conf.module, conf.input,
                            conf.stateModule, conf.stateOutput, command == OnOffType.ON);
                    }
                    break;
            }
        }
    }

    @Override
    public void initialize() {
        conf = getConfigAs(IHCServerIOConfiguration.class);

        updateStatus(ThingStatus.ONLINE);

        bridgeHandler = getBridgeHandler();

        if (bridgeHandler != null) {
            ThingStatusInfo bridgeStatusInfo = bridgeHandler.getThing().getStatusInfo();
            if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE || bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
                updateStatus(bridgeStatusInfo.getStatus());
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
            return;
        }
        isInitialized = true;
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE
                || bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(bridgeStatusInfo.getStatus());
        }
    }

    private synchronized IHCServerBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        IHCServerBridgeHandler myBridgeHandler = (IHCServerBridgeHandler) bridge.getHandler();

        return myBridgeHandler;
    }

    private void delayedUpdateState(final ChannelUID channelUID, final State state, final int delay) {
        ScheduledExecutorService updateStateScheduler
                = Executors.newSingleThreadScheduledExecutor();

        Runnable task = () -> {
            updateState(channelUID, state);
        };

        updateStateScheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
        updateStateScheduler.shutdown();
    }

    IHCServerIOConfiguration getConfiguration() {
        return conf;
    }

}

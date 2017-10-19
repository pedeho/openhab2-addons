/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.openhab.binding.ihcserver.handler.IHCServerBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul Dhaene
 */
public class Tasks {

    private final Logger logger = LoggerFactory.getLogger(Tasks.class);
    private final static int DELAY = 10; // milliseconds
    private ExecutorService executor;
    private IHCServerBridgeHandler handler;
    private Client client;

    public void initialize(IHCServerBridgeHandler handler, Client client) {
        this.handler = handler;
        this.client = client;
        executor = Executors.newFixedThreadPool(1);
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    public void executeStateTask() {
        executor.execute(new StateTask());
    }

    public void executeInputTask(int module, int io, int stateModule, int stateIO, boolean state) {
        executor.execute(new InputCmdTask(module, io, stateModule, stateIO, state));
    }

    public void executeOutputTask(int module, int io, boolean state) {
        executor.execute(new OutputCmdTask(module, io, state));
    }

    private class StateTask implements Runnable {

        @Override
        public void run() {
            try {
                handler.updateStates();
                TimeUnit.MILLISECONDS.sleep(DELAY);
            } catch (IOException | InterruptedException ex) {
                logger.error("Exception during state poll : {}", ex);
            }
        }
    }

    private class InputCmdTask implements Runnable {

        private final int module;
        private final int io;
        private final int stateModule;
        private final int stateIO;
        private final boolean state;

        public InputCmdTask(int module, int io, int stateModule, int stateIO, boolean state) {
            this.module = module;
            this.io = io;
            this.stateModule = stateModule;
            this.stateIO = stateIO;
            this.state = state;
        }

        @Override
        public void run() {
            client.activateInput(module, io, stateModule, stateIO, state);
            try {
                TimeUnit.MILLISECONDS.sleep(DELAY);
            } catch (InterruptedException ex) {
            }
        }
    }

    private class OutputCmdTask implements Runnable {

        private final int module;
        private final int io;
        private final boolean state;

        public OutputCmdTask(int module, int io, boolean state) {
            this.module = module;
            this.io = io;
            this.state = state;
        }

        @Override
        public void run() {
            client.setOutput(module, io, state);
            try {
                TimeUnit.MILLISECONDS.sleep(DELAY);
            } catch (InterruptedException ex) {
            }
        }
    }
}

/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver;

import static org.openhab.binding.ihcserver.Helpers.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paul Dhaene
 */
public class Client {

    private class LoginResponse {

        public String Userlevel; //BASIC
        public boolean result;   //true = accepted
        
        public boolean isValid(){
            return Userlevel.equals("BASIC") && result;
        }
    }

    private class Login {

        private final String id = UUID;
        private final String type = "keypadAction";
        private final String action = "login";
        private final String input;

        public Login(String password) {
            this.input = password;
        }
    }

    private class GetAllRequest {

        private final String id = UUID;
        private final String type = "getAll";
    }

    private class ActivateInput {

        private final String id = UUID;
        private final int moduleNumber;
        private final int ioNumber;
        private final String type;

        public ActivateInput(int moduleNumber, int ioNumber, boolean state) {
            this.moduleNumber = moduleNumber;
            this.ioNumber = ioNumber;
            this.type = state ? "activateInput" : "deactivateInput";
        }
    }

    private class ToggleOutput {

        private final String id = UUID;
        private final int moduleNumber;
        private final int ioNumber;
        private final String type = "toggleOutput";

        public ToggleOutput(int moduleNumber, int ioNumber) {
            this.moduleNumber = moduleNumber;
            this.ioNumber = ioNumber;
        }
    }

    private final static Logger logger = LoggerFactory.getLogger(Client.class);
    private static final int TIMEOUT = 5000;
    private final static String UUID = randomUUID();
    private final Map<Integer, Map<Integer, Integer>> currStates;
    private URL url;
    private final Gson gson;

    public Client(String someurl, int port) {
        this.currStates = new HashMap<>();
        try {
            url = new URL(someurl + ":" + port + "/ihcrequest");
        } catch (MalformedURLException ex) {
            logger.error("MalformedURLException: {}", ex.getMessage());
        }
        gson = new Gson();
    }

    public boolean login(String password) {
        Login login = new Login(password);
        String json = gson.toJson(login);
        String resp = postCommand(json);
        LoginResponse lresp = gson.fromJson(resp, LoginResponse.class);
        return lresp.result;
    }

    public void activateInput(int moduleNumber, int ioNumber,
            int stateModId, int stateIoId, boolean state) {
        if (stateModId == 0 && stateIoId == 0) {
            currStates.clear();
        }

        ActivateInput activateInput = new ActivateInput(moduleNumber, ioNumber, state);

        if (!currStates.containsKey(stateModId)) {
            currStates.put(stateModId, new HashMap<>());
        }
        Map<Integer, Integer> mod = currStates.get(stateModId);
        mod.put(stateIoId, state ? 1 : 0);

        postCommand(gson.toJson(activateInput));
    }

    public void setOutput(int moduleNumber, int ioNumber, boolean state) {
        Boolean currentState = getOutputState(moduleNumber, ioNumber);
        if (currentState == null) {
            logger.error("setOutput error: Module/Output not found!");
            return;    
        }
        ToggleOutput toggle = new ToggleOutput(moduleNumber, ioNumber);

        if (!currStates.containsKey(moduleNumber)) {
            currStates.put(moduleNumber, new HashMap<>());
        }
        Map<Integer, Integer> mod = currStates.get(moduleNumber);
        mod.put(ioNumber, state ? 1 : 0);

        postCommand(gson.toJson(toggle));
    }

    public Boolean getOutputState(int module, int io) {
        String json = getStates();
        JsonObject root = gson.fromJson(json, JsonObject.class);
        JsonObject modules = root.getAsJsonObject("modules");
        JsonArray outputModules = modules.getAsJsonArray("outputModules");
        for (JsonElement e : outputModules) {
            JsonObject outputModule = e.getAsJsonObject();
            boolean state = outputModule.get("state").getAsBoolean();
            if (state) {
                int moduleNumber = outputModule.get("moduleNumber").getAsInt();
                JsonArray outputArray = outputModule.getAsJsonArray("outputStates");
                for (JsonElement outputElm : outputArray) {
                    JsonObject output = outputElm.getAsJsonObject();
                    int outputNumber = output.get("outputNumber").getAsInt();
                    boolean outputState = output.get("outputState").getAsBoolean();
                    if (module == moduleNumber && io == outputNumber) {
                        return outputState;
                    }
                }
            }
        }
        return null;
    }

    private synchronized String getStates() {
        String json = gson.toJson(new GetAllRequest());
        return postCommand(json);
    }

    public void mapAllStates(Map<Integer, Map<Integer, Integer>> outMap) throws IOException {
        String states = getStates();

        JsonObject root = gson.fromJson(states, JsonObject.class);
        JsonObject modules = root.getAsJsonObject("modules");
        JsonArray outputModules = modules.getAsJsonArray("outputModules");
        outputsToMap(outputModules, outMap);
    }

    private void outputsToMap(JsonArray array, Map<Integer, Map<Integer, Integer>> map) {
        for (JsonElement e : array) {
            JsonObject outputModule = e.getAsJsonObject();
            if (outputModule.get("state").getAsBoolean()) {//add only active modules
                Map<Integer, Integer> moduleMap = new HashMap<>();
                int moduleNumber = outputModule.get("moduleNumber").getAsInt();
                map.put(moduleNumber, moduleMap);
                JsonArray inputArray = outputModule.getAsJsonArray("outputStates");
                for (JsonElement outputElm : inputArray) {
                    JsonObject output = outputElm.getAsJsonObject();
                    int outputNumber = output.get("outputNumber").getAsInt();
                    int outputState = output.get("outputState").getAsBoolean() ? 1 : 0;
                    // check if module & output exists in currStates
                    Map<Integer, Integer> mod = currStates.get(moduleNumber);
                    if (mod != null && mod.containsKey(outputNumber)) {
                        Integer state = mod.get(outputNumber);
                        map.get(moduleNumber).put(outputNumber, state);
                    } else {
                        map.get(moduleNumber).put(outputNumber, outputState);
                    }
                }
            }
        }
    }

    private String postCommand(String json) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestMethod("POST");

            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(json);
            wr.flush();

            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                return sb.toString();
            } else {
                logger.error("IHC request failed: request:\n{}\ncode:{} msg:{}",
                        json,con.getResponseCode(), con.getResponseMessage());
                return null;
            }
        } catch (IOException ex) {
            logger.error("Could not connect to url:{}", url);
        }
        return null;
    }
}

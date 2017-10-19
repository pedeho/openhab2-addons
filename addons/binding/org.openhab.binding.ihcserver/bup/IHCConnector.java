package ihcclient;

import static ihcclient.Helpers.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IHCConnector
{
  static final int TIMEOUT = 5000;
  private final String url;
  private final String uuid;
  private final String getAllRequest;
  private static final Logger logger = LoggerFactory.getLogger(IHCConnector.class);
  private final URL urlObj;

  public IHCConnector(String url, int port, String password)
    throws MalformedURLException
  {
    this.url = (url + ":" + port + "/ihcrequest");
    this.urlObj = new URL(this.url);
    this.uuid = randomUUID();
    JSONObject jobj = new JSONObject();
    jobj.put("id", this.uuid);
    jobj.put("type", "getAll");
    this.getAllRequest = jobj.toString();
    if (Helpers.validateAdminCode(password))
      login(password);
    else
      logger.warn("IHCServer login fail: invalid admin code!");
  }

  public final void login(String password)
  {
    JSONObject jobj = new JSONObject();
    jobj.put("id", this.uuid);
    jobj.put("type", "keypadAction");
    jobj.put("action", "login");
    jobj.put("input", encryptPassword(password));
  }

  protected final String getJsonInputString(int moduleNumber, int ioNumber, boolean state)
  {
    JSONObject jobj = new JSONObject();
    jobj.put("id", this.uuid);
    jobj.put("ioNumber", ioNumber);
    jobj.put("moduleNumber", moduleNumber);
    jobj.put("type", state ? "activateInput" : "deactivateInput");
    String request = jobj.toString();

    return request;
  }

  protected final String getJsonOutputString(int moduleNumber, int ioNumber, boolean state)
  {
    JSONObject jobj = new JSONObject();
    jobj.put("id", this.uuid);
    jobj.put("ioNumber", ioNumber);
    jobj.put("moduleNumber", moduleNumber);
    jobj.put("state", state);
    jobj.put("type", "setOutputState");
    String request = jobj.toString();

    return request;
  }

  public String activateInput(int moduleNumber, int ioNumber, boolean state) throws IOException {
    String request = getJsonInputString(moduleNumber, ioNumber, state);
    String s = sendPost(request, 5000);
    return s;
  }

  public String setOutput(int moduleNumber, int ioNumber, boolean state) throws IOException {
    String request = getJsonOutputString(moduleNumber, ioNumber, state);
    return sendPost(request, 5000);
  }
  
  public String toggleOutput(int moduleNumber,int ioNumber){
      JSONObject jobj = new JSONObject();
    jobj.put("id", this.uuid);
    jobj.put("ioNumber", ioNumber);
    jobj.put("moduleNumber", moduleNumber);
    jobj.put("type", "toggleOutput");
    String request = jobj.toString();
    return sendPost(request, 5000);
  }

  public synchronized void mapAllStates(Map<Integer, Map<Integer, Integer>> inMap, Map<Integer, Map<Integer, Integer>> outMap) throws IOException
  {
    String state = getState();

    JSONObject obj = new JSONObject(state);
    JSONObject modules = obj.getJSONObject("modules");
    JSONArray inmods = modules.getJSONArray("inputModules");
    inputsToMap(inmods, inMap);
    JSONArray outmods = modules.getJSONArray("outputModules");
    outputsToMap(outmods, outMap);
  }

  private static void inputsToMap(JSONArray arr, Map<Integer, Map<Integer, Integer>> map) {
    int n = arr.length();
    for (int i = 0; i < n; i++) {
      JSONObject module = arr.getJSONObject(i);
      if (module.getBoolean("state")) {
        int moduleNumber = module.getInt("moduleNumber");
        Map moduleMap = new HashMap();
        map.put(Integer.valueOf(moduleNumber), moduleMap);
        JSONArray inputStates = module.getJSONArray("inputStates");
        for (int j = 0; j < inputStates.length(); j++) {
          JSONObject inputState = inputStates.getJSONObject(j);
          int inputNumber = inputState.getInt("inputNumber");
          int state = inputState.getBoolean("inputState") ? 1 : 0;
          ((Map)map.get(Integer.valueOf(moduleNumber))).put(Integer.valueOf(inputNumber), Integer.valueOf(state));
        }
      }
    }
  }

  private static void outputsToMap(JSONArray arr, Map<Integer, Map<Integer, Integer>> map) {
    int n = arr.length();
    for (int i = 0; i < n; i++) {
      JSONObject module = arr.getJSONObject(i);
      if (module.getBoolean("state")) {
        int moduleNumber = module.getInt("moduleNumber");
        Map moduleMap = new HashMap();
        map.put(Integer.valueOf(moduleNumber), moduleMap);
        JSONArray outputStates = module.getJSONArray("outputStates");
        for (int j = 0; j < outputStates.length(); j++) {
          JSONObject outputState = outputStates.getJSONObject(j);
          int outputNumber = outputState.getInt("outputNumber");
          int state = outputState.getBoolean("outputState") ? 1 : 0;
          ((Map)map.get(Integer.valueOf(moduleNumber))).put(Integer.valueOf(outputNumber), Integer.valueOf(state));
        }
      }
    }
  }

  protected final String getState() throws IOException {
    return sendPost(this.getAllRequest, 5000);
  }

  private String sendPost(String json, int timeout){
        try {
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setReadTimeout(timeout);
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
                logger.debug("Method failed: {}", con.getResponseCode() + " " + con.getResponseMessage());
		return null;
            }
        } catch (IOException ex) {
            logger.debug("Could not make http connection", ex);
        }
        return null;
    }

  
}
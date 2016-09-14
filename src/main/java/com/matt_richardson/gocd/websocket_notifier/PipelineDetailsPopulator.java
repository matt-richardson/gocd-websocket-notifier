package com.matt_richardson.gocd.websocket_notifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class PipelineDetailsPopulator {
    private static Logger LOGGER = Logger.getLoggerFor(GoNotificationPlugin.class);
    private int httpPort;
	
    public PipelineDetailsPopulator() {
        this.httpPort = 8443;
    }

    public PipelineDetailsPopulator(int httpPort) {
        this.httpPort = httpPort;
    }

    String mergeInPipelineInstanceDetails(JsonElement notification, JsonElement pipelineInstance)
    {
        JsonObject json = notification.getAsJsonObject();
        json.add("x-pipeline-instance-details", pipelineInstance);
        return json.toString();
    }

    JsonElement downloadPipelineInstanceDetails(String pipelineName) throws IOException {
    	javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
    			new javax.net.ssl.HostnameVerifier(){
    				
    				public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
    					if (hostname.equals("localhost")) {
    						return true;
    					}
    					return false;
    				}
    			});
    	
    	String sURL = "https://localhost:" + httpPort + "/go/api/pipelines/" + pipelineName + "/history";

        URL url = new URL(sURL);
        HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
        request.connect();

        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(new InputStreamReader((InputStream) request.getContent()));
        JsonObject json = rootElement.getAsJsonObject();
        JsonArray pipelines = json.get("pipelines").getAsJsonArray();
        return pipelines.get(0);
    }

    public String extendMessageToIncludePipelineDetails(String requestBody) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(requestBody).getAsJsonObject();

        String result = requestBody;
        try {
            JsonElement pipelineName = json.get("pipeline-name"); //GoCD 15.1
            if (null == pipelineName) {
                //GoCD 15.2
                JsonObject pipeline = json.get("pipeline").getAsJsonObject();
                pipelineName = pipeline.get("name");
            }
            JsonElement extraDetails = downloadPipelineInstanceDetails(pipelineName.getAsString());
            result = mergeInPipelineInstanceDetails(json, extraDetails);
        } catch (IOException e) {
            LOGGER.error("Failed to download pipeline instance details for requestBody '" + requestBody + "'", e);
        }
        return result;
    }
}

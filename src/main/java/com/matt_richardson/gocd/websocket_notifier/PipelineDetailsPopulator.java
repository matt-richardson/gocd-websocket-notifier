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

import org.apache.commons.codec.binary.Base64;

public class PipelineDetailsPopulator {
    private static Logger LOGGER = Logger.getLoggerFor(GoNotificationPlugin.class);
    private static PluginConfig pluginConfig;

    public PipelineDetailsPopulator(PluginConfig pluginConfig) {
        PipelineDetailsPopulator.pluginConfig = pluginConfig;
    }

    String mergeInPipelineInstanceDetails(JsonElement notification, JsonElement pipelineInstance)
    {
        JsonObject json = notification.getAsJsonObject();
        json.add("x-pipeline-instance-details", pipelineInstance);
        return json.toString();
    }

    JsonElement downloadPipelineInstanceDetails(String pipelineName) throws IOException {
    	int goHttpPort = pluginConfig.getGoHttpPort();
        String sURL = "http://localhost:" + goHttpPort + "/go/api/pipelines/" + pipelineName + "/history";

        URL url = new URL(sURL);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        if (pluginConfig.hasBasicAuth()) {
        	String user = pluginConfig.getGoUser();
        	String password = pluginConfig.getGoPassword();
        	
        	String authString = user + ":" + password;
            String encoded = new String(Base64.encodeBase64(authString.getBytes()));
            request.setRequestProperty("Authorization", "Basic "+encoded);
        }
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

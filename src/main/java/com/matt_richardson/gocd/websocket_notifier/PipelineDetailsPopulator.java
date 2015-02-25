package com.matt_richardson.gocd.websocket_notifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PipelineDetailsPopulator {
    String mergeInPipelineInstanceDetails(JsonElement notification, JsonElement pipelineInstance)
    {
        JsonObject json = notification.getAsJsonObject();
        json.add("x-pipeline-instance-details", pipelineInstance);
        return json.toString();
    }

    JsonElement downloadPipelineInstanceDetails(String pipelineName) throws IOException {
        String sURL = "http://localhost:8153/go/api/pipelines/" + pipelineName + "/history";

        URL url = new URL(sURL);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
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
            JsonElement extraDetails = downloadPipelineInstanceDetails(json.get("pipeline-name").getAsString());
            result = mergeInPipelineInstanceDetails(json, extraDetails);
        } catch (IOException e) {
            //TODO: log
        }
        return result;
    }
}

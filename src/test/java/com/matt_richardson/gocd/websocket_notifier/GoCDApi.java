package com.matt_richardson.gocd.websocket_notifier;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

class GoCDApi {

    private int httpPort;

    GoCDApi(int httpPort) {
        this.httpPort = httpPort;
    }

    String createPipeline() throws Exception {
        String name = UUID.randomUUID().toString();
        String url = "http://localhost:" + httpPort + "/go/api/admin/pipelines";
        String urlParameters = "{ \"group\": \"first\",\"pipeline\": {\"label_template\": \"${COUNT}\",\"enable_pipeline_locking\": true,\"name\": \"" + name + "\",\"template\": null,\"materials\": [{\"type\": \"git\",\"attributes\": {\"url\": \"https://github.com/matt-richardson/gocd-websocket-notifier.git\",\"destination\": \"dest\",\"filter\": null,\"invert_filter\": false,\"name\": null,\"auto_update\": true,\"branch\": \"master\",\"submodule_folder\": null,\"shallow_clone\": true}}],\"stages\": [{\"name\": \"defaultStage\",\"fetch_materials\": true,\"clean_working_directory\": false,\"never_cleanup_artifacts\": false,\"approval\": {\"type\": \"success\",\"authorization\": {\"roles\": [],\"users\": []}},\"environment_variables\": [],\"jobs\": [{\"name\": \"defaultJob\",\"run_instance_count\": null,\"timeout\": 0,\"environment_variables\": [],\"resources\": [],\"tasks\": [{\"type\": \"exec\",\"attributes\": {\"run_if\": [\"passed\"],\"command\": \"ls\",\"working_directory\": null}}]}]}]}}";
        sendPost(url, urlParameters);
        return name;
    }

    void unPausePipeline(String name) throws IOException {
        String url = "http://localhost:" + httpPort + "/go/api/pipelines/" + name + "/unpause";
        sendPost(url);
    }

    private void sendPost(String url, String body) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Accept", "application/vnd.go.cd.v2+json");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Confirm", "true");

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(body);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post body : " + body);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.println(response.toString());
    }

    private void sendPost(String url) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Confirm", "true");

        int responseCode = con.getResponseCode();
        System.out.println("Sending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        System.out.println(response.toString());
    }

}

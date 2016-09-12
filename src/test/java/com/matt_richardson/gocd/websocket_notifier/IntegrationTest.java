package com.matt_richardson.gocd.websocket_notifier;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class IntegrationTest {
    static DockerClient docker = null;
    static String containerId = null;
    static String httpPort;
    static String httpsPort;
    static String websocketsPort;

    @BeforeClass
    public static void BeforeAnyTest() throws Exception {
        String testPath = DetermineTestPath();
        CleanupTestFolder(testPath);
        SetupTestFolder(testPath);
        SetupContainer(testPath);
        RunContainer();
    }

    private static String DetermineTestPath() throws UnsupportedEncodingException {
        String path = IntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String testPath = URLDecoder.decode(path, "UTF-8") + "../docker-testing";
        System.out.println("Test path is " + testPath);
        return testPath;
    }

    private static void SetupTestFolder(String testPath) throws IOException {
        String pluginLocation = testPath + "/lib/plugins/external";
        System.out.println("Creating path " + pluginLocation);
        Files.createDirectories(Paths.get(pluginLocation));
        String srcPath = testPath + "/../gocd-websocket-notifier.jar";
        if (!Files.exists(Paths.get(srcPath))) {
            srcPath = testPath + "/../../classes/artifacts/gocd-websocket-notifier.jar";
        }
        String destPath = testPath + "/lib/plugins/external/gocd-websocket-notifier.jar";
        System.out.println("Copying '" + srcPath + "' to '" + destPath + "'");
        Files.copy(Paths.get(srcPath), Paths.get(destPath), REPLACE_EXISTING);
    }

    private static void CleanupTestFolder(String testPath) throws IOException {
        deleteFile(new File(testPath));
    }

    public static void deleteFile(File element) {
        if (element.isDirectory()) {
            for (File sub : element.listFiles()) {
                deleteFile(sub);
            }
        }
        element.delete();
    }

    private static void RunContainer() throws Exception {
        // Start container
        System.out.println("Starting container " + containerId + "...");
        docker.startContainer(containerId);

        // Inspect container

        long t = System.currentTimeMillis();
        long end = t + (20 * 60 * 1000);
        while(System.currentTimeMillis() < end) {
            try (LogStream stream = docker.logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
                String message = stream.readFully();
                System.out.println(message);
                if (message.contains("Error starting Go Server.")) {
                    throw new Exception("Error starting Go Server.");
                }
                if (message.contains("Go Server has started on port 8153 inside this container")) {
                    final ContainerInfo info = docker.inspectContainer(containerId);
                    httpPort = info.networkSettings().ports().get("8153/tcp").get(0).hostPort();
                    httpsPort = info.networkSettings().ports().get("8154/tcp").get(0).hostPort();
                    websocketsPort = info.networkSettings().ports().get("8887/tcp").get(0).hostPort();

                    System.out.println("HTTP port is " + httpPort);
                    System.out.println("HTTPS port is " + httpsPort);
                    System.out.println("WS port is " + websocketsPort);
                    return;
                }
                Thread.sleep(5000);
            }
        }
        throw new Exception("GoCD didn't launch within timeout");
    }

    private static void SetupContainer(String testPath) throws DockerCertificateException, DockerException, InterruptedException, IOException {
        // Create a client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
        docker = DefaultDockerClient.fromEnv().build();

        // Pull an image
        docker.pull("gocd/gocd-server:latest");

        // Bind container ports to host ports
        final String[] ports = {"8153", "8154", "8887"};
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        for (String port : ports) {
            List<PortBinding> hostPorts = new ArrayList<>();
            hostPorts.add(PortBinding.of("0.0.0.0", port));
            portBindings.put(port, hostPorts);
        }

        // Bind container port 443 to an automatically allocated available host port.
        List<PortBinding> randomPort = new ArrayList<>();
        randomPort.add(PortBinding.randomPort("0.0.0.0"));
        portBindings.put("8153", randomPort);
        randomPort = new ArrayList<>();
        randomPort.add(PortBinding.randomPort("0.0.0.0"));
        portBindings.put("8154", randomPort);
        randomPort = new ArrayList<>();
        randomPort.add(PortBinding.randomPort("0.0.0.0"));
        portBindings.put("8887", randomPort);

        final HostConfig hostConfig = HostConfig.builder()
                .portBindings(portBindings)
                .binds(HostConfig.Bind.from(testPath + "/lib").to("/var/lib/go-server").build())
                .build();

        // Create container with exposed ports
        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image("gocd/gocd-server:latest")
                .exposedPorts(ports)
                .build();

        final ContainerCreation creation = docker.createContainer(containerConfig);
        containerId = creation.id();
    }

    @AfterClass
    public static void ShutdownContainer() throws Exception {

        if (containerId != null) {

            System.out.println("Reading '/var/lib/go-server/go-server.log'");
            final String[] command = {"cat", "/var/lib/go-server/go-server.log"};
            final String execId = docker.execCreate(
                    containerId, command, DockerClient.ExecCreateParam.attachStdout(),
                    DockerClient.ExecCreateParam.attachStderr());
            final LogStream output = docker.execStart(execId);
            final String execOutput = output.readFully();
            System.out.println(execOutput);

            System.out.println("Stopping container " + containerId + "...");
            docker.stopContainer(containerId, 10);

            System.out.println("Removing container " + containerId + "...");
            docker.removeContainer(containerId);
        }
        if (docker != null) {
            docker.close();
        }
    }

    private String createPipeline() throws Exception {
        String name = UUID.randomUUID().toString();
        String url = "http://localhost:" + httpPort + "/go/api/admin/pipelines";
        String urlParameters = "{ \"group\": \"first\",\"pipeline\": {\"label_template\": \"${COUNT}\",\"enable_pipeline_locking\": true,\"name\": \"" + name + "\",\"template\": null,\"materials\": [{\"type\": \"git\",\"attributes\": {\"url\": \"https://github.com/matt-richardson/gocd-websocket-notifier.git\",\"destination\": \"dest\",\"filter\": null,\"invert_filter\": false,\"name\": null,\"auto_update\": true,\"branch\": \"master\",\"submodule_folder\": null,\"shallow_clone\": true}}],\"stages\": [{\"name\": \"defaultStage\",\"fetch_materials\": true,\"clean_working_directory\": false,\"never_cleanup_artifacts\": false,\"approval\": {\"type\": \"success\",\"authorization\": {\"roles\": [],\"users\": []}},\"environment_variables\": [],\"jobs\": [{\"name\": \"defaultJob\",\"run_instance_count\": null,\"timeout\": 0,\"environment_variables\": [],\"resources\": [],\"tasks\": [{\"type\": \"exec\",\"attributes\": {\"run_if\": [\"passed\"],\"command\": \"ls\",\"working_directory\": null}}]}]}]}}";
        sendPost(url, urlParameters);
        return name;
    }

    private void unPausePipeline(String name) throws IOException {
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
        StringBuffer response = new StringBuffer();

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
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());
    }

    @Test
    public void testWebSocketReceivesMessageOnNewPipeline() throws Exception
    {
        final CountDownLatch lock = new CountDownLatch(1);
        final String[] result = {null};

        WebSocketClient mWs = new WebSocketClient( new URI( "ws://localhost:" + websocketsPort ), new Draft_10() )
        {
            @Override
            public void onMessage( String message ) {
                System.out.println("Received websocket message: " + message);
                result[0] = message;
                lock.countDown();
            }

            @Override
            public void onOpen( ServerHandshake handshake ) {
                System.out.println("Opened connection to ws://localhost:" + websocketsPort);
            }

            @Override
            public void onClose( int code, String reason, boolean remote ) {
                System.out.println("Closed connection");
                lock.countDown();
            }

            @Override
            public void onError( Exception ex ) {
                ex.printStackTrace();
                lock.countDown();
            }
        };

        System.out.println("Connecting to websocket");
        mWs.connect();
        System.out.println("Creating pipeline");
        String pipelineName = createPipeline();
        System.out.println("Un-pausing pipeline");
        unPausePipeline(pipelineName);
        System.out.println("Waiting for websocket message after pipeline is triggered");
        lock.await(5, TimeUnit.MINUTES);
        mWs.close();
        if (result.length == 0 || result[0] == null)
            throw new Exception("Didn't get a message over the websocket.");
        String expectedPattern = "\\{\"pipeline\":\\{\"name\":\"" + pipelineName + "\",\"counter\":\"1\",\"group\":\"first\",\"build-cause\":\\[\\{\"material\":\\{\"type\":\"git\",\"git-configuration\":\\{\"shallow-clone\":false,\"branch\":\"master\",\"url\":\"https://github.com/matt-richardson/gocd-websocket-notifier.git\"}},\"changed\":true,\"modifications\":\\[\\{\"revision\":\"[0-9a-f]*\",\"modified-time\":\".*\",\"data\":\\{}}]}],\"stage\":\\{\"name\":\"defaultStage\",\"counter\":\"1\",\"approval-type\":\"success\",\"approved-by\":\".*\",\"state\":\"Building\",\"result\":\"Unknown\",\"create-time\":\".*\",\"last-transition-time\":\"\",\"jobs\":\\[\\{\"name\":\"defaultJob\",\"schedule-time\":\".*\",\"complete-time\":\"\",\"state\":\"Scheduled\",\"result\":\"Unknown\"}]}},\"x-pipeline-instance-details\":\\{\"build_cause\":\\{\"approver\":\".*\",\"material_revisions\":\\[\\{\"modifications\":\\[\\{\"email_address\":null,\"id\":1,\"modified_time\":\\d*,\"user_name\":\".*\",\"comment\":\".*\",\"revision\":\"[0-9a-f]*\"}],\"material\":\\{\"description\":\"URL: https://github.com/matt-richardson/gocd-websocket-notifier.git, Branch: master\",\"fingerprint\":\".*\",\"type\":\"Git\",\"id\":\\d*},\"changed\":true}],\"trigger_forced\":.*,\"trigger_message\":\".*\"},\"name\":\"" + pipelineName + "\",\"natural_order\":1.0,\"can_run\":false,\"comment\":null,\"stages\":\\[\\{\"name\":\"defaultStage\",\"approved_by\":\".*\",\"jobs\":\\[\\{\"name\":\"defaultJob\",\"result\":\"Unknown\",\"state\":\"Scheduled\",\"id\":1,\"scheduled_date\":\\d*}],\"can_run\":false,\"result\":\"Unknown\",\"approval_type\":\"success\",\"counter\":\"1\",\"id\":1,\"operate_permission\":true,\"rerun_of_counter\":null,\"scheduled\":true}],\"counter\":1,\"id\":1,\"preparing_to_schedule\":false,\"label\":\"1\"}}";
        Assert.assertTrue(result[0].matches(expectedPattern));
    }
}

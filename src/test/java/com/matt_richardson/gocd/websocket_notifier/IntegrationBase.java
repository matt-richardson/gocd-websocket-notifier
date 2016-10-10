package com.matt_richardson.gocd.websocket_notifier;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public abstract class IntegrationBase {
    private static DockerClient docker = null;
    private static String containerId = null;
    static PluginConfig pluginConfig = null;
    static GoCDApi goCdApi;

    @BeforeClass
    public static void BeforeAnyTest() throws Exception {
        String testPath = DetermineTestPath();
        CleanupTestFolder(testPath);
        SetupTestFolder(testPath);
        SetupContainer();
        RunContainer(testPath);
        goCdApi = new GoCDApi(pluginConfig.getGoHttpPort());
    }

    private static String DetermineTestPath() throws UnsupportedEncodingException {
        String path = IntegrationTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String testPath = Paths.get(URLDecoder.decode(path, "UTF-8")).resolve("../docker-testing").normalize().toString();
        return testPath;
    }

    private static void SetupTestFolder(String testPath) throws IOException {
        String pluginLocation = testPath + "/lib/plugins/external";
        Files.createDirectories(Paths.get(pluginLocation));
        String srcPath = testPath + "/../gocd-websocket-notifier.jar";
        if (!Files.exists(Paths.get(srcPath))) {
            srcPath = testPath + "/../../classes/artifacts/gocd-websocket-notifier.jar";
        }
        String destPath = testPath + "/lib/plugins/external/gocd-websocket-notifier.jar";
        Files.copy(Paths.get(srcPath), Paths.get(destPath), REPLACE_EXISTING);
    }

    private static void CleanupTestFolder(String testPath) throws IOException {
        deleteFile(new File(testPath));
    }

    private static boolean deleteFile(File element) {
        if (element.isDirectory()) {
            File[] files = element.listFiles();
            if (files != null) {
                for (File sub : files) {
                    deleteFile(sub);
                }
            }
        }
        return element.delete();
    }

    private static void CopyJarIntoContainer(String testPath) throws IOException, DockerException, InterruptedException {
        String srcPath = testPath + "/lib/plugins/external/gocd-websocket-notifier.jar";
        String destPath = "/var/lib/go-server/plugins/external";
        String command = "docker exec -i " + containerId + " mkdir -p /var/lib/go-server/plugins/external";
        exec(command);

        command = "docker cp " + srcPath + " " + containerId + ":" + destPath;
        exec(command);

        command = "docker exec -i " + containerId + " chown -R go:go /var/lib/go-server/plugins";
        exec(command);
    }

    private static void exec(String command) throws IOException, InterruptedException {
        Runtime rt = Runtime.getRuntime();
        System.out.println(command);
        Process pr = rt.exec(command);
        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        String line;

        while((line=input.readLine()) != null) {
            System.out.println(line);
        }

        int exitVal = pr.waitFor();
        System.out.println("Exited with error code "+exitVal);
    }

    private static void RunContainer(String testPath) throws Exception {
        System.out.println("Starting container " + containerId + "...");
        docker.startContainer(containerId);

        CopyJarIntoContainer(testPath);

        String lastMessage = "";
        long t = System.currentTimeMillis();
        long end = t + (20 * 60 * 1000);
        while(System.currentTimeMillis() < end) {
            try (LogStream stream = docker.logs(containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
                String message = stream.readFully();
                System.out.println(message.substring(lastMessage.length()));
                lastMessage = message;
                if (message.contains("Error starting Go Server.")) {
                    throw new Exception("Error starting Go Server.");
                }
                if (message.contains("Go Server has started on port 8153 inside this container")) {
                    final ContainerInfo info = docker.inspectContainer(containerId);
                    pluginConfig = new PluginConfig();
                    pluginConfig.setGoHttpPort(Integer.parseInt(info.networkSettings().ports().get("8153/tcp").get(0).hostPort()));
                    pluginConfig.setPort(Integer.parseInt(info.networkSettings().ports().get("8887/tcp").get(0).hostPort()));

                    System.out.println("HTTP port is " + pluginConfig.getGoHttpPort());
                    System.out.println("WS port is " + pluginConfig.getGoHttpPort());
                    return;
                }
                Thread.sleep(5000);
            }
        }
        throw new Exception("GoCD didn't launch within timeout");
    }

    private static void SetupContainer() throws DockerCertificateException, DockerException, InterruptedException, IOException {
        // Create client based on DOCKER_HOST and DOCKER_CERT_PATH env vars
        docker = DefaultDockerClient.fromEnv().build();

        docker.pull("gocd/gocd-server:latest");

        // Bind container ports to host ports
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        portBindings.put("8153", getRandomPort());
        portBindings.put("8154", getRandomPort());
        portBindings.put("8887", getRandomPort());

        final HostConfig hostConfig = HostConfig.builder()
                .portBindings(portBindings)
                .privileged(true)
                .build();

        // Create container with exposed ports
        final ContainerConfig containerConfig = ContainerConfig.builder()
                .hostConfig(hostConfig)
                .image("gocd/gocd-server:latest")
                .exposedPorts("8153", "8154", "8887")
                .build();

        final ContainerCreation creation = docker.createContainer(containerConfig);
        containerId = creation.id();
    }

    private static List<PortBinding> getRandomPort() {
        List<PortBinding> randomPort = new ArrayList<>();
        randomPort.add(PortBinding.randomPort("0.0.0.0"));
        return randomPort;
    }

    @AfterClass
    public static void ShutdownContainer() throws Exception {

        if (containerId != null) {
            System.out.println("Stopping container " + containerId + "...");
            docker.stopContainer(containerId, 10);

            System.out.println("Removing container " + containerId + "...");
            docker.removeContainer(containerId);
        }
        if (docker != null) {
            docker.close();
        }
    }
}
